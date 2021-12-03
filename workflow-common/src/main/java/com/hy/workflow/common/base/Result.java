package com.hy.workflow.common.base;

public class Result{

    /**  响应状态码 */
    private Integer code=0;

    /** 响应的数据 */
    private Object data;


    public Result() {
    }

    public Result(Object data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }



}
