package com.smdt.mips.source.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.util.Date;

@Data
@TableName(value = "t_source")
@Accessors(chain = true)
public class Source {
    @TableId(value = "id",type = IdType.AUTO)
    private int id;
    private int sourcetype;
    private String userid;
    private Date validtime;
    private int usertypevalue;
    private int restcount;
    private int totalcount;
    private int parallelnum;
    private Date createAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(int sourcetype) {
        this.sourcetype = sourcetype;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public Date getValidtime() {
        return validtime;
    }

    public void setValidtime(Date validtime) {
        this.validtime = validtime;
    }

    public int getRestcount() {
        return restcount;
    }

    public void setRestcount(int restcount) {
        this.restcount = restcount;
    }

    public int getTotalcount() {
        return totalcount;
    }

    public void setTotalcount(int totalcount) {
        this.totalcount = totalcount;
    }

    public int getParallelnum() {
        return parallelnum;
    }

    public void setParallelnum(int parallelnum) {
        this.parallelnum = parallelnum;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public int getUsertypevalue() {
        return usertypevalue;
    }

    public void setUsertypevalue(int usertypevalue) {
        this.usertypevalue = usertypevalue;
    }

}
