package com.smdt.mips.faceDetect.asyncrequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.smdt.mips.faceDetect.utils.Response_result;
//import com.smdt.mips.constv.Const;
import com.smdt.mips.faceDetect.controller.FaceDetectDisruptorController;
import com.smdt.mips.faceDetect.megdetect.FaceRect;
import com.smdt.mips.faceDetect.mtcnn.Box;
import com.smdt.mips.faceDetect.utils.ImageUtiles;
import com.smdt.mips.request_record.service.RequestRecordService;
import com.smdt.mips.sdk.MipsAccessKeyApiUrl;
import com.smdt.mips.source.entity.Source;
import com.smdt.mips.source.service.SourceService;
import com.smdt.mips.util.SignDTO;
import com.smdt.mips.util.SimpleResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.smdt.mips.faceDetect.utils.InitKit.megdetect_multi;
import static com.smdt.mips.faceDetect.utils.InitKit.mtcnn_multi;
import static com.smdt.mips.faceDetect.utils.ServerUtils.serverUtils;


/**
 * 处理Http请求接口的任务，每个任务类处理一种接口
 *
 * @author Aruen
 *
 */
@Component
public class HttpTask extends Thread {

    private static final Logger log = LoggerFactory.getLogger(HttpTask.class);

    @Autowired
    private RequestQueue queue;

    @Autowired
    private SourceService sourceService;

    @Autowired
    private RequestRecordService requestRecordService;

    private boolean running = true;

    private int m;

    public HttpTask(RequestQueue queue){
        this.queue = queue;
    }


