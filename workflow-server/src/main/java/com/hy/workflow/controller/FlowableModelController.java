package com.hy.workflow.controller;

import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.entity.FlowableModel;
import com.hy.workflow.model.ModelRequest;
import com.hy.workflow.service.FlowableModelService;
import io.swagger.annotations.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.DateUtils;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


@RestController
@RequestMapping("/FlowableModelController")
@Api(value = "Flowable流程模型", tags = "ModelController", description = "流程模型接口")
public class FlowableModelController {

    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

    @Autowired
    private FlowableModelService flowableModelService;


    @ApiOperation(value = "获取流程模型列表(多条件查询)", tags = { "ModelController" })
    @PostMapping("/models/findModelList")
    public PageBean<FlowableModel> findModelList(@ApiParam @RequestParam(defaultValue = "1") Integer startPage,
              @ApiParam @RequestParam(defaultValue = "10") Integer pageSize,
              @RequestBody ModelRequest modelRequest) {
        //PageRequest pageRequest = PageRequest.of(startPage-1, pageSize, Sort.by(Sort.Order.desc("createTime")));
        PageRequest pageRequest = PageBean.getPageRequest(modelRequest,startPage,pageSize);
        return flowableModelService.findModelList(modelRequest,pageRequest);
    }


    @ApiOperation(value = "ID查找模型", tags = { "ModelController" })
    @GetMapping(value = "/models/getModel/{modelId}", produces = "application/json")
    public FlowableModel getModel(@ApiParam(name = "modelId",value = "模型ID") @PathVariable String modelId, HttpServletRequest request) {
        FlowableModel model = flowableModelService.findById(modelId);
        return model;
    }


    @ApiOperation(value = "部署模型", tags = { "ModelController" })
    @GetMapping(value = "/models/deploy/{modelId}")
    public void deploy(@ApiParam(name = "modelId",value = "模型ID") @PathVariable("modelId") String modelId, HttpServletResponse response) {
        FlowableModel model = flowableModelService.findById(modelId);
        if(model==null) throw new WorkflowException("流程模型不存在：ModelId="+modelId);
        flowableModelService.deploy(model);
        response.setStatus(HttpStatus.OK.value());
    }


    @ApiOperation(value = "删除模型", tags = { "ModelController" })
    @DeleteMapping("/models/deleteModel/{modelId}")
    public void deleteModel(@ApiParam(name = "modelId",value = "模型ID") @PathVariable String modelId, HttpServletResponse response) {
        flowableModelService.deleteModel(modelId);
        response.setStatus(HttpStatus.NO_CONTENT.value());

    }


    @ApiOperation(value = "批量删除模型", tags = { "ModelController" })
    @DeleteMapping("/models/batchDeleteModel")
    public void batchDeleteModel(@ApiParam(name = "modelIds",value = "模型ID") @RequestParam String[] modelIds, HttpServletResponse response) {
        flowableModelService.batchDeleteModel(modelIds);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "保存模型(创建或修改)", tags = {"ModelController" })
    @PostMapping(value = "/models",consumes ="application/json" )
    public FlowableModel saveModel(@RequestBody ModelRequest modelRequest){
        return flowableModelService.saveModel(modelRequest);
    }


