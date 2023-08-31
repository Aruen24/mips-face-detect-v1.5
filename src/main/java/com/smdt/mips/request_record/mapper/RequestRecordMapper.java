package com.smdt.mips.request_record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smdt.mips.request_record.entity.RequestRecord;
import org.apache.ibatis.annotations.*;

import java.util.Date;

@Mapper
public interface RequestRecordMapper extends BaseMapper<RequestRecord> {

    @Update("INSERT INTO t_request_record(usercode, sourcevalue, requestid, createAt, status, sourcevalidtime, usertype, accesskeyid, sourceip) VALUES(#{userid}, #{sourcevalue}, #{request_id}, #{request_time}, #{status}, #{sourcevalidtime}, #{usertype}, #{accessKey_id}, #{source_ip})")
    public int insertRequestRecord(@Param("userid") String userid, @Param("sourcevalue") int sourcevalue, @Param("request_id") String request_id, @Param("request_time") Date request_time, @Param("status") int status, @Param("sourcevalidtime") Date sourcevalidtime, @Param("usertype") int usertype, @Param("accessKey_id") String accessKey_id, @Param("source_ip") String source_ip);

    @Update("INSERT INTO t_inner_user_record(usercode, sourcevalue, requestid, createAt, status, sourcevalidtime, usertype, accesskeyid, sourceip) VALUES(#{userid}, #{sourcevalue}, #{request_id}, #{request_time}, #{status}, #{sourcevalidtime}, #{usertype}, #{accessKey_id}, #{source_ip})")
    public int insertInnerUserRecord(@Param("userid") String userid, @Param("sourcevalue") int sourcevalue, @Param("request_id") String request_id, @Param("request_time") Date request_time, @Param("status") int status, @Param("sourcevalidtime") Date sourcevalidtime, @Param("usertype") int usertype, @Param("accessKey_id") String accessKey_id, @Param("source_ip") String source_ip);

    @Update("INSERT INTO t_outer_user_record(usercode, sourcevalue, requestid, createAt, status, sourcevalidtime, usertype, accesskeyid, sourceip) VALUES(#{userid}, #{sourcevalue}, #{request_id}, #{request_time}, #{status}, #{sourcevalidtime}, #{usertype}, #{accessKey_id}, #{source_ip})")
    public int insertOuterUserRecord(@Param("userid") String userid, @Param("sourcevalue") int sourcevalue, @Param("request_id") String request_id, @Param("request_time") Date request_time, @Param("status") int status, @Param("sourcevalidtime") Date sourcevalidtime, @Param("usertype") int usertype, @Param("accessKey_id") String accessKey_id, @Param("source_ip") String source_ip);
}