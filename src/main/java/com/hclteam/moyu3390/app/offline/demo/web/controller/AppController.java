/**
 * AppController
 * <p>
 * 1.0
 * <p>
 * 2023/2/2 9:41
 */

package com.hclteam.moyu3390.app.offline.demo.web.controller;

import com.hclteam.moyu3390.app.offline.demo.web.bean.response.Result;
import com.hclteam.moyu3390.app.offline.demo.web.service.AppService;
import com.hclteam.moyu3390.app.offline.demo.web.service.AppService2;
import com.hclteam.moyu3390.app.offline.demo.web.service.OrderService;
import com.hclteam.moyu3390.app.offline.demo.web.service.OrderService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app")
public class AppController {
    @Autowired
    AppService service;
    @Autowired
    AppService2 service2;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderService2 orderService2;

    @GetMapping("/openApp")
    public Result openApp(@RequestParam String appId) {
        List<String> appOrders = service.getAppOrders(appId);
        return Result.success(appOrders);
    }

    /**
     * 方式①，测试事务功能
     * @param appId
     * @param processorId
     * @return
     */
    @GetMapping("/testOrder")
    public Result executeOrder(@RequestParam String appId, @RequestParam String processorId) {
        return orderService.execute(appId, processorId);
    }

    /**
     * 方式①，发布应用
     * @param appInfo 应用信息，格式：appId=version;实例：testapp=v1
     * @return
     */
    @PostMapping("/publishApp")
    public Result publishApp(@RequestBody String appInfo) {
        String[] s = appInfo.split("=");
        String appId = s[0];
        String version = s[1];
        Object o = service.publishApp(appId, version);
        return Result.success(o);
    }

    /**
     * 方式②，发布应用
     * @param appInfo 应用信息，格式：appId=version;实例：testapp=v1
     * @return
     */
    @PostMapping("/publishApp2")
    public Result publishApp2(@RequestBody String appInfo) {
        String[] s = appInfo.split("=");
        String appId = s[0];
        String version = s[1];
        Object o = service2.publishApp(appId, version);
        return Result.success(o);
    }


    /**
     * 方式②，测试事务功能
     * @param appId
     * @param processorId
     * @return
     */
    @GetMapping("/testOrder2")
    public Result executeOrder2(@RequestParam String appId, @RequestParam String processorId) {
        return orderService2.execute2(appId, processorId);
    }


}
