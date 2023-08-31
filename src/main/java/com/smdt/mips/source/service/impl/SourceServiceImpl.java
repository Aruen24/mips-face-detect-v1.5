package com.smdt.mips.source.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smdt.mips.source.entity.Source;
import com.smdt.mips.source.mapper.SourceMapper;
import com.smdt.mips.source.service.SourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class SourceServiceImpl extends ServiceImpl<SourceMapper, Source> implements SourceService {
//    public int u(String id, String addr) {
//        return baseMapper.updateU(id, addr);
//    }

    public List<Source> findBySourceType(int sourcetype) {
        return baseMapper.findBySourceType(sourcetype);
    }

    public List<Source> findByUserId(String userid) {
        return baseMapper.findByUserId(userid);
    }

    public Source findRequestInfo(String userid, int sourcetype) {
        return baseMapper.findRequestInfo(userid, sourcetype);
    }

    public List<Source> findLastSourceByUserId(String userid, int sourcetype, Date requestTime, int usertype) {
        return baseMapper.findLastSourceByUserId(userid, sourcetype, requestTime, usertype);
    }

    public Source findLastYffSourceByUserId(String userid, int sourcetype, Date requestTime, int usertype) {
        return baseMapper.findLastYffSourceByUserId(userid, sourcetype, requestTime, usertype);
    }

//    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.DEFAULT,timeout=36000,rollbackFor=Exception.class)
    //将事务隔离级别降低到Read Committed,将事务传播行为改为REQUIRES_NEW
    @Transactional(propagation = Propagation.REQUIRES_NEW,isolation = Isolation.READ_COMMITTED,timeout=36000,rollbackFor=Exception.class)
    public int updateRestCount(String userid, int sourcetype, Date validtime, int usertype) {
        return baseMapper.updateRestCount(userid, sourcetype, validtime, usertype);
    }

//    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.DEFAULT,timeout=36000,rollbackFor=Exception.class)
    //将事务隔离级别降低到Read Committed,将事务传播行为改为REQUIRES_NEW
    @Transactional(propagation = Propagation.REQUIRES_NEW,isolation = Isolation.READ_COMMITTED,timeout=36000,rollbackFor=Exception.class)
    public int updateTransactionalCount(String userid, int sourcetype, Date validtime, int usertype) {
        int newrestcount = baseMapper.findMaxRestCount(userid, sourcetype, validtime, usertype);
        int newrestcountplus = newrestcount + 1;
        return baseMapper.updateCount(userid, sourcetype, validtime, usertype, newrestcount, newrestcountplus);
    }
}
