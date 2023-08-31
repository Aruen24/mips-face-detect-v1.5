package com.smdt.mips.faceDetect.utils;

import lombok.Getter;

/**
 * 返回结果实体类
 *
 * @author Aruen
 * @update 2020/2/13
 * @since 2020/2/13
 */
@Getter
public class Response_result {
    private int errno;
    private String err_msg;
    private Object data;
    private String request_id;

    public int getErrno() {
        return errno;
    }

    private Response_result setResult(int errno, String err_msg, String request_id, Object data) {
        this.errno = errno;
        this.err_msg = err_msg;
        this.request_id = request_id;
        this.data = data;
        return this;
    }

    public Response_result success(String request_id) {
        return setResult(0, "success", request_id, null);
    }

    public Response_result success(String request_id, Object data) {
        return setResult(0, "success", request_id, data);
    }

    public Response_result fail(Object data, String err_msg, String request_id) {
        return setResult(1, err_msg, request_id, data);
    }

    public Response_result fail(Object data, String err_msg, int errno, String request_id) {
        return setResult(errno, err_msg, request_id, data);
    }

    public Response_result fail(String err_msg, int errno, String request_id) {
        return setResult(errno, err_msg, request_id, null);
    }
}