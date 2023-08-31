package com.smdt.mips.source.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smdt.mips.source.entity.Source;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface SourceService extends IService<Source> {
//    public int u(String id, String addr);

    public List<Source> findBySourceType(int sourcetype);

    /**
     * 根据用户id查找资源信息
     * @param userid
     * @return
     */
    public List<Source> findByUserId(String userid);

    /**
     * 根据用户id和资源包种类查找资源信息
     * @param userid
     * @param sourcetype
     * @return
     */
    public Source findRequestInfo(String userid, int sourcetype);

    /**
     * 根据用户id和资源包种类查找在有效期内,失效日期最近的资源信息
     * @param userid
     * @param sourcetype
     * @return
     */
    public List<Source> findLastSourceByUserId(String userid, int sourcetype, Date requestTime, int usertype);

    /**
     * 根据用户id和预付费资源查找在有效期内,失效日期最近的且可用次数大于0的资源信息,当有多个失效日期，选剩余次数较少的资源
     * @param userid
     * @param sourcetype
     * @return
     */
    public Source findLastYffSourceByUserId(String userid, int sourcetype, Date requestTime, int usertype);


    /**
     * 更新用户资源包的使用剩余次数，查找更新restcount值在一个sql中完成
     * @param userid
     * @param sourcetype
     * @return
     */
    public int updateRestCount(String userid, int sourcetype, Date validtime, int usertype);


    public int updateTransactionalCount(String userid, int sourcetype, Date validtime, int usertype);
}
