package com.alex.web.util;

import lombok.Data;

import java.util.Date;

/**
 * @Description 所有接口调用返回的统一包装结果类
 */
@Data
public class ApiResult implements java.io.Serializable {

    private String code;
    private Date timestamp;
    private String msg = "";
    private Object data;

    public ApiResult(){
        this.code = Constants.SUCCESS;
    }

    public ApiResult(final String code, final Object data){
        this.code = code;
        this.data = data;
    }

    public ApiResult(final String code, final String msg, final Object data){
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public ApiResult(final Object data){
        this.code = Constants.SUCCESS;
        this.data = data;
    }

    public Date getTimestamp() {
        return timestamp == null ? new Date(): timestamp;
    }
}
