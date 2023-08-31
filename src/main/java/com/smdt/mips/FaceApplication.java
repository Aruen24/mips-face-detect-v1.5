package com.smdt.mips;

import com.smdt.mips.faceDetect.utils.InitKit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class FaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaceApplication.class, args);
        //在启动部分初始化队列、new模型对象
        InitKit.start();
    }
}