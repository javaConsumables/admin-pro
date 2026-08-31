package com.adminpro.service;

import com.adminpro.common.exception.BusinessException;
import com.adminpro.entity.SysUser;
import com.adminpro.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户查询缓存（Cache-Aside + 空值缓存防穿透）
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CACHE_KEY_PREFIX = "adminpro:cache:user:";
    private static final String EMPTY_PLACEHOLDER = "EMPTY";
    private static final Duration EMPTY_TTL = Duration.ofSeconds(60);
    private static final Duration NORMAL_TTL = Duration.ofMinutes(10);

    private final SysUserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SysUser getByIdCached(Long id) throws Exception {
        String key = CACHE_KEY_PREFIX + id;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (EMPTY_PLACEHOLDER.equals(cached)) {
                throw new BusinessException("用户不存在");
            }
            return objectMapper.readValue(cached, SysUser.class);
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            // 空值缓存：防止恶意 id 打穿到数据库
            stringRedisTemplate.opsForValue().set(key, EMPTY_PLACEHOLDER, EMPTY_TTL);
            throw new BusinessException("用户不存在");
        }
        stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(user), NORMAL_TTL);
        return user;
    }

    /** 用户数据变更后失效缓存 */
    public void evictCache(Long id) {
        if (id != null) {
            stringRedisTemplate.delete(CACHE_KEY_PREFIX + id);
        }
    }
}
