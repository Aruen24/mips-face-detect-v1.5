package com.smdt.mips.request_record.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.util.Date;

@Data
@TableName(value = "t_request_record")
@Accessors(chain = true)
public class RequestRecord {
    @TableId(value = "id",type = IdType.AUTO)
    private int id;
    private String usercode;
    private int sourcevalue;
    private String request_id;
    private Date createAt;
    private int status;
    private Date sourcevalidtime;
    private int usertype;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsercode() {
        return usercode;
    }

    public void setUsercode(String usercode) {
        this.usercode = usercode;
    }

    public int getSourcevalue() {
        return sourcevalue;
    }

    public void setSourcevalue(int sourcevalue) {
        this.sourcevalue = sourcevalue;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getSourcevalidtime() {
        return sourcevalidtime;
    }

    public void setSourcevalidtime(Date sourcevalidtime) {
        this.sourcevalidtime = sourcevalidtime;
    }

    public int getUsertype() {
        return usertype;
    }

    public void setUsertype(int usertype) {
        this.usertype = usertype;
    }
}
