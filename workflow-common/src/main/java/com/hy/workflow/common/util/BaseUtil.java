package com.hy.workflow.common.util;

import java.util.Random;

public class BaseUtil {

    public static void main(String[] args){

    }


    /**
     * 获取一个随机字符串
     *
     * @param length	字符串长度
     * @return
     */
    public static String getRandomString(int length) {
        StringBuffer buffer = new StringBuffer("0123456789abcdefghijklmnopqrstuvwxyz");
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        int range = buffer.length();
        for (int i = 0; i < length; i ++) {
            sb.append(buffer.charAt(random.nextInt(range)));
        }
        return sb.toString();
    }



}
