package com.smdt.mips.faceDetect.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.properties")
public class ConfigBean {

    public static String signUrl;
    @Value("${signature.sign_url}")
    public void setSign_Url(String sign_url) {
        signUrl = sign_url;
    }

    public static String logUrl;
    @Value("${signature.log_url}")
    public void setLog_Url(String log_url) {
        logUrl = log_url;
    }

    public static String redisIp;
    @Value("${signature.redis_ip}")
    public void setRedis_Ip(String redis_ip) {
        redisIp = redis_ip;
    }

    public static String redisPort;
    @Value("${signature.redis_port}")
    public void setRedis_Port(String redis_port) {
        redisPort = redis_port;
    }

    public static String minSizeMtcnn;
    @Value("${model.min_size_mtcnn}")
    public void setMin_Size_Mtcnn(String min_size_mtcnn) {
        minSizeMtcnn = min_size_mtcnn;
    }

    public static String mtcnnNum;
    @Value("${model.mtcnn_num}")
    public void setMtcnn_Num(String mtcnn_num) {
        mtcnnNum = mtcnn_num;
    }

    public static String megdetectNum;
    @Value("${model.megdetect_num}")
    public void setMegdetect_Num(String megdetect_num) {
        megdetectNum = megdetect_num;
    }

    public static String requestUrlTimeOut;
    @Value("${request.timeout}")
    public void setRequest_Time_Out(String timeout) {
        requestUrlTimeOut = timeout;
    }

    public static String megdetectModelPath;
    @Value("${model.megdetect_model_path}")
    public void setMegdetect_Model_Path(String megdetect_model_path) {
        megdetectModelPath = megdetect_model_path;
    }

    public static String mtcnnModelPath;
    @Value("${model.mtcnn_model_path}")
    public void setMtcnn_Model_Path(String mtcnn_model_path) {
        mtcnnModelPath = mtcnn_model_path;
    }

    public static String minSizeMegdetect;
    @Value("${model.min_size_megdetect}")
    public void setMin_Size_Megdetect(String min_size_megdetect) {
        minSizeMegdetect = min_size_megdetect;
    }


}
