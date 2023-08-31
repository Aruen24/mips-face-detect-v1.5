package com.smdt.mips.request_record.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smdt.mips.request_record.entity.RequestRecord;
import com.smdt.mips.request_record.mapper.RequestRecordMapper;
import com.smdt.mips.request_record.service.RequestRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RequestRecordServiceImpl extends ServiceImpl<RequestRecordMapper, RequestRecord> implements RequestRecordService {
//    public int u(String id, String addr) {
//        return baseMapper.updateU(id, addr);
//    }

    public int insertRequestRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip){
        return baseMapper.insertRequestRecord(userid, sourcevalue, request_id, request_time, status, sourcevalidtime, usertype, accessKey_id, source_ip);
    }

    public int insertInnerUserRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip){
        return baseMapper.insertInnerUserRecord(userid, sourcevalue, request_id, request_time, status, sourcevalidtime, usertype, accessKey_id, source_ip);
    }

    public int insertOuterUserRecord(String userid, int sourcevalue, String request_id, Date request_time, int status, Date sourcevalidtime, int usertype, String accessKey_id, String source_ip){
        return baseMapper.insertOuterUserRecord(userid, sourcevalue, request_id, request_time, status, sourcevalidtime, usertype, accessKey_id, source_ip);
    }
}
