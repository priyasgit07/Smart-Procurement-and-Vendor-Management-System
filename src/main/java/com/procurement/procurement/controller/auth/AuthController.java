package com.procurement.procurement.controller.auth;

import com.procurement.procurement.dto.auth.AuthResponseDTO;
import com.procurement.procurement.dto.auth.LoginRequestDTO;
import com.procurement.procurement.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}
