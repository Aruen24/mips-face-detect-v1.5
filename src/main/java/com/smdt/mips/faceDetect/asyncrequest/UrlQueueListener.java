package com.smdt.mips.faceDetect.asyncrequest;

//import com.smdt.mips.constv.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 队列监听器，初始化启动所有监听任务
 *
 * @author Aruen
 *
 */
@Component
public class UrlQueueListener {

    @Autowired
    private HttpUrlTask httpUrlTask;

    @Autowired
    private RequestUrlQueue url_queue;

    @Value("${model.url_thread}")
    private int urlThread;

    /**
     * 初始化时启动监听请求队列
     */
    @PostConstruct
    public void init() throws InterruptedException {
        ExecutorService threadPoolExecutors = Executors.newFixedThreadPool(urlThread);

        for(int i = 0; i < urlThread; i++){
            threadPoolExecutors.submit(new HttpUrlTask(url_queue));
        }

    }

    /**
     * 销毁容器时停止监听任务
     */
    @PreDestroy
    public void destory() {
        httpUrlTask.setRunning(false);
    }

}

