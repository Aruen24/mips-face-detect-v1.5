package com.smdt.mips.faceDetect.asyncrequest;

import org.springframework.web.context.request.async.DeferredResult;

/**
 * 存储异步处理信息
 *
 * @author Aruen
 *
 * @param <I> 接口输入body参数
 * @param <H> 接口输入header参数
 * @param <O> 接口返回参数
 */
public class AsyncVo<I, H, N, O> {

    /**
     * 请求body参数
     */
    private I params;

    /**
     * 请求头参数
     */
    private H headers;

    /**
     * 请求头参数
     */
    private N requestid;

    /**
     * 响应结果
     */
    private DeferredResult<O> result;

    public I getParams() {
        return params;
    }

    public void setParams(I params) {
        this.params = params;
    }

    public N getRequestid() {
        return requestid;
    }

    public void setRequestid(N requestid) {
        this.requestid = requestid;
    }

    public H getHeaders() {
        return headers;
    }

    public void setHeaders(H headers) {
        this.headers = headers;
    }

    public DeferredResult<O> getResult() {
        return result;
    }

    public void setResult(DeferredResult<O> result) {
        this.result = result;
    }

}


