package com.smdt.mips.faceDetect.asyncrequest;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.smdt.mips.faceDetect.utils.Response_result;
import org.springframework.stereotype.Component;


/**
 * 存放所有异步处理接口请求队列的对象,一个接口对应一个队列
 *
 * @author Aruen
 *
 */
@Component
public class RequestQueue {

    /**
     * 处理http接口的队列，设置缓冲容量为100
     */
    private BlockingQueue<AsyncVo<String, Map<String, String>,String, Response_result>> httpQueue = new LinkedBlockingQueue<>(10000);

    public BlockingQueue<AsyncVo<String,Map<String, String>,String, Response_result>> getHttpQueue() {
        return httpQueue;
    }

}

