package com.hy.workflow.base;

import com.alibaba.fastjson.JSONObject;
import com.hy.workflow.enums.HttpStatusEnum;

public class ErrorResultBody {

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误原因
     */
    private String cause;


    public ErrorResultBody() {
    }

    public ErrorResultBody(HttpStatusEnum statusEnum) {
        this.code = statusEnum.value();
        this.message = statusEnum.getMsg();
    }

    public static ErrorResultBody error(HttpStatusEnum httpStatus, String cause) {
        ErrorResultBody resultBody = new ErrorResultBody();
        resultBody.setCode(httpStatus.value());
        resultBody.setMessage(httpStatus.getMsg());
        resultBody.setCause(cause);
        return resultBody;
    }

    public static ErrorResultBody error(String code, String message) {
        ErrorResultBody resultBody = new ErrorResultBody();
        resultBody.setCode(code);
        resultBody.setMessage(message);
        return resultBody;
    }

    public static ErrorResultBody error(String code, String message,String cause) {
        ErrorResultBody resultBody = new ErrorResultBody();
        resultBody.setCode(code);
        resultBody.setMessage(message);
        resultBody.setCause(cause);
        return resultBody;
    }


    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }


}
