package com.c2c.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 只要继承 BaseMapper，MyBatis-Plus 就自动帮你写好了 CRUD 语句！
public interface UserMapper extends BaseMapper<User> {
}