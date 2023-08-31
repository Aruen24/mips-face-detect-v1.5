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
public class QueueListener {

    @Autowired
    private HttpTask httpTask;

    @Autowired
    private RequestQueue queue;

    @Value("${model.pic_thread}")
    private int picThread;

    /**
     * 初始化时启动监听请求队列
     */
    @PostConstruct
    public void init() throws InterruptedException {
        ExecutorService threadPoolExecutors = Executors.newFixedThreadPool(picThread);

        for(int i = 0; i < picThread; i++){
            threadPoolExecutors.submit(new HttpTask(queue));
        }

    }

    /**
     * 销毁容器时停止监听任务
     */
    @PreDestroy
    public void destory() {
        httpTask.setRunning(false);
    }

}

