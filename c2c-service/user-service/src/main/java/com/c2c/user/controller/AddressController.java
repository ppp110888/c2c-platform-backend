package com.c2c.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.c2c.common.result.Result;
import com.c2c.user.entity.UserAddress;
import com.c2c.user.service.UserAddressService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/address")
public class AddressController {

    @Resource
    private UserAddressService addressService;

    // 1. 获取我的地址列表 (默认地址排在最前面)
    @GetMapping("/list")
    public Result<List<UserAddress>> getMyAddress(@RequestHeader("X-User-Id") Long userId) {
        List<UserAddress> list = addressService.list(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault) // 默认地址排第一
                .orderByDesc(UserAddress::getCreateTime));
        return Result.success(list);
    }

    // 2. 获取单个地址详情 (用于编辑回显)
    @GetMapping("/detail/{id}")
    public Result<UserAddress> getDetail(@PathVariable("id") Long id,
                                         @RequestHeader("X-User-Id") Long userId) {
        UserAddress address = addressService.getOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, id).eq(UserAddress::getUserId, userId));
        return address == null ? Result.failed(404, "地址不存在") : Result.success(address);
    }

    // 3. 新增或修改地址
    @PostMapping("/save")
    public Result<String> saveAddress(@RequestHeader("X-User-Id") Long userId, @RequestBody UserAddress address) {
        if (address.getId() != null && addressService.count(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, address.getId()).eq(UserAddress::getUserId, userId)) == 0) {
            return Result.failed(403, "无权修改该地址");
        }
        address.setUserId(userId);

        // 如果用户把这个地址设为默认，需要先把其他地址的默认状态清空
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressService.update(new LambdaUpdateWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .set(UserAddress::getIsDefault, 0));
        }

        addressService.saveOrUpdate(address);
        return Result.success("保存成功");
    }

    // 4. 删除地址
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteAddress(@PathVariable("id") Long id,
                                        @RequestHeader("X-User-Id") Long userId) {
        boolean removed = addressService.remove(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, id).eq(UserAddress::getUserId, userId));
        return removed ? Result.success("删除成功") : Result.failed(404, "地址不存在");
    }
}
