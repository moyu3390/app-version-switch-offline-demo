/**
 * OrderService
 * <p>
 * 1.0
 * <p>
 * 2023/2/2 10:29
 */

package com.hclteam.moyu3390.app.offline.demo.web.service;

import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.response.Result;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Objects;

@Service
public class OrderService {

    public Result execute(String appId, String processId) {
        Object processor = ProcessorManager.getProcessor(appId, "OrderProcessor");
        if (Objects.isNull(processor)) {
            return Result.error("未找到对应的事务，应用标识：【" + appId + "】，事务标识：【" + processId + "】");
        }
        try {
            Method processorMethod = processor.getClass().getMethod("processor", String.class);
            Method getVersion = processor.getClass().getMethod("getVersion");
            Object version = getVersion.invoke(processor);
//            System.err.println("应用版本：" + version);
            Object res = processorMethod.invoke(processor, appId);
//            System.err.println(res);
//            System.err.println("==========================================================================");

            return Result.success(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
