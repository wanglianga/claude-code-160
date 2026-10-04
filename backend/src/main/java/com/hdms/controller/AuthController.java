package com.hdms.controller;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.JwtService;
import com.hdms.config.SecurityUtils;
import com.hdms.model.User;
import com.hdms.repo.UserRepo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!user.isActive() || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtService.generate(user.getId(), user.getUsername(), user.getRole().name(), user.getName());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("user", user);
        return resp;
    }

    @GetMapping("/me")
    public User me() {
        return SecurityUtils.currentUser();
    }
}
