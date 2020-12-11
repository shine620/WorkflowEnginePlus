package com.hy.workflow.common.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Map;

public class BaseRequest {

    @ApiModelProperty(value="创建日期",example = "2020-09-20 21:36:13")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date createTime;

    @ApiModelProperty(value="修改日期",example = "2020-09-20 22:39:22")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date updateTime;

    private Map<String,String> sortMap;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Map<String, String> getSortMap() {
        return sortMap;
    }

    public void setSortMap(Map<String, String> sortMap) {
        this.sortMap = sortMap;
    }


}
