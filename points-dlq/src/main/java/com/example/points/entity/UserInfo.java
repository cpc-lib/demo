package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class UserInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String mobile;
}
