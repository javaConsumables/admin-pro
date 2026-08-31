package com.adminpro.service;

import com.adminpro.common.exception.BusinessException;
import com.adminpro.dto.LoginRequest;
import com.adminpro.dto.LoginResponse;
import com.adminpro.entity.SysUser;
import com.adminpro.mapper.SysUserMapper;
import com.adminpro.util.JwtUtil;
import com.adminpro.util.PasswordUtil;
import com.adminpro.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_KEY_PREFIX = "adminpro:token:";

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !PasswordUtil.verify(request.getPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (Objects.equals(user.getStatus(), 0)) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        // 登录态写入 Redis：同一用户新登录会顶掉旧 token（支持登出/踢人）
        stringRedisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + user.getId(), token,
                Duration.ofSeconds(jwtUtil.getExpireSeconds()));
        return new LoginResponse(token, jwtUtil.getExpireSeconds(), UserVO.from(user));
    }

    public void logout(Long userId) {
        stringRedisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }

    public UserVO me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.from(user);
    }
}
