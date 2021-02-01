package com.hy.workflow.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


public class HttpUtil {
	
    private static PoolingHttpClientConnectionManager connectionManager;
    private static RequestConfig requestConfig ;
	
    static {
    	connectionManager = new PoolingHttpClientConnectionManager();
    	connectionManager.setMaxTotal(500);
    	connectionManager.setDefaultMaxPerRoute(200);
        requestConfig = RequestConfig.custom()
        		.setConnectionRequestTimeout(3000)  //从连接池中获取可用连接超时
        		.setConnectTimeout(3000) //连接目标超时
        		.setSocketTimeout(50000) //.等待响应超时(读取数据超时)
        		.build();
    }

    //获HttpClient对象
    public static CloseableHttpClient getHttpClient(){
    	 CloseableHttpClient httpClient =HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
	     return httpClient;
    }

    //设置Post请求参数
	public static void setPostParams(HttpPost httPost, Map<String, Object> paramMap) {
        List<NameValuePair> pair = new ArrayList<NameValuePair>();
        for (String key : paramMap.keySet()) {
        	pair.add( new BasicNameValuePair(key, paramMap.get(key).toString()) );
        }
        try {
            httPost.setEntity(new UrlEncodedFormEntity(pair, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        	new RuntimeException(e);
        }
    }

    public static String post(String url, String params, Map<String,Object> headers){
        CloseableHttpClient client = HttpUtil.getHttpClient();
        HttpPost httpPost = new HttpPost(url);
        try{
            //设置请求头
            if(headers!=null){
                for(Map.Entry<String,Object> entry : headers.entrySet()){
                    httpPost.setHeader(entry.getKey(),entry.getValue()==null?"":entry.getValue().toString());
                }
            }
            //设置请求参数
            if(params!=null) httpPost.setEntity(new StringEntity(params, Charset.forName("utf-8")));
            //发送Post请求
            CloseableHttpResponse response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            //返回响应数据
            String result = EntityUtils.toString(entity);
            return  result;
        } catch (IOException  e) {
            throw new RuntimeException(e);
        }
    }

    public static String get(String url,Map<String,Object> headers){
        CloseableHttpClient client = HttpUtil.getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try{
            //设置请求头
            if(headers!=null){
                for(Map.Entry<String,Object> entry : headers.entrySet()){
                    httpGet.setHeader(entry.getKey(),entry.getValue()==null?"":entry.getValue().toString());
                }
            }
            //发送Get请求
            CloseableHttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            //返回响应数据
            String result = EntityUtils.toString(entity);
            return  result;
        } catch (IOException  e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        Map<String,Object> headers = new HashMap<>();
        headers.put("username","zhangsan");
        headers.put("password","11223344556677889");
        headers.put("Content-Type","application/json;charset=utf-8");
        String getresult = HttpUtil.get("http://localhost/WorkflowController/management/properties",headers);
        System.out.println(getresult);

        String params = "{\n" +
                "  \"businessId\": 1000,\n" +
                "  \"businessName\": \"房屋租赁合同\",\n" +
                "  \"businessType\": \"CONTRACT\",\n" +
                "  \"businessUrl\": \"las/viewContract/1000\",\n" +
                "  \"processDefinitionId\": \"ApproveSelectProcess:1:262707\",\n" +
                "  \"userId\": \"zhanxiaosan\",\n" +
                "  \"variables\": {\"age\":18,\"assigneeList\":[\"wangmixi\",\"zhaoxiaohua\"]},\n" +
                "  \"nextTaskList\": [\n" +
                "    {\n" +
                "      \"assignee\": \"zhangmingming\",\n" +
                "      \"flowElementId\": \"JingLi\",\n" +
                "      \"flowElementType\": \"userTask\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String postresult = HttpUtil.post("http://localhost/ProcessInstanceController/process-instances/startProcessInstance",params,headers);
        System.out.println(postresult);

    }


}
