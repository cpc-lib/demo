package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user")
public class User {

    @TableId
    private Long id;

    private String username;

    private String mobile;

    private Date createTime;
}
