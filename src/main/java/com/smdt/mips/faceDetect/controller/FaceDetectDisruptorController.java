package com.smdt.mips.faceDetect.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.smdt.mips.faceDetect.asyncrequest.AsyncUrlVo;
import com.smdt.mips.faceDetect.asyncrequest.AsyncVo;
import com.smdt.mips.faceDetect.asyncrequest.RequestQueue;
import com.smdt.mips.faceDetect.asyncrequest.RequestUrlQueue;
import com.smdt.mips.faceDetect.utils.Response_result;
import com.smdt.mips.request_record.service.RequestRecordService;
import com.smdt.mips.source.service.SourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.util.*;



//GET（SELECT）：从服务器取出资源（一项或多项）。
//POST（CREATE）：在服务器新建一个资源。
//PUT（UPDATE）：在服务器更新资源（客户端提供改变后的完整资源）。
//PATCH（UPDATE）：在服务器更新资源（客户端提供改变的属性）。
//DELETE（DELETE）：从服务器删除资源。

//旷视和自己算法：旷视接口传图片格式，jpg的二进制byte[]；我们自己算法接口传输rgb byte[]

@RestController
@RequestMapping("/api/face")
public class FaceDetectDisruptorController {

    @Autowired
    private SourceService sourceService;

    @Autowired
    private RequestRecordService requestRecordService;

    @Autowired
    private RequestQueue queue;

    @Autowired
    private RequestUrlQueue urlqueue;

    private static final Logger log = LoggerFactory.getLogger(FaceDetectDisruptorController.class);


    /**
     * <blockquote>
     * <pre>
     * 模拟下单处理，实现高吞吐量异步处理请求
     * 1、 Controller层接口只接收请求，不进行处理，而是把请求信息放入到对应该接口的请求队列中
     * 2、 该接口对应的任务类监听对应接口的请求队列，从队列中顺序取出请求信息并进行处理
     * 优点：接口几乎在收到请求的同时就已经返回，处理程序在后台异步进行处理，大大提高吞吐量
     * </pre>
     * </blockquote>
     * 异步请求
     * @author Aruen
     */
    @RequestMapping(value="/detect", method = RequestMethod.POST)
    public DeferredResult<Response_result> detect(HttpServletRequest request, @RequestBody String params, @RequestHeader Map<String, String> headers) throws Exception {

        AsyncVo<String,Map<String, String>,String,Response_result> vo = new AsyncVo<>();
        AsyncUrlVo<String,Map<String, String>,String,Response_result> url_vo = new AsyncUrlVo<>();
        DeferredResult<Response_result> result = new DeferredResult<>(30*1000L);

        result.onTimeout(new Runnable() {
            @Override
            public void run() {
                result.setResult(new Response_result().fail("线程执行超时", 1034, null));
                log.info("1034#线程执行超时");
            }
        });
        result.onCompletion(new Runnable() {
            @Override
            public void run() {
                log.info("异步执行完毕");
            }
        });

        JSONObject object = JSON.parseObject(params);
        //model_type选用的模型，0：mtcnn   1:face++;  para_type图片参数，0：图片url   1:图片的base64编码
        Integer model_type = object.getInteger("model_type");
        Integer para_type = object.getInteger("type");
        if(StringUtils.isEmpty(para_type) || StringUtils.isEmpty(model_type)){
            Response_result result1 = new Response_result().fail("body中未传图片类型参数", 1032, null);
            log.info("1032#body中未传图片类型参数");
            result.setResult(result1);
        }else if(StringUtils.isEmpty(headers.get("authorization")) || StringUtils.isEmpty(headers.get("date"))){
            Response_result result1 = new Response_result().fail("header中有必选参数未传", 1032, null);
            log.info("1032#header中有必选参数未传");
            result.setResult(result1);
        }else if(headers.get("authorization").split(" ").length < 2){
            Response_result result1 = new Response_result().fail("传入签名有误", 1032, null);
            log.info("l032#传入签名有误");
            result.setResult(result1);
        }else if(para_type == 1){
            //图片base64字符串
            vo.setParams(params);
            vo.setHeaders(headers);
            vo.setResult(result);
            queue.getHttpQueue().put(vo);
        }else if(para_type == 0){
            //图片url
            url_vo.setParams(params);
            url_vo.setHeaders(headers);
            url_vo.setResult(result);
            urlqueue.getHttpUrlQueue().put(url_vo);
        }else {
            log.info("1032#参数错误：无效的para_type");
            result.setResult(new Response_result().fail("参数错误：无效的para_type", 1032, null));
        }

        return result;
    }
}