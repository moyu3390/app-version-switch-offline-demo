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

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
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
        System.err.println("appinfo对象被回收了," + this.toString());
    }
}
