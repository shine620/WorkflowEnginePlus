package com.hy.workflow.util;

import org.springframework.data.domain.PageRequest;

public class ValidateUtil {

    public static void checkPageNum(PageRequest pageRequest){
        if(pageRequest.getPageNumber()<0)
            throw new RuntimeException("页码必须大于1");
    }

}
