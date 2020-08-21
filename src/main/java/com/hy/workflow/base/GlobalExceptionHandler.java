package com.hy.workflow.base;

import com.hy.workflow.enums.HttpStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义的业务异常
     * @param request
     * @param response
     * @param e
     * @return
     */
    @ExceptionHandler(value = WorkflowException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public  ErrorResultBody exceptionHandler(HttpServletRequest request, HttpServletResponse response, WorkflowException e){
        logger.error("发生业务异常:",e);
        String stackTraceInfo = getStackTraceInfo(e);
        return ErrorResultBody.error(e.getErrorCode(),e.getErrorMsg(),stackTraceInfo);
    }

    /**
     * 处理空指针的异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResultBody exceptionHandler(NullPointerException e){
        logger.error("发生空指针异常:",e);
        return createErrorResultBody(e);
    }

    /**
     * 处理运行时异常
     * @param e
     * @return
     */
    @ExceptionHandler(value =RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResultBody exceptionHandler(RuntimeException e){
        logger.error("发生运行时异常:",e);
        return createErrorResultBody(e);
    }

    //封装错误信息实体
    private ErrorResultBody createErrorResultBody(Exception e){
        String stackTraceInfo = getStackTraceInfo(e);
        return ErrorResultBody.error( HttpStatusEnum.INTERNAL_SERVER_ERROR.value(), e.toString(), stackTraceInfo);
    }

    //获取堆错误栈信息
    private String getStackTraceInfo(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            e.printStackTrace(pw);
        } finally {
            pw.close();
        }
        return sw.toString();
    }


    /**
     * 处理未知异常
     * @param request
     * @param response
     * @param e
     * @return
     */
    /*@ExceptionHandler(value =Exception.class)
    @ResponseBody
    public ErrorResultBody exceptionHandler(HttpServletRequest request,  HttpServletResponse response, Exception e){
        logger.error("发生未知异常:",e);
        response.setStatus(HttpStatusEnum.INTERNAL_SERVER_ERROR.getCode());
        return createErrorResultBody(e);
    }*/



}
