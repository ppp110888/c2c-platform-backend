package com.c2c.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.ResultCode;
import com.c2c.common.utils.JwtUtils;
import com.c2c.user.dto.UserLoginDTO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.user.service.UserService;
import com.c2c.user.vo.UserLoginVO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        // 1. 安全校验：利用 Lambda 规避硬编码字符串字段名，查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));

        // 2. 业务断言：用户是否存在
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new RuntimeException("账号已停用");
        }

        // 3. 安全比对：利用 BCrypt 强哈希算法比对明文密码与数据库密文
        boolean checkpw = BCrypt.checkpw(loginDTO.getPassword(), user.getPassword());
        if (!checkpw) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 4. 签发核心凭证 JWT Token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 组装企业级 VO 返回结果
        return UserLoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar()) // 顺手把你实体类里的头像返回出去
                .build();
    }

    @Override
    public void register(UserLoginDTO dto) {
        if (dto.getUsername() == null || !dto.getUsername().matches("^[\\p{L}\\p{N}_]{3,30}$")
                || dto.getPassword() == null || dto.getPassword().length() < 8 || dto.getPassword().length() > 72) {
            throw new IllegalArgumentException("用户名需为3到30位字符，密码需为8到72位");
        }
        // 1. 检查用户名是否被抢占 (统一改用 LambdaQueryWrapper，规避魔法值)
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername());
        if (this.count(query) > 0) {
            throw new RuntimeException("哎呀，用户名已被注册啦");
        }

        // 2. 创建新用户并附赠初始“门面”
        User user = new User();
        user.setUsername(dto.getUsername());

        // ==========================================================
        // 🚨 核心魔术：给密码加密！
        // 既然你的 login 用的是 Hutool 的 BCrypt.checkpw，
        // 这里落库时就必须用 BCrypt.hashpw 进行加密生成密文！
        // ==========================================================
        String encryptPwd = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());

        user.setPassword(encryptPwd);
        user.setNickname("新晋买家_" + dto.getUsername());
        // 默认头像
        user.setAvatar("http://127.0.0.1:9000/c2c-items/default-avatar.jpg");
        user.setStatus(1);

        // 3. 落库
        this.save(user);
    }
}
