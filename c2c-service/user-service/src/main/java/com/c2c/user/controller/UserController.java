package com.c2c.user.controller;

import com.c2c.common.result.Result;
import com.c2c.user.dto.UserLoginDTO;
import com.c2c.user.entity.User;
import com.c2c.user.service.UserService;
import com.c2c.user.vo.UserLoginVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO loginDTO) {
        UserLoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody UserLoginDTO dto) {
        userService.register(dto);
        return Result.success("注册成功，快去登录吧！");
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long finalUserId = paramUserId != null ? paramUserId : headerUserId;
        if (finalUserId == null) {
            return Result.failed(400, "无法获取用户ID");
        }

        User user = userService.getById(finalUserId);
        if (user == null) {
            return Result.failed(404, "用户不存在");
        }

        // 组装安全数据，确保 bio 和 avatar 都能回显给前端
        Map<String, Object> safeUserInfo = new HashMap<>();
        safeUserInfo.put("id", user.getId());
        safeUserInfo.put("nickname", user.getNickname());
        safeUserInfo.put("avatar", user.getAvatar());
        if (finalUserId.equals(headerUserId)) {
            safeUserInfo.put("phone", user.getPhone());
        }
        safeUserInfo.put("bio", user.getBio());

        return Result.success(safeUserInfo);
    }

    @PostMapping("/update")
    public Result<String> updateUserInfo(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody User updateUser) {

        // 1. 先查出原用户信息
        User oldUser = userService.getById(userId);
        if (oldUser == null) {
            return Result.failed(404, "用户不存在");
        }

        // 2. 依次覆盖前端传过来的且不为空的字段
        if (updateUser.getNickname() != null && !updateUser.getNickname().trim().isEmpty()) {
            oldUser.setNickname(updateUser.getNickname());
        }
        if (updateUser.getBio() != null) {
            oldUser.setBio(updateUser.getBio());
        }

        // 🚨 终极修复：密码修改必须经过 BCrypt 强加密后再落库！
        if (updateUser.getPassword() != null && !updateUser.getPassword().trim().isEmpty()) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            oldUser.setPassword(encoder.encode(updateUser.getPassword()));
        }

        if (updateUser.getAvatar() != null && !updateUser.getAvatar().trim().isEmpty()) {
            oldUser.setAvatar(updateUser.getAvatar());
        }

        if (updateUser.getPhone() != null && !updateUser.getPhone().trim().isEmpty()) {
            oldUser.setPhone(updateUser.getPhone());
        }

        // 3. 执行更新
        boolean success = userService.updateById(oldUser);

        if (success) {
            return Result.success("资料更新成功");
        } else {
            return Result.failed(500, "资料更新失败");
        }
    }
}
