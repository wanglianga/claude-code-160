package com.hd.center.config;

/**
 * 当前登录用户（存于 SecurityContext 的 principal）。
 */
public record CurrentUser(Long id, String username, String name, String role) {}
