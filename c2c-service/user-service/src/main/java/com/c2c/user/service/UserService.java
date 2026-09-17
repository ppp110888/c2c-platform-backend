package com.c2c.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.c2c.user.dto.UserLoginDTO;
import com.c2c.user.entity.User;
import com.c2c.user.vo.UserLoginVO;

public interface UserService extends IService<User> {
    /**
     * 用户登录接口
     * @param loginDTO 登录参数
     * @return 包含Token的VO对象
     */
    UserLoginVO login(UserLoginDTO loginDTO);
    void register(UserLoginDTO registerDTO);

}