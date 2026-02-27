package org.security.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.security.user.dto.RegisterRequest;
import org.security.user.dto.UserResponse;
import org.security.user.dto.ValidateRequest;
import org.security.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCredentials(@RequestBody ValidateRequest request) {
        return ResponseEntity.ok(userService.validateCredentials(request));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserRoles(id));
    }
}
