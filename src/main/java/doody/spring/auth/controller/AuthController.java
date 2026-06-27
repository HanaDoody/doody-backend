package doody.spring.auth.controller;

import doody.spring.auth.dto.LoginRequest;
import doody.spring.auth.dto.LoginResponse;
import doody.spring.auth.dto.SignupRequest;
import doody.spring.auth.dto.SignupResponse;
import doody.spring.auth.service.AuthService;
import doody.spring.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@RequestBody SignupRequest request) {
        return ApiResponse.success(HttpStatus.CREATED, "signup success.", authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(HttpStatus.OK, "login success.", authService.login(request));
    }
}