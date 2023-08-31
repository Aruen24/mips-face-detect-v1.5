package com.smdt.mips.source.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smdt.mips.source.entity.Source;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Mapper
public interface SourceMapper extends BaseMapper<Source> {
//    @Insert("INSERT INTO t_source(NAME, AGE) VALUES(#{name}, #{age})")
//    public int insert(@Param("name") String name, @Param("age") Integer age);

    @Delete("DELETE from t_source where id = #{id}")
    public int deleteU(@Param("id") Integer id);

    @Update("update t_source set addr = #{address} where user_id = #{userid}")
    public int updateU(@Param("userid") String id, @Param("address") String addr);

    @Select("select * from t_source where userid = #{userid}")
    public List<Source> findByUserId(@Param("userid") String userid);

    @Select("SELECT * FROM t_source WHERE sourcetype = #{sourcetype}")
    public List<Source> findBySourceType(@Param("sourcetype") int sourcetype);

    @Select("SELECT * FROM t_source WHERE userid = #{userid} and sourcetype = #{sourcetype}")
    public Source findRequestInfo(@Param("userid") String userid, @Param("sourcetype") int sourcetype);

    @Select("SELECT A.* FROM t_source A,(select min(validtime) min_day from t_source where userid = #{userid} and sourcetype = #{sourcetype} and validtime >= #{requestTime} and usertypevalue = #{usertype}) B WHERE  A.validtime = B.min_day and userid = #{userid} and sourcetype = #{sourcetype} and usertypevalue = #{usertype}")
    public List<Source> findLastSourceByUserId(@Param("userid") String userid, @Param("sourcetype") int sourcetype, @Param("requestTime") Date requestTime, @Param("usertype") int usertype);

    @Select("select A.* from t_source A,(SELECT B.min_day,max(usedcount) max_usedcount FROM t_source A,(select min(validtime) min_day from t_source where userid = #{userid} and sourcetype = #{sourcetype} and validtime >= #{requestTime} and usertypevalue = #{usertype} and usedcount < totalcount) B WHERE A.validtime = B.min_day and userid = #{userid} and sourcetype = #{sourcetype} and usertypevalue = #{usertype} and usedcount < totalcount) C WHERE  A.validtime = min_day and userid = #{userid} and sourcetype = #{sourcetype} and usertypevalue = #{usertype} and usedcount = max_usedcount")
    public Source findLastYffSourceByUserId(@Param("userid") String userid, @Param("sourcetype") int sourcetype, @Param("requestTime") Date requestTime, @Param("usertype") int usertype);

    // 增加行级锁
    @Update("update t_source A, (SELECT max(usedcount) newusedcount from t_source where userid = #{userid} and sourcetype = #{sourcetype} and validtime = #{validtime} and usertypevalue = #{usertype} and usedcount < totalcount for update) B set A.usedcount=B.newusedcount+1 where userid = #{userid} and sourcetype = #{sourcetype} and validtime = #{validtime} and usertypevalue = #{usertype} and usedcount = B.newusedcount")
    public int updateRestCount(@Param("userid") String userid, @Param("sourcetype") int sourcetype, @Param("validtime") Date validtime, @Param("usertype") int usertype);

    @Select("SELECT max(usedcount) from t_source where userid = #{userid} and sourcetype = #{sourcetype} and validtime = #{validtime} and usertypevalue = #{usertype} and usedcount < totalcount")
    public int findMaxRestCount(@Param("userid") String userid, @Param("sourcetype") int sourcetype, @Param("validtime") Date validtime, @Param("usertype") int usertype);

    @Update("update t_source A set usedcount = #{newrestcountplus} where userid = #{userid} and sourcetype = #{sourcetype} and validtime = #{validtime} and usertypevalue = #{usertype} and usedcount = #{newrestcount}")
    public int updateCount(@Param("userid") String userid, @Param("sourcetype") int sourcetype, @Param("validtime") Date validtime, @Param("usertype") int usertype, @Param("newrestcount") int newrestcount, @Param("newrestcountplus") int newrestcountplus);

}