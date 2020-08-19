package com.hy.workflow.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.Serializable;
import java.util.List;

public class PageBean<T> implements Serializable {

    //页码
    private int  pageNum;

    //每页大小
    private int  pageSize;

    //总记录数
    private long  totalCount;

    //总页数
    private int   totalPage;

    //数据
    private List<T> data;


    public PageBean(int pageNum, int pageSize, long totalCount) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        if(totalCount%pageSize==0){
            this.totalPage = (int) totalCount / pageSize;
        }else{
            this.totalPage = (int) (totalCount / pageSize) +1;
        }
    }

    public PageBean(Page p) {
        this.pageNum = p.getNumber()+1;
        this.pageSize = p.getSize();
        this.totalCount = p.getTotalElements();
        this.totalPage = p.getTotalPages();
        this.data = p.getContent();
    }


    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }


}
