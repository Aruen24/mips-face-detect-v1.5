package com.smdt.mips.faceDetect.utils;

import com.smdt.mips.request_record.service.RequestRecordService;
import com.smdt.mips.source.service.SourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ServerUtils {
    @Autowired
    public SourceService sourceService;

    @Autowired
    public RequestRecordService requestRecordService;

    public static ServerUtils serverUtils;

    @PostConstruct
    public void init() {
        serverUtils = this;
    }

    //utils工具类中使用service和mapper接口的方法例子，用"testUtils.xxx.方法" 就可以了
//    public static void test(Item record){
//        testUtils.itemMapper.insert(record);
//        testUtils.itemService.queryAll();
//    }
}
