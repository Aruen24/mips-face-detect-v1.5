package com.smdt.mips.request_record.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smdt.mips.request_record.entity.RequestRecord;

import java.util.Date;

public interface RequestRecordService extends IService<RequestRecord> {

    public int insertRequestRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip);

    public int insertInnerUserRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip);

    public int insertOuterUserRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip);
}
