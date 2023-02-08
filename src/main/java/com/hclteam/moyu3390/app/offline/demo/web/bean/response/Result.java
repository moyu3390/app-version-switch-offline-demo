/**
 * Result
 * <p>
 * 1.0
 * <p>
 * 2023/2/1 15:47
 */

package com.hclteam.moyu3390.app.offline.demo.web.bean.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private String code;
    private String msg;
    private Boolean success;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(Boolean.TRUE);
        result.setMsg("");
        result.setCode("0");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setSuccess(Boolean.FALSE);
        result.setMsg(msg);
        result.setCode("-10000");
        return result;
    }
}
