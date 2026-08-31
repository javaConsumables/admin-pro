package com.adminpro.aspect;

import com.adminpro.annotation.RequiresPermission;
import com.adminpro.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Before("@annotation(requiresPermission)")
    public void check(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        Long userId = request == null ? null : (Long) request.getAttribute("userId");
        permissionService.checkPermission(userId, requiresPermission.value());
    }
}