    @ApiOperation(value = "导入模型", tags = { "ModelController" })
    @PostMapping(value = "/models/importModel")
    public List<FlowableModel> importModel(@RequestParam("modelfile")MultipartFile uploadFile) {

        String fileName = uploadFile.getOriginalFilename();
        List<FlowableModel> modelList = new ArrayList<>();
        try {
            //导入单个模型
            if(fileName.endsWith(".xml")||fileName.endsWith(".bpmn")){
                FlowableModel model = flowableModelService.importModel(fileName,uploadFile.getInputStream());
                modelList.add(model);
            }
            //ZIP格式批量导入
            else if(fileName.endsWith(".zip")){
                //存储上传的文件
                String tempPath = System.getProperty("java.io.tmpdir");
                File zipFile = new File(tempPath+File.separator+fileName);
                uploadFile.transferTo(zipFile);
                //从压缩文件中读取模型数据
                ZipFile zip = new ZipFile(zipFile, Charset.forName("UTF-8"));
                for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String zipEntryName = entry.getName();
                    // 判断是否为文件夹,是文件夹直接跳过
                    if (entry.isDirectory()) continue;
                    //只读取.xml/.bpmn/.json格式的文件
                    if(!zipEntryName.endsWith(".xml")&&!zipEntryName.endsWith(".bpmn")&&!zipEntryName.endsWith(".json")) continue;
                    logger.info("解析的模型文件：{}",zipEntryName);
                    InputStream in = zip.getInputStream(entry);
                    FlowableModel model = flowableModelService.importModel(zipEntryName,in);
                    modelList.add(model);
                }
                zip.close();
                zipFile.delete();
            }
            else{
                throw new WorkflowException("请上传xml、bpmn、json、zip等符合导入格式的压缩文件");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modelList;
    }


    @ApiOperation(value = "导出模型", tags = { "ModelController" })
    @GetMapping(value = "/models/exportModel/{modelId}")
    public void exportModel(    HttpServletRequest request, HttpServletResponse response,
           @ApiParam(name = "modelId",value = "模型ID") @PathVariable String modelId){

        FlowableModel model = flowableModelService.findById(modelId);
        if(model==null) return;
        String name = model.getName().replaceAll(" ", "_");
        try {
            byte[] dataBytes = model.getXml().getBytes();
            name+=".bpmn20.xml";
            response.setContentType("application/xml");
            //设置下载文件名（处理不同浏览器文件名乱码问题）
            String agent = request.getHeader("user-agent").toLowerCase();
            if (agent.contains("firefox")) {
                name = new String(name.getBytes("UTF-8"), "iso-8859-1");
            } else {
                name = URLEncoder.encode(name, "UTF-8");
            }
            String contentDispositionValue = "attachment; filename=" + name;
            response.setHeader("Content-Disposition", contentDispositionValue);
            //输出文件
            ServletOutputStream servletOutputStream = response.getOutputStream();
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(dataBytes));
            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) {
                    break;
                }
                servletOutputStream.write(buffer, 0, count);
            }
            //刷新且关闭流
            servletOutputStream.flush();
            servletOutputStream.close();
        } catch (IOException e) {
            throw new InternalServerErrorException("文件读写失败");
        }
    }


    @ApiOperation(value = "批量导出流程模型", tags = { "ModelController" })
    @GetMapping(value = "/models/exportModels")
    public void exportModels( @ApiParam(name = "modelIds",value = "模型ID") @RequestParam String[] modelIds,HttpServletResponse response){

        if(modelIds!=null&&modelIds.length>100) throw new WorkflowException("一次最多只能导出100条模型数据！");
        //压缩文件
        String zipName = DateUtils.formatDate(new Date(),"yyyyMMddHHmmssSSS")+ RandomStringUtils.randomAlphanumeric(5)+".zip";
        String tempPath = System.getProperty("java.io.tmpdir");
        File zipFile= new File(tempPath + File.separator+zipName+".zip");
        try{
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
            Set<String> names = new HashSet<>();
            for(String modelId : modelIds){
                FlowableModel model = flowableModelService.findById(modelId);
                if(model==null) continue;
                String name = model.getName().replaceAll(" ", "_");
                if(names.contains(name)) name+= model.getModelKey();
                names.add(name);
                byte[] dataBytes = model.getXml().getBytes();
                name+=".bpmn20.xml";
                zipOut.putNextEntry(new ZipEntry(name));
                zipOut.write(dataBytes);
                zipOut.closeEntry();
            }
            zipOut.close();

            //浏览器下载
            String contentDispositionValue = "attachment; filename=" +URLEncoder.encode(zipName, "UTF-8");
            response.setHeader("Content-Disposition", contentDispositionValue);
            response.setContentType("application/x-download");
            InputStream in = new FileInputStream(zipFile);
            OutputStream out = response.getOutputStream();
            //循环取出流中的数据
            byte[] b = new byte[2048];
            int len;
            while ((len = in.read(b)) > 0){
                out.write(b, 0, len);
            }
            in.close();
            out.close();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        //删除生成的压缩文件
        zipFile.delete();
    }



}
