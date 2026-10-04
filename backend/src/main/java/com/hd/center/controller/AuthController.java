package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.config.JwtService;
import com.hd.center.entity.User;
import com.hd.center.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        User user = userRepo.findByUsername(request.username())
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtService.generate(user));
        result.put("user", toUserInfo(user));
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CurrentUser current) {
        User user = userRepo.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return toUserInfo(user);
    }

    private Map<String, Object> toUserInfo(User user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("name", user.getName());
        info.put("role", user.getRole().name());
        return info;
    }
}
