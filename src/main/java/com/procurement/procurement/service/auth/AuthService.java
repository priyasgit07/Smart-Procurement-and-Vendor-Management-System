package com.procurement.procurement.service.auth;

import com.procurement.procurement.dto.auth.AuthResponseDTO;
import com.procurement.procurement.dto.auth.LoginRequestDTO;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.security.JwtUtil;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        userRepository.findByUsername(request.getUsername()).ifPresent(user ->
                auditService.log("User", user.getId(), "LOGIN", "User logged in: " + user.getUsername())
        );

        return new AuthResponseDTO(token);
    }

    @Transactional
    public String register(LoginRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return "Username already exists";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getUsername() + "@procurement.com");

        User saved = userRepository.save(user);

        auditService.log("User", saved.getId(), "REGISTER", "User registered: " + saved.getUsername());

        return "User registered successfully";
    }
}
