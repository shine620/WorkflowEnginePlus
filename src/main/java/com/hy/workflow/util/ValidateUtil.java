package com.hy.workflow.util;

import com.hy.workflow.base.WorkflowException;
import org.springframework.data.domain.PageRequest;

public class ValidateUtil {

    public static void checkPageNum(PageRequest pageRequest){
        if(pageRequest.getPageNumber()<0)
            throw new WorkflowException("页码必须大于1");
    }

}
