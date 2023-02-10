/**
 * AppInfo
 * <p>
 * 1.0
 * <p>
 * 2023/2/2 17:41
 */

package com.hclteam.moyu3390.app.offline.demo.web.bean.vo;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@Slf4j
public class AppInfo implements Serializable {
    private String appId;

    private Map<String, Object> processors;
    /**
     * 是否下线
     */
    public volatile Boolean isOffline = Boolean.FALSE;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        log.info("appinfo对象被回收了," + this.toString());
    }
}
