package com.smdt.mips.faceDetect.utils;


import org.springframework.stereotype.Component;

import java.util.Properties;


@Component
public class Config
{

    public static Properties getProperties()
    {
        Properties properties = new Properties();

        //键的首字母必须大写
        properties.setProperty("AccessKey", "xxxxxxxxx");
        //
        properties.setProperty("SecretKey", "xxxxxxxxxxxxxxxxxx");
        //
        properties.setProperty("SendMsgTimeoutMillis", "3000");


        properties.put("signUrl", ConfigBean.signUrl);
        properties.put("logUrl", ConfigBean.logUrl);
        properties.put("redisIp", ConfigBean.redisIp);
        properties.put("redisPort", ConfigBean.redisPort);
        properties.put("minSizeMtcnn", ConfigBean.minSizeMtcnn);
        properties.put("mtcnnNum", ConfigBean.mtcnnNum);
        properties.put("megdetectNum", ConfigBean.megdetectNum);
        properties.put("requestUrlTimeOut", ConfigBean.requestUrlTimeOut);
        properties.put("megdetectModelPath", ConfigBean.megdetectModelPath);
        properties.put("mtcnnModelPath", ConfigBean.mtcnnModelPath);
        properties.put("minSizeMegdetect", ConfigBean.minSizeMegdetect);


        return properties;
    }
}
