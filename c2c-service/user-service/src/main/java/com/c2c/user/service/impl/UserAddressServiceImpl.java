package com.c2c.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.c2c.user.entity.UserAddress;
import com.c2c.user.mapper.UserAddressMapper;
import com.c2c.user.service.UserAddressService;
import org.springframework.stereotype.Service;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
}