package com.smdt.mips.faceDetect.utils;

import com.smdt.mips.faceDetect.megdetect.MegDetect;
import com.smdt.mips.faceDetect.mtcnn.Mtcnn;
import com.smdt.mips.sdk.MipsAccessKeyApiUrl;

import java.util.Properties;

public class InitKit {

    //初始化模型
    public static Mtcnn mtcnn_multi;
    public static MegDetect megdetect_multi;


    public static void start() {
        Properties properties = Config.getProperties();
        megdetect_multi = new MegDetect(properties.getProperty("megdetectModelPath"), Integer.parseInt(properties.getProperty("minSizeMegdetect")), Integer.parseInt(properties.getProperty("megdetectNum")));

        //初始化mtcnn模型 System.getProperty("user.dir") + "/models/mtcnn_model"
        mtcnn_multi = new Mtcnn(properties.getProperty("mtcnnModelPath"), Integer.parseInt(properties.getProperty("minSizeMtcnn")), Integer.parseInt(properties.getProperty("mtcnnNum")));

        try {
            String url = properties.getProperty("signUrl","127.0.0.1");
            String logUrl = properties.getProperty("logUrl","127.0.0.1");
            String ip = properties.getProperty("redisIp","127.0.0.1");
            String port =  properties.getProperty("redisPort","80");
            int redisPort = Integer.parseInt(port);
            MipsAccessKeyApiUrl.INSTANCE.init(url, logUrl, ip, redisPort);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}