    @SneakyThrows
    @Override
    public void run() {
        while (running) {
//            try {
                AsyncVo<String,Map<String, String>,String, Response_result> vo = queue.getHttpQueue().take();
//                System.out.println("[ HttpTask ]开始处理订单");

            try {
                String params = vo.getParams();
                Map<String, String> headers = vo.getHeaders();
//                String request_id = vo.getRequestid();
                //AtomicLong lastId = new AtomicLong();      // 自增id，用于requestId的生成过程
                Random r = new Random();
                int randomDate = r.nextInt(10000);
                long startTimeStamp = System.currentTimeMillis();
//              String ip = headers.get("host").split(":")[0];
                String ip = "192.168.9.1";
                //String request_id = hexIp(ip) + Long.toString(startTimeStamp, Character.MAX_RADIX) + "-" + lastId.incrementAndGet();
                String request_id = hexIp(ip) + Long.toString(startTimeStamp, Character.MAX_RADIX) + "-" + randomDate;


                String request_time = headers.get("date");
                SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
                df.setTimeZone(new SimpleTimeZone(0, "GMT"));
                //        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date requestTime =df.parse(request_time);

                String authorization = headers.get("authorization");
                String[] accessKeyValue = authorization.split(" ");
                String AccessKey_Id = accessKeyValue[1].split(":")[0];
//                String source_ip = headers.get("host").split(":")[0];
                String source_ip = getIpAddr(headers);

                JSONObject object = JSON.parseObject(params);
                //model_type选用的模型，0：mtcnn   1:face++;  para_type图片参数，0：图片url   1:图片的base64编码

                Integer model_type = object.getInteger("model_type");
                Integer para_type = object.getInteger("type");


                // 2、根据key_id调sdk获得用户Id和资源类型(内部子系统用户调用为true-1；第三方用户调用的为false-0)
//                String userid = AccessKey_Id;
                //        String userid = "5k8264iLtKCh16Cq1231m";
//                int usertype = 0;
//                String threadId = currentThread().getName();
                int modelId = Integer.parseInt(currentThread().getName().split("-")[3]);

                // 1、验证签名是否合法，根据key_id调sdk获得签名，比较签名是否一致

                long startTime0 = System.currentTimeMillis();
                SignDTO sign = new SignDTO();
                sign.setMethod("POST");
                sign.setAccept(headers.get("accept"));
                sign.setContentType(headers.get("content-type"));
                sign.setPath("/api/face/detect");
                sign.setBody(params);
                sign.setTimestamp(headers.get("date"));
                sign.setSign(headers.get("authorization"));
                sign.setIp(source_ip);
                SimpleResult accessResult = MipsAccessKeyApiUrl.INSTANCE.validationSign(sign);
                if(accessResult.getResult().equals("SUCCESS")){
                // 2、根据key_id调sdk获得用户Id和资源类型(内部子系统用户调用为true-1；第三方用户调用的为false-0)
                //{"company_code":"2990757318216068","create_time":1586508297000,"is_default":true,"status":"0"}
                //company_code:用户ID  is_default:是否使用默认的资源包（内部子系统调用），非默认的使用付费资源包（第三方用户调用）
                    String signatureResult = accessResult.getBody();
                    JSONObject jsonResult = JSON.parseObject(signatureResult);

                    String userid = jsonResult.getString("company_code");
                    boolean is_default = jsonResult.getBooleanValue("is_default");
//                    String ak_status = jsonResult.getString("status");
                    Long snow_flake_id = Long.parseLong(jsonResult.getString("snow_flake_id"));

                    int usertype = 1;
                    if(is_default){
                        //内部子系统用户,不做数据库计数
                        usertype = 1;
                        Date sourceValidTime = null;
                        long end1 = System.currentTimeMillis();
                        //                      long t1 = end1 - start;
                        //                      System.out.println("异步调用中调用数据库耗时："+t1);

                        // 2.3 调用人脸检测服务
                        Map<String, String> contain_content = new HashMap<>();
                        Map<String, Response_result> result_content = new HashMap<>();

                        int restCount, count;
                        if(model_type == 0) {
                            log.info("mtcnn-pic-子用户-"+userid);
                            byte[] rgbByte = null;
                            int width = 0, height = 0;
                            String image_base64 = object.getString("content");
                            if (image_base64 == null || image_base64 == "" || image_base64.length() == 0) {
                                //内部子系统用户不用返回状态给云平台页面
                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：无效的type");
                                //内部子系统用户
                                serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                log.info("mtcnn-pic-子用户-"+userid+"-1030#参数错误：无效的type");
                                vo.getResult().setResult(new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id));
                            }else{
                                String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
                                if (!image_base64.matches(base64Pattern)) {
                                    MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                    //内部子系统用户
                                    serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                    log.info("mtcnn-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                    vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                }else{
                                    // 将 base64 字符转转为二进制数组类型
                                    byte[] decoderBytes = Base64Utils.decodeFromString(image_base64);

                                    try {
                                        // 二进制输入流
                                        InputStream in = new ByteArrayInputStream(decoderBytes);
                                        BufferedImage ImageOne = ImageIO.read(in);
                                        in.close();

                                        if (StringUtils.isEmpty(ImageOne)) {
                                            //内部子系统用户
                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 300000, "1032#参数错误：无效的image content");
                                            log.info("mtcnn-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                            vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                        } else {
                                            //可以获得宽和高了
                                            height = ImageOne.getHeight();
                                            width = ImageOne.getWidth();

                                            rgbByte = ImageUtiles.image2ByteRgb(ImageOne);

                                            long startTime1 = System.currentTimeMillis();

                                            Map<String, Object> res = new HashMap<>();
                                            //            res.put("request_id", request_id);
                                            List<Object> face_list = new ArrayList<>();
                                            int value = Math.min(height, width) / 3;
                                            //                                    mtcnn_multi.setMinSize(value, modelId-1);
                                            //图片参数为rgb的byte数组
                                            Vector<Box> boxes = mtcnn_multi.detectFaces(rgbByte, width, height, modelId - 1); //图片参数为byte数组

                                            res.put("face_num", boxes.size());

                                            //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
                                            if (!boxes.isEmpty()) {
                                                for (int i = 0; i < boxes.size(); i++) {
                                                    Map<String, Object> face_rect = new HashMap<>();
                                                    Map<String, Object> angle = new HashMap<>();
                                                    Map<String, Object> score = new HashMap<>();
                                                    Map<String, Object> res1 = new HashMap<>();

                                                    int[] box = boxes.get(i).box;
                                                    float score_value = boxes.get(i).score;
                                                    float[] angle_value = boxes.get(i).angles;

                                                    face_rect.put("left", box[0]);
                                                    face_rect.put("top", box[1]);
                                                    face_rect.put("right", box[2]);
                                                    face_rect.put("bottom", box[3]);

                                                    angle.put("yaw", angle_value[1]);
                                                    angle.put("pitch", angle_value[2]);
                                                    angle.put("roll", angle_value[0]);

                                                    score.put("score", score_value);


                                                    res1.put("face_rect", face_rect);
                                                    res1.put("pose", angle);
                                                    res1.put("quality", score);

                                                    face_list.add(res1);

                                                }
                                            }

                                            res.put("face_list", face_list);
                                            //                          mtcnn_multi.releaseDetect(); //检测完成后释放c++对象
                                            long endTime1 = System.currentTimeMillis();
                                            long detectTime1 = endTime1 - startTime1;
                                            res.put("detect_time", detectTime1);

                                            Response_result result = new Response_result().success(request_id, res);

                                            //内部子系统用户
                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);

                                            //调用成功
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 100000);
                                            log.info("mtcnn-pic-子用户-"+userid+"-检测结果："+String.valueOf(res));
                                            vo.getResult().setResult(result);
                                        }
                                    }catch (IOException e2){
                                        //内部子系统用户
                                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 300000, "1032#参数错误：无效的image content");
                                        log.info("mtcnn-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                        vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                    }
                                }
                            }

                        }else if(model_type == 1){
                            log.info("megdetect-pic-子用户-"+userid);
                            byte[] jpgByte = null;
                            String image_base64 = object.getString("content");
                            if (image_base64 == null || image_base64.length() == 0 || image_base64 == "") {
                                //内部子系统用户
                                serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);

                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：无效的type");
                                log.info("megdetect-pic-子用户-"+userid+"-1030#参数错误：无效的type");
                                vo.getResult().setResult(new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id));
                            }else{
                                String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
                                if (!image_base64.matches(base64Pattern)) {
                                    //内部子系统用户
                                    serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);

                                    MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                    log.info("megdetect-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                    vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                }else{
                                    jpgByte = Base64Utils.decodeFromString(image_base64);
                                    long startTime2 = System.currentTimeMillis();

                                    try {
                                        // 二进制输入流
                                        InputStream in = new ByteArrayInputStream(jpgByte);
                                        BufferedImage ImageOne = ImageIO.read(in);
                                        in.close();

                                        if (StringUtils.isEmpty(ImageOne)) {
                                            //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
                                            //内部子系统用户
                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 300000, "1032#参数错误：无效的image content");
                                            log.info("megdetect-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                            vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                        } else {
                                            int value = Math.min(ImageOne.getWidth(), ImageOne.getHeight()) / 3;
                                            //                                    megdetect_multi.setMinSize(value, modelId-1);
                                            //图片参数为jpg的byte数组
                                            Vector<FaceRect> boxes = megdetect_multi.detectFacesByJpgByte(jpgByte, modelId - 1); //图片参数为byte数组


                                            Map<String, Object> res = new HashMap<>();
                                            res.put("face_num", boxes.size());

                                            List<Object> face_list = new ArrayList<>();

                                            //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
                                            if (!boxes.isEmpty()) {
                                                for (int i = 0; i < boxes.size(); i++) {
                                                    Map<String, Object> face_rect = new HashMap<>();
                                                    Map<String, Object> pose = new HashMap<>();
                                                    Map<String, Object> quality = new HashMap<>();
                                                    Map<String, Object> res1 = new HashMap<>();

                                                    face_rect.put("left", boxes.get(i).face_rect[0]);
                                                    face_rect.put("top", boxes.get(i).face_rect[1]);
                                                    face_rect.put("right", boxes.get(i).face_rect[2]);
                                                    face_rect.put("bottom", boxes.get(i).face_rect[3]);

                                                    pose.put("yaw", boxes.get(i).pose[2]);
                                                    pose.put("pitch", boxes.get(i).pose[1]);
                                                    pose.put("roll", boxes.get(i).pose[0]);

                                                    quality.put("blur", boxes.get(i).quality[0]);
                                                    quality.put("face_brightness", boxes.get(i).quality[1]);
                                                    quality.put("brightness_deviation", boxes.get(i).quality[2]);

                                                    res1.put("face_rect", face_rect);
                                                    res1.put("pose", pose);
                                                    res1.put("quality", quality);
                                                    res1.put("face_completeness", boxes.get(i).face_completeness);
                                                    res1.put("errcode", boxes.get(i).errcode);

                                                    face_list.add(res1);

                                                }
                                            }

                                            res.put("face_list", face_list);
                                            //                              megdetect.releaseDetect(); //检测完成后释放c++对象
                                            long endTime2 = System.currentTimeMillis();
                                            long detectTime2 = endTime2 - startTime2;
                                            res.put("detect_time", detectTime2);

                                            Response_result result = new Response_result().success(request_id, res);

                                            //内部子系统用户
                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);

                                            //调用成功
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 100000);
                                            log.info("megdetect-pic-子用户-"+userid+"-检测结果："+String.valueOf(res));
                                            vo.getResult().setResult(result);
                                        }
                                    }catch (IOException e2){
                                        //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
                                        //内部子系统用户
//                                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 300000, "1032#参数错误：无效的image content");
                                        log.info("megdetect-pic-子用户-"+userid+"-1032#参数错误：无效的image content");
                                        vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                    }
                                }
                            }
                        }else{
                            //内部子系统用户
                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);

                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：无效的type");
                            log.info("pic-子用户-"+userid+"-1030#参数错误：无效的type");
                            vo.getResult().setResult(new Response_result().fail("参数错误：无效的type", 1030, request_id));
                        }
                    }else{
                        //第三方用户
                        usertype = 0;

                        // 根据用户ID查看用户拥有的资源包(预付费与QPS,可能有多个)
                        // 查找在有效期内预付费资源包(sourcetype = 0)失效日期最近的资源信息
                        List<Source> lastSourceInfo = serverUtils.sourceService.findLastSourceByUserId(userid, 0, requestTime,usertype);
                        // 查找在有效期内预付费资源包(sourcetype = 0)失效日期最近的且可用次数小于总共可使用次数的资源信息（调用次数是否超过限制？）,失效日期有多个，选使用次数较多的那个资源
                        Source lastYffSourceInfo = serverUtils.sourceService.findLastYffSourceByUserId(userid, 0, requestTime, usertype);
                        // 查找在有效期内QPS资源包（sourcetype = 1）失效日期最近的资源信息
                        List<Source> lastQpsSourceInfo = serverUtils.sourceService.findLastSourceByUserId(userid, 1, requestTime, usertype);

                        if(lastSourceInfo != null || lastSourceInfo.size() > 0) {
                            if (lastYffSourceInfo != null) {
                                Date sourceValidTime = lastYffSourceInfo.getValidtime();

                                long end1 = System.currentTimeMillis();

                                // 2.3 调用人脸检测服务
                                Map<String, String> contain_content = new HashMap<>();
                                Map<String, Response_result> result_content = new HashMap<>();

                                int restCount, count;

                                if(model_type == 0) {
                                    log.info("mtcnn-pic-第三方用户-"+userid);
                                    byte[] rgbByte = null;
                                    int width = 0, height = 0;
                                    String image_base64 = object.getString("content");
                                    if (image_base64 == null || image_base64 == "" || image_base64.length() == 0) {
                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：无效的type");

                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                        log.info("mtcnn-pic-第三方用户-"+userid+"-1030#参数错误：无效的type");
                                        vo.getResult().setResult(new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id));
                                    }else{
                                        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
                                        if (!image_base64.matches(base64Pattern)) {
                                            Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");

                                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                            log.info("mtcnn-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                            vo.getResult().setResult(result1);
                                        }else{
                                            // 将 base64 字符转转为二进制数组类型
                                            byte[] decoderBytes = Base64Utils.decodeFromString(image_base64);

                                            try{
                                                // 二进制输入流
                                                InputStream in = new ByteArrayInputStream(decoderBytes);
                                                BufferedImage ImageOne = ImageIO.read(in);
                                                in.close();

                                                if(StringUtils.isEmpty(ImageOne)){
                                                    MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                                    //第三方用户
                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                    log.info("mtcnn-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                                    vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                                }else {
                                                    //可以获得宽和高了
                                                    height = ImageOne.getHeight();
                                                    width = ImageOne.getWidth();

                                                    rgbByte = ImageUtiles.image2ByteRgb(ImageOne);

                                                    long startTime1 = System.currentTimeMillis();

                                                    Map<String, Object> res = new HashMap<>();
                                                    //            res.put("request_id", request_id);
                                                    List<Object> face_list = new ArrayList<>();
                                                    int value = Math.min(height, width) / 3;
                                                    //                                    mtcnn_multi.setMinSize(value, modelId-1);
                                                    //图片参数为rgb的byte数组
                                                    Vector<Box> boxes = mtcnn_multi.detectFaces(rgbByte, width, height, modelId - 1); //图片参数为byte数组

                                                    res.put("face_num", boxes.size());

                                                    //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
                                                    if (!boxes.isEmpty()) {
                                                        for (int i = 0; i < boxes.size(); i++) {
                                                            Map<String, Object> face_rect = new HashMap<>();
                                                            Map<String, Object> angle = new HashMap<>();
                                                            Map<String, Object> score = new HashMap<>();
                                                            Map<String, Object> res1 = new HashMap<>();

                                                            int[] box = boxes.get(i).box;
                                                            float score_value = boxes.get(i).score;
                                                            float[] angle_value = boxes.get(i).angles;

                                                            face_rect.put("left", box[0]);
                                                            face_rect.put("top", box[1]);
                                                            face_rect.put("right", box[2]);
                                                            face_rect.put("bottom", box[3]);

                                                            angle.put("yaw", angle_value[1]);
                                                            angle.put("pitch", angle_value[2]);
                                                            angle.put("roll", angle_value[0]);

                                                            score.put("score", score_value);


                                                            res1.put("face_rect", face_rect);
                                                            res1.put("pose", angle);
                                                            res1.put("quality", score);

                                                            face_list.add(res1);

                                                        }
                                                    }

                                                    res.put("face_list", face_list);
                                                    //mtcnn_multi.releaseDetect(); //检测完成后释放c++对象
                                                    long endTime1 = System.currentTimeMillis();
                                                    long detectTime1 = endTime1 - startTime1;
                                                    res.put("detect_time", detectTime1);

                                                    Response_result result = new Response_result().success(request_id, res);

                                                    //Response_result result = new Response_result().success( request_id,"请求成功！");

                                                    //更新剩余使用次数restcount字段值
                                                    //先查询剩余次数
                                                    //int newCount= serverUtils.sourceService.findRestCount(userid, 0, sourceValidTime);
                                                    //再更新
                                                    //serverUtils.sourceService.updateCount(userid, 0, sourceValidTime, newCount);
                                                    //查询跟更新放到一起
                                                    int mtcnn_update = serverUtils.sourceService.updateRestCount(userid, 0, sourceValidTime, usertype);
                                                    if (mtcnn_update == 1) {
                                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                        //调用成功
                                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 100000);
                                                        log.info("mtcnn-pic-第三方用户-"+userid+"-检测结果："+String.valueOf(res));
                                                        vo.getResult().setResult(result);
                                                    } else {
                                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1033#有效期限内预付费资源使用次数到达上限");
                                                        log.info("mtcnn-pic-第三方用户-"+userid+"-1033#有效期限内预付费资源使用次数到达上限");
                                                        vo.getResult().setResult(new Response_result().fail("有效期限内预付费资源调用次数到达上限!", 1033, request_id));
                                                    }
                                                }
                                            }catch (IOException e2){
                                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                                //内部子系统用户
                                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                log.info("mtcnn-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                                vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                            }
                                        }
                                    }
                                }else if(model_type == 1){
                                    log.info("megdetect-pic-第三方用户-"+userid);
                                    byte[] jpgByte = null;
                                    String image_base64 = object.getString("content");
                                    if (image_base64 == null || image_base64.length() == 0 || image_base64 == "") {
                                        Response_result result = new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id);

                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：无效的type");
                                        log.info("megdetect-pic-第三方用户-"+userid+"-1030#参数错误：无效的type");
                                        vo.getResult().setResult(result);
                                    }else{
                                        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
                                        if (!image_base64.matches(base64Pattern)) {
                                            Response_result result = new Response_result().fail("参数错误：无效的image content", 1032, request_id);

                                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                            log.info("megdetect-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                            vo.getResult().setResult(result);
                                        }else{
                                            jpgByte = Base64Utils.decodeFromString(image_base64);
                                            long startTime2 = System.currentTimeMillis();

                                            try {
                                                // 二进制输入流
                                                InputStream in = new ByteArrayInputStream(jpgByte);
                                                BufferedImage ImageOne = ImageIO.read(in);
                                                in.close();

                                                if (StringUtils.isEmpty(ImageOne)) {
                                                    Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
                                                    MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 300000, "1032#参数错误：无效的image content");
                                                    //内部子系统用户
                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                    log.info("megdetect-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                                    vo.getResult().setResult(result1);
                                                } else {
                                                    int value = Math.min(ImageOne.getWidth(), ImageOne.getHeight()) / 3;
                                                    // megdetect_multi.setMinSize(value, modelId-1);
                                                    //图片参数为jpg的byte数组
                                                    Vector<FaceRect> boxes = megdetect_multi.detectFacesByJpgByte(jpgByte, modelId - 1); //图片参数为byte数组

                                                    Map<String, Object> res = new HashMap<>();
                                                    res.put("face_num", boxes.size());

                                                    List<Object> face_list = new ArrayList<>();

                                                    //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
                                                    if (!boxes.isEmpty()) {
                                                        for (int i = 0; i < boxes.size(); i++) {
                                                            Map<String, Object> face_rect = new HashMap<>();
                                                            Map<String, Object> pose = new HashMap<>();
                                                            Map<String, Object> quality = new HashMap<>();
                                                            Map<String, Object> res1 = new HashMap<>();

                                                            face_rect.put("left", boxes.get(i).face_rect[0]);
                                                            face_rect.put("top", boxes.get(i).face_rect[1]);
                                                            face_rect.put("right", boxes.get(i).face_rect[2]);
                                                            face_rect.put("bottom", boxes.get(i).face_rect[3]);

                                                            pose.put("yaw", boxes.get(i).pose[2]);
                                                            pose.put("pitch", boxes.get(i).pose[1]);
                                                            pose.put("roll", boxes.get(i).pose[0]);

                                                            quality.put("blur", boxes.get(i).quality[0]);
                                                            quality.put("face_brightness", boxes.get(i).quality[1]);
                                                            quality.put("brightness_deviation", boxes.get(i).quality[2]);


                                                            res1.put("face_rect", face_rect);
                                                            res1.put("pose", pose);
                                                            res1.put("quality", quality);
                                                            res1.put("face_completeness", boxes.get(i).face_completeness);
                                                            res1.put("errcode", boxes.get(i).errcode);

                                                            face_list.add(res1);
                                                        }
                                                    }

                                                    res.put("face_list", face_list);
                                                    //                              megdetect.releaseDetect(); //检测完成后释放c++对象
                                                    long endTime2 = System.currentTimeMillis();
                                                    long detectTime2 = endTime2 - startTime2;
                                                    res.put("detect_time", detectTime2);

                                                    Response_result result = new Response_result().success(request_id, res);

                                                    int megdetect_update = serverUtils.sourceService.updateRestCount(userid, 0, sourceValidTime, usertype);
                                                    if (megdetect_update == 1) {
                                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                        //调用成功
                                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id, 100000);
                                                        log.info("megdetect-pic-第三方用户-"+userid+"-检测结果："+String.valueOf(res));
                                                        vo.getResult().setResult(result);
                                                    } else {
                                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1033#有效期限内预付费资源使用次数到达上限");
                                                        log.info("megdetect-pic-第三方用户-"+userid+"-1033#有效期限内预付费资源使用次数到达上限");
                                                        vo.getResult().setResult(new Response_result().fail("有效期限内预付费资源调用次数到达上限!", 1033, request_id));
                                                    }
                                                }
                                            }catch (IOException e2){
                                                Response_result result = new Response_result().fail("参数错误：无效的image content", 1032, request_id);

                                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1032#参数错误：无效的image content");
                                                log.info("megdetect-pic-第三方用户-"+userid+"-1032#参数错误：无效的image content");
                                                vo.getResult().setResult(new Response_result().fail("参数错误：无效的image content", 1032, request_id));
                                            }
                                        }
                                    }
                                }else{
                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
                                    MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1030#参数错误：参数错误：无效的type");
                                    log.info("megdetect-pic-第三方用户-"+userid+"-1030#参数错误：参数错误：无效的type");
                                    vo.getResult().setResult(new Response_result().fail("参数错误：无效的type", 1030, request_id));
                                }
                            }else{
                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, null, usertype, AccessKey_Id, source_ip);
                                //资源服务已使用完
                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1033#有效期限内预付费资源使用次数到达上限");
                                log.info("pic-第三方用户-"+userid+"-1033#有效期限内预付费资源使用次数到达上限");
                                vo.getResult().setResult(new Response_result().fail("有效期限内预付费资源调用次数到达上限!", 1033, request_id));
                            }
                        }else {
                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, null, usertype, AccessKey_Id, source_ip);
                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,300000, "1034#预付费资源包已超过购买的有效期限");
                            log.info("pic-第三方用户-"+userid+"-1034#预付费资源包已超过购买的有效期限");
                            vo.getResult().setResult(new Response_result().fail("预付费资源包已超过购买的有效期限", 1034, request_id));
                        }
                    }
                }else{
                    //AK校验失败
                    log.info("1035#AK校验失败");
                    vo.getResult().setResult( new Response_result().fail(accessResult.getBody(), 1035, request_id));
                }

            } catch (Exception e) {
                log.info("1001#检测异常");
                vo.getResult().setResult(new Response_result().fail("检测异常", 1001, null));
                e.printStackTrace();
//                running = false;
            }
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /*
     * 判断是否为url
     */
    public static boolean isURL(String str){
        //转换为小写
        str = str.toLowerCase();
        String regex = "^((https|http|ftp|rtsp|mms)?://)"  //https、http、ftp、rtsp、mms
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184
                + "|" // 允许IP和DOMAIN（域名）
                + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
                + "[a-z]{2,6})" // first level domain- .com or .museum
                + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
                + "((/?)|" // a slash isn't required if there is no file name
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";

        return  str.matches(regex);
    }

    // 将ip转换为定长8个字符的16进制表示形式：255.255.255.255 -> FFFFFFFF
    private static String hexIp(String ip) {
        StringBuilder sb = new StringBuilder();
        for (String seg : ip.split("\\.")) {
            String h = Integer.toHexString(Integer.parseInt(seg));
            if (h.length() == 1) sb.append("0");
            sb.append(h);
        }
        return sb.toString();
    }

    private static String getIpAddr(Map<String, String> headers) {
        String ipAddress = headers.get("x-forwarded-for");
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.get("Proxy-Client-IP");
        }
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.get("WL-Proxy-Client-IP");
        }
        if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if(ipAddress!=null && ipAddress.length()>15){ //"***.***.***.***".length() = 15
            if(ipAddress.indexOf(",")>0){
                ipAddress = ipAddress.substring(0,ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

}






    //去掉签名校验
//    @SneakyThrows
//    @Override
//    public void run() {
//        while (running) {
////            try {
//                AsyncVo<String,Map<String, String>,String, Response_result> vo = queue.getHttpQueue().take();
////                System.out.println("[ HttpTask ]开始处理订单");
//
//            try {
//                String params = vo.getParams();
//                Map<String, String> headers = vo.getHeaders();
////                String request_id = vo.getRequestid();
//                //AtomicLong lastId = new AtomicLong();      // 自增id，用于requestId的生成过程
//                Random r = new Random();
//                int randomDate = r.nextInt(10000);
//                long startTimeStamp = System.currentTimeMillis();
////              String ip = headers.get("host").split(":")[0];
//                String ip = "192.168.9.1";
////                String request_id = hexIp(ip) + Long.toString(startTimeStamp, Character.MAX_RADIX) + "-" + lastId.incrementAndGet();
//                String request_id = hexIp(ip) + Long.toString(startTimeStamp, Character.MAX_RADIX) + "-" + randomDate;
//
//
//                String request_time = headers.get("date");
//                SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
//                df.setTimeZone(new SimpleTimeZone(0, "GMT"));
//                //        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                Date requestTime =df.parse(request_time);
//
//                String authorization = headers.get("authorization");
//                String[] accessKeyValue = authorization.split(" ");
//                String AccessKey_Id = accessKeyValue[1].split(":")[0];
//                String source_ip = headers.get("host").split(":")[0];
//
//                JSONObject object = JSON.parseObject(params);
//                //model_type选用的模型，0：mtcnn   1:face++;  para_type图片参数，0：图片url   1:图片的base64编码
//
//                Integer model_type = object.getInteger("model_type");
//                Integer para_type = object.getInteger("type");
//
//
//                // 2、根据key_id调sdk获得用户Id和资源类型(内部子系统用户调用为true-1；第三方用户调用的为false-0)
////                String userid = AccessKey_Id;
//                //        String userid = "5k8264iLtKCh16Cq1231m";
////                int usertype = 0;
////                String threadId = currentThread().getName();
//                int modelId = Integer.parseInt(currentThread().getName().split("-")[3]);
//                System.out.println("当前线程名"+currentThread().getName()+"模型名称："+modelId);
//                log.info("当前线程名"+currentThread().getName()+"模型名称："+modelId);
//
//                //        int sourcetype = 0;
//
//
//                // 1、验证签名是否合法，根据key_id调sdk获得签名，比较签名是否一致
//
//                long startTime0 = System.currentTimeMillis();
////                SignDTO sign = new SignDTO();
////                sign.setMethod("POST");
////                sign.setAccept(headers.get("accept"));
////                sign.setContentType(headers.get("content-type"));
////                sign.setPath("/api/face/detect");
////                sign.setBody(params);
////                sign.setTimestamp(headers.get("date"));
////                sign.setSign(headers.get("authorization"));
////                sign.setIp(source_ip);
////                SimpleResult accessResult = MipsAccessKeyApiUrl.INSTANCE.validationSign(sign);
////                if(accessResult.getResult().equals("SUCCESS")){
//                // 2、根据key_id调sdk获得用户Id和资源类型(内部子系统用户调用为true-1；第三方用户调用的为false-0)
//                //{"company_code":"2990757318216068","create_time":1586508297000,"is_default":true,"status":"0"}
//                //company_code:用户ID  is_default:是否使用默认的资源包（内部子系统调用），非默认的使用付费资源包（第三方用户调用）
////                    String signatureResult = accessResult.getBody();
////                    JSONObject jsonResult = JSON.parseObject(signatureResult);
////
////                    String userid = jsonResult.getString("company_code");
//                String userid = "2990757318216068";
////                    boolean is_default = jsonResult.getBooleanValue("is_default");
//                boolean is_default = true;
////                    String ak_status = jsonResult.getString("status");
////                    Long snow_flake_id = Long.parseLong(jsonResult.getString("snow_flake_id"));
//
//                //is_default=0默认的key，内部子系统user_type=1；is_default=1不是默认的key，第三方用户user_type=0
//                int usertype = 1;
//
//                if(is_default){
//                    //内部子系统用户
//                    usertype = 1;
//
//                    Date sourceValidTime = null;
//
//                    long end1 = System.currentTimeMillis();
//                    //                      long t1 = end1 - start;
//                    //                      System.out.println("异步调用中调用数据库耗时："+t1);
//
//                    // 2.3 调用人脸检测服务
//                    Map<String, String> contain_content = new HashMap<>();
//                    Map<String, Response_result> result_content = new HashMap<>();
//
//
//                    int restCount, count;
//
//                    if(model_type == 0) {
//                        byte[] rgbByte = null;
//                        int width = 0, height = 0;
//                        String image_base64 = object.getString("content");
//                        if (image_base64 == null || image_base64 == "" || image_base64.length() == 0) {
//                            Response_result result1 = new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id);
////                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
////                                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                            //内部子系统用户
//                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                            vo.getResult().setResult(result1);
//                        }else{
//                            String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
//                            if (!image_base64.matches(base64Pattern)) {
//                                Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                //内部子系统用户
//                                serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                vo.getResult().setResult(result1);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                            }else{
//                                    // 将 base64 字符转转为二进制数组类型
//                                    byte[] decoderBytes = Base64Utils.decodeFromString(image_base64);
//
//                                    try {
//                                        // 二进制输入流
//                                        InputStream in = new ByteArrayInputStream(decoderBytes);
//
//                                        BufferedImage ImageOne = ImageIO.read(in);
//                                        in.close();
//                                        if (StringUtils.isEmpty(ImageOne)) {
//                                            Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                            //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                            //内部子系统用户
//                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                            vo.getResult().setResult(result1);
//                                        } else {
//                                            //可以获得宽和高了
//                                            height = ImageOne.getHeight();
//                                            width = ImageOne.getWidth();
//
//                                            rgbByte = ImageUtiles.image2ByteRgb(ImageOne);
//
//
//                                            long startTime1 = System.currentTimeMillis();
//
//                                            Map<String, Object> res = new HashMap<>();
//                                            //            res.put("request_id", request_id);
//                                            List<Object> face_list = new ArrayList<>();
//                                            int value = Math.min(height, width) / 3;
//                                            //                                            mtcnn_multi.setMinSize(value, modelId-1);
//                                            //图片参数为rgb的byte数组
//                                            Vector<Box> boxes = mtcnn_multi.detectFaces(rgbByte, width, height, modelId - 1); //图片参数为byte数组
//
//                                            res.put("face_num", boxes.size());
//
//                                            //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
//                                            if (!boxes.isEmpty()) {
//                                                for (int i = 0; i < boxes.size(); i++) {
//                                                    Map<String, Object> face_rect = new HashMap<>();
//                                                    Map<String, Object> angle = new HashMap<>();
//                                                    Map<String, Object> score = new HashMap<>();
//                                                    Map<String, Object> res1 = new HashMap<>();
//
//                                                    int[] box = boxes.get(i).box;
//                                                    float score_value = boxes.get(i).score;
//                                                    float[] angle_value = boxes.get(i).angles;
//
//                                                    face_rect.put("left", box[0]);
//                                                    face_rect.put("top", box[1]);
//                                                    face_rect.put("right", box[2]);
//                                                    face_rect.put("bottom", box[3]);
//
//                                                    angle.put("yaw", angle_value[1]);
//                                                    angle.put("pitch", angle_value[2]);
//                                                    angle.put("roll", angle_value[0]);
//
//                                                    score.put("score", score_value);
//
//
//                                                    res1.put("face_rect", face_rect);
//                                                    res1.put("pose", angle);
//                                                    res1.put("quality", score);
//
//                                                    face_list.add(res1);
//
//                                                }
//                                            }
//
//
//                                            res.put("face_list", face_list);
//                                            //                          mtcnn_multi.releaseDetect(); //检测完成后释放c++对象
//                                            long endTime1 = System.currentTimeMillis();
//                                            long detectTime1 = endTime1 - startTime1;
//                                            res.put("detect_time", detectTime1);
//
//                                            Response_result result = new Response_result().success(request_id, res);
//
//                                            //内部子系统用户
//                                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//
//                                            //调用成功
//                                            //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,100000);
//                                            vo.getResult().setResult(result);
//                                        }
//                                    }catch (IOException e2){
//                                        Response_result result = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
////                                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                        //内部子系统用户
//                                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
////                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                        vo.getResult().setResult(result);
//                                    }
//
//                                }
//                            }
//
//                        System.out.println("httptask当前线程名：+" + Thread.currentThread().getName() + "*****" + Thread.currentThread().getId());
//                        log.info("httptask当前线程名：+" + Thread.currentThread().getName() + "*****" + Thread.currentThread().getId());
//                    }else if(model_type == 1){
//                        byte[] jpgByte = null;
//                        String image_base64 = object.getString("content");
//                        if (image_base64 == null || image_base64.length() == 0 || image_base64 == "") {
//                            Response_result result = new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id);
////                                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                            //内部子系统用户
//                            serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
////                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                            vo.getResult().setResult(result);
//                        }else{
//                            String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
//                            if (!image_base64.matches(base64Pattern)) {
//                                Response_result result = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                //内部子系统用户
//                                serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                vo.getResult().setResult(result);
//                            }else{
//                                jpgByte = Base64Utils.decodeFromString(image_base64);
//                                long startTime2 = System.currentTimeMillis();
//
//                                try {
//                                    // 二进制输入流
//                                    InputStream in = new ByteArrayInputStream(jpgByte);
//                                    BufferedImage ImageOne = ImageIO.read(in);
//                                    in.close();
//
//                                    if (StringUtils.isEmpty(ImageOne)) {
//                                        Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                        //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                        //内部子系统用户
//                                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                        vo.getResult().setResult(result1);
//                                    } else {
//                                        int value = Math.min(ImageOne.getWidth(), ImageOne.getHeight()) / 3;
//                                        //                                            megdetect_multi.setMinSize(value, modelId-1);
//                                        //图片参数为jpg的byte数组
//                                        Vector<FaceRect> boxes = megdetect_multi.detectFacesByJpgByte(jpgByte, modelId - 1); //图片参数为byte数组
//
//
//                                        Map<String, Object> res = new HashMap<>();
//                                        res.put("face_num", boxes.size());
//
//                                        List<Object> face_list = new ArrayList<>();
//
//                                        //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
//                                        if (!boxes.isEmpty()) {
//                                            for (int i = 0; i < boxes.size(); i++) {
//                                                Map<String, Object> face_rect = new HashMap<>();
//                                                Map<String, Object> pose = new HashMap<>();
//                                                Map<String, Object> quality = new HashMap<>();
//                                                Map<String, Object> res1 = new HashMap<>();
//
//                                                face_rect.put("left", boxes.get(i).face_rect[0]);
//                                                face_rect.put("top", boxes.get(i).face_rect[1]);
//                                                face_rect.put("right", boxes.get(i).face_rect[2]);
//                                                face_rect.put("bottom", boxes.get(i).face_rect[3]);
//
//                                                pose.put("yaw", boxes.get(i).pose[2]);
//                                                pose.put("pitch", boxes.get(i).pose[1]);
//                                                pose.put("roll", boxes.get(i).pose[0]);
//
//                                                quality.put("blur", boxes.get(i).quality[0]);
//                                                quality.put("face_brightness", boxes.get(i).quality[1]);
//                                                quality.put("brightness_deviation", boxes.get(i).quality[2]);
//
//
//                                                res1.put("face_rect", face_rect);
//                                                res1.put("pose", pose);
//                                                res1.put("quality", quality);
//                                                res1.put("face_completeness", boxes.get(i).face_completeness);
//                                                res1.put("errcode", boxes.get(i).errcode);
//
//
//                                                face_list.add(res1);
//
//                                            }
//                                        }
//
//                                        res.put("face_list", face_list);
//                                        //                              megdetect.releaseDetect(); //检测完成后释放c++对象
//                                        long endTime2 = System.currentTimeMillis();
//                                        long detectTime2 = endTime2 - startTime2;
//                                        res.put("detect_time", detectTime2);
//
//                                        Response_result result = new Response_result().success(request_id, res);
//
//                                        //
//                                        //内部子系统用户
//                                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//
//                                        //调用成功
//                                        //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,100000);
//                                        vo.getResult().setResult(result);
//                                    }
//                                }catch (IOException e2){
//                                    Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                    //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                    //内部子系统用户
//                                    serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                    vo.getResult().setResult(result1);
//                                }
//                            }
//                        }
//
//                        System.out.println("httptask的megdetect模型图片base64的onEvent检测结束------------"+request_id);
//                        log.info("httptask的megdetect模型图片base64的onEvent检测结束------------"+request_id);
//                    }else{
////                                serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                        //内部子系统用户
//                        serverUtils.requestRecordService.insertInnerUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
////                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                        vo.getResult().setResult(new Response_result().fail("参数错误：无效的type", 1030, request_id));
//                    }
//                }else{
//                    //第三方用户
//                    usertype = 0;
//
//                    //                    Response_result result = new Response_result().success( request_id,"请求成功！");
////                    vo.getResult().setResult(result);
//
//                    // 根据用户ID查看用户拥有的资源包(预付费与QPS,可能有多个)
//                    // 查找在有效期内预付费资源包(sourcetype = 0)失效日期最近的资源信息
//                    List<Source> lastSourceInfo = serverUtils.sourceService.findLastSourceByUserId(userid, 0, requestTime,usertype);
//                    // 查找在有效期内预付费资源包(sourcetype = 0)失效日期最近的且可用次数小于总共可使用次数的资源信息（调用次数是否超过限制？）,失效日期有多个，选使用次数较多的那个资源
//                    Source lastYffSourceInfo = serverUtils.sourceService.findLastYffSourceByUserId(userid, 0, requestTime, usertype);
//                    // 查找在有效期内QPS资源包（sourcetype = 1）失效日期最近的资源信息
//                    List<Source> lastQpsSourceInfo = serverUtils.sourceService.findLastSourceByUserId(userid, 1, requestTime, usertype);
//
//                    if(lastSourceInfo != null || lastSourceInfo.size() > 0) {
//                        if (lastYffSourceInfo != null) {
//                            Date sourceValidTime = lastYffSourceInfo.getValidtime();
//
//                            long end1 = System.currentTimeMillis();
//                            //                      long t1 = end1 - start;
//                            //                      System.out.println("异步调用中调用数据库耗时："+t1);
//
//                            // 2.3 调用人脸检测服务
//                            Map<String, String> contain_content = new HashMap<>();
//                            Map<String, Response_result> result_content = new HashMap<>();
//
//
//                            int restCount, count;
//
//                            if(model_type == 0) {
//                                byte[] rgbByte = null;
//                                int width = 0, height = 0;
//                                String image_base64 = object.getString("content");
//                                if (image_base64 == null || image_base64 == "" || image_base64.length() == 0) {
//                                    Response_result result1 = new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id);
////                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
////                                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                    vo.getResult().setResult(result1);
//                                }else{
//                                    String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
//                                    if (!image_base64.matches(base64Pattern)) {
//                                        Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//
//                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                        vo.getResult().setResult(result1);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                    }else{
//                                        // 将 base64 字符转转为二进制数组类型
//
//                                        byte[] decoderBytes = Base64Utils.decodeFromString(image_base64);
//
//                                        try {
//                                            // 二进制输入流
//                                            InputStream in = new ByteArrayInputStream(decoderBytes);
//                                            BufferedImage ImageOne = ImageIO.read(in);
//                                            in.close();
//
//                                            if (StringUtils.isEmpty(ImageOne)) {
//                                                Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                                //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                                //内部子系统用户
//                                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                                vo.getResult().setResult(result1);
//                                            } else {
//                                                //可以获得宽和高了
//                                                height = ImageOne.getHeight();
//                                                width = ImageOne.getWidth();
//
//                                                rgbByte = ImageUtiles.image2ByteRgb(ImageOne);
//
//                                                long startTime1 = System.currentTimeMillis();
//
//                                                Map<String, Object> res = new HashMap<>();
//                                                //            res.put("request_id", request_id);
//                                                List<Object> face_list = new ArrayList<>();
//                                                int value = Math.min(height, width) / 3;
////                                            mtcnn_multi.setMinSize(value, modelId-1);
//                                                //图片参数为rgb的byte数组
//                                                Vector<Box> boxes = mtcnn_multi.detectFaces(rgbByte, width, height, modelId - 1); //图片参数为byte数组
//
//                                                res.put("face_num", boxes.size());
//
//                                                //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
//                                                if (!boxes.isEmpty()) {
//                                                    for (int i = 0; i < boxes.size(); i++) {
//                                                        Map<String, Object> face_rect = new HashMap<>();
//                                                        Map<String, Object> angle = new HashMap<>();
//                                                        Map<String, Object> score = new HashMap<>();
//                                                        Map<String, Object> res1 = new HashMap<>();
//
//                                                        int[] box = boxes.get(i).box;
//                                                        float score_value = boxes.get(i).score;
//                                                        float[] angle_value = boxes.get(i).angles;
//
//                                                        face_rect.put("left", box[0]);
//                                                        face_rect.put("top", box[1]);
//                                                        face_rect.put("right", box[2]);
//                                                        face_rect.put("bottom", box[3]);
//
//                                                        angle.put("yaw", angle_value[1]);
//                                                        angle.put("pitch", angle_value[2]);
//                                                        angle.put("roll", angle_value[0]);
//
//                                                        score.put("score", score_value);
//
//
//                                                        res1.put("face_rect", face_rect);
//                                                        res1.put("pose", angle);
//                                                        res1.put("quality", score);
//
//                                                        face_list.add(res1);
//
//                                                    }
//                                                }
//
//                                                res.put("face_list", face_list);
//                                                //                          mtcnn_multi.releaseDetect(); //检测完成后释放c++对象
//                                                long endTime1 = System.currentTimeMillis();
//                                                long detectTime1 = endTime1 - startTime1;
//                                                res.put("detect_time", detectTime1);
//
//                                                Response_result result = new Response_result().success(request_id, res);
//
////                                Response_result result = new Response_result().success( request_id,"请求成功！");
//
//                                                //更新剩余使用次数restcount字段值
//                                                //先查询剩余次数
//                                                //                          int newCount= serverUtils.sourceService.findRestCount(userid, 0, sourceValidTime);
//                                                //再更新
//                                                //                          serverUtils.sourceService.updateCount(userid, 0, sourceValidTime, newCount);
//                                                //查询跟更新放到一起
//                                                int mtcnn_update = serverUtils.sourceService.updateRestCount(userid, 0, sourceValidTime, usertype);
////                                            serverUtils.sourceService.updateTransactionalCount(userid, 0, sourceValidTime, usertype);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                if (mtcnn_update == 1) {
//                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                } else {
//                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                }
//
//                                                //调用成功
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,100000);
//                                                vo.getResult().setResult(result);
//                                            }
//                                        }catch (IOException e2){
//                                            Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                            //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                            //内部子系统用户
//                                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                            vo.getResult().setResult(result1);
//                                        }
//                                    }
//                                }
//                                System.out.println("httptask当前线程名：+" + Thread.currentThread().getName() + "*****" + Thread.currentThread().getId());
//                                log.info("httptask当前线程名：+" + Thread.currentThread().getName() + "*****" + Thread.currentThread().getId());
//                            }else if(model_type == 1){
//                                byte[] jpgByte = null;
//                                String image_base64 = object.getString("content");
//                                if (image_base64 == null || image_base64.length() == 0 || image_base64 == "") {
//                                    Response_result result = new Response_result().fail("传入图片type与图片内容不一致", 1030, request_id);
////                                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                     serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
////                                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                    vo.getResult().setResult(result);
//                                }else{
//                                    String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
//                                    if (!image_base64.matches(base64Pattern)) {
//                                        Response_result result = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                        vo.getResult().setResult(result);
//                                    }else{
//                                        jpgByte = Base64Utils.decodeFromString(image_base64);
//                                        long startTime2 = System.currentTimeMillis();
//
//                                        try {
//                                            // 二进制输入流
//                                            InputStream in = new ByteArrayInputStream(jpgByte);
//                                            BufferedImage ImageOne = ImageIO.read(in);
//                                            in.close();
//
//                                            if (StringUtils.isEmpty(ImageOne)) {
//                                                Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                                //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                                //内部子系统用户
//                                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                                vo.getResult().setResult(result1);
//                                            } else {
//                                                int value = Math.min(ImageOne.getWidth(), ImageOne.getHeight()) / 3;
////                                            megdetect_multi.setMinSize(value, modelId-1);
//                                                //图片参数为jpg的byte数组
//                                                Vector<FaceRect> boxes = megdetect_multi.detectFacesByJpgByte(jpgByte, modelId - 1); //图片参数为byte数组
//
//
//                                                Map<String, Object> res = new HashMap<>();
//                                                res.put("face_num", boxes.size());
//
//                                                List<Object> face_list = new ArrayList<>();
//
//                                                //如果一张图片有多个Bbox框，取质量最好（quality ok(根据errcode去确定，为0就是ok,非0就是bad) and blur biggest）的那个框
//                                                if (!boxes.isEmpty()) {
//                                                    for (int i = 0; i < boxes.size(); i++) {
//                                                        Map<String, Object> face_rect = new HashMap<>();
//                                                        Map<String, Object> pose = new HashMap<>();
//                                                        Map<String, Object> quality = new HashMap<>();
//                                                        Map<String, Object> res1 = new HashMap<>();
//
//                                                        face_rect.put("left", boxes.get(i).face_rect[0]);
//                                                        face_rect.put("top", boxes.get(i).face_rect[1]);
//                                                        face_rect.put("right", boxes.get(i).face_rect[2]);
//                                                        face_rect.put("bottom", boxes.get(i).face_rect[3]);
//
//                                                        pose.put("yaw", boxes.get(i).pose[2]);
//                                                        pose.put("pitch", boxes.get(i).pose[1]);
//                                                        pose.put("roll", boxes.get(i).pose[0]);
//
//                                                        quality.put("blur", boxes.get(i).quality[0]);
//                                                        quality.put("face_brightness", boxes.get(i).quality[1]);
//                                                        quality.put("brightness_deviation", boxes.get(i).quality[2]);
//
//
//                                                        res1.put("face_rect", face_rect);
//                                                        res1.put("pose", pose);
//                                                        res1.put("quality", quality);
//                                                        res1.put("face_completeness", boxes.get(i).face_completeness);
//                                                        res1.put("errcode", boxes.get(i).errcode);
//
//
//                                                        face_list.add(res1);
//
//                                                    }
//                                                }
//
//                                                res.put("face_list", face_list);
////                              megdetect.releaseDetect(); //检测完成后释放c++对象
//                                                long endTime2 = System.currentTimeMillis();
//                                                long detectTime2 = endTime2 - startTime2;
//                                                res.put("detect_time", detectTime2);
//
//                                                Response_result result = new Response_result().success(request_id, res);
//
////                                Response_result result = new Response_result().success( request_id,"请求成功！");
//                                                int megdetect_update = serverUtils.sourceService.updateRestCount(userid, 0, sourceValidTime, usertype);
////                                            serverUtils.sourceService.updateTransactionalCount(userid, 0, sourceValidTime, usertype);
////                                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                if (megdetect_update == 1) {
//                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 1, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                } else {
//                                                    serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//                                                }
//
//                                                //调用成功
////                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,100000);
//                                                vo.getResult().setResult(result);
//                                            }
//                                        }catch (IOException e2){
//                                            Response_result result1 = new Response_result().fail("参数错误：无效的image content", 1032, request_id);
//                                            //                                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                            //内部子系统用户
//                                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                            vo.getResult().setResult(result1);
//                                        }
//                                    }
//                                }
//
//                                System.out.println("httptask的megdetect模型图片base64的onEvent检测结束------------"+request_id);
//                                log.info("httptask的megdetect模型图片base64的onEvent检测结束------------"+request_id);
//                            }else{
////                                serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
//
//                                serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, sourceValidTime, usertype, AccessKey_Id, source_ip);
////                                MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200004);
//                                vo.getResult().setResult(new Response_result().fail("参数错误：无效的type", 1030, request_id));
//                            }
//                        }else{
////                            serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, null, usertype, AccessKey_Id, source_ip);
//
//                            serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, null, usertype, AccessKey_Id, source_ip);
//                            //资源服务已使用完
////                            MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200000);
//                            vo.getResult().setResult(new Response_result().fail("有效期限内预付费资源调用次数到达上限!", 1033, request_id));
//                        }
//                    }else {// QPS资源包在这里处理
////                        serverUtils.requestRecordService.insertRequestRecord(userid, 0, request_id, requestTime, 0, null, usertype, AccessKey_Id, source_ip);
//
//                        serverUtils.requestRecordService.insertOuterUserRecord(userid, 0, request_id, new Date(), 0, null, usertype, AccessKey_Id, source_ip);
////                        MipsAccessKeyApiUrl.INSTANCE.returnResult(snow_flake_id,200003);
//                        vo.getResult().setResult(new Response_result().fail("预付费资源包已超过购买的有效期限!", 1034, request_id));
//                    }
//                }
//
////                }else{
////                    //                return new Response_result().fail(accessResult.getBody(), 1035, request_id);
////                    Response_result result = new Response_result().fail(accessResult.getBody(), 1035, request_id);
////                    vo.getResult().setResult(result);
////                }
//
//            } catch (Exception e) {
//                Response_result result1 = new Response_result().fail("参数异常", 1032, null);
//                vo.getResult().setResult(result1);
//                e.printStackTrace();
////                running = false;
//            }
//        }
//    }
//
//    public void setRunning(boolean running) {
//        this.running = running;
//    }
//
//    /*
//     * 判断是否为url
//     */
//    public static boolean isURL(String str){
//        //转换为小写
//        str = str.toLowerCase();
//        String regex = "^((https|http|ftp|rtsp|mms)?://)"  //https、http、ftp、rtsp、mms
//                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
//                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL- 例如：199.194.52.184
//                + "|" // 允许IP和DOMAIN（域名）
//                + "([0-9a-z_!~*'()-]+\\.)*" // 域名- www.
//                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
//                + "[a-z]{2,6})" // first level domain- .com or .museum
//                + "(:[0-9]{1,5})?" // 端口号最大为65535,5位数
//                + "((/?)|" // a slash isn't required if there is no file name
//                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";
//
//        return  str.matches(regex);
//    }
//
//    // 将ip转换为定长8个字符的16进制表示形式：255.255.255.255 -> FFFFFFFF
//    private static String hexIp(String ip) {
//        StringBuilder sb = new StringBuilder();
//        for (String seg : ip.split("\\.")) {
//            String h = Integer.toHexString(Integer.parseInt(seg));
//            if (h.length() == 1) sb.append("0");
//            sb.append(h);
//        }
//        return sb.toString();
//    }
//
//}


