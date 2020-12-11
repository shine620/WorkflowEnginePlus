package com.hy.workflow.common.enums;

public enum HttpStatusEnum {

    // 数据操作错误定义
    SUCCESS(200, "成功!"),
    BODY_NOT_MATCH(400,"请求的数据格式不符!"),
    SIGNATURE_NOT_MATCH(401,"请求的数字签名不匹配!"),
    NOT_FOUND(404, "未找到该资源!"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误!"),
    SERVER_BUSY(503,"服务器正忙，请稍后再试!");

    /** 状态码 */
    private int code;

    /** 状态描述 */
    private String msg;


    HttpStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String value() {
        return String.valueOf(code);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


}