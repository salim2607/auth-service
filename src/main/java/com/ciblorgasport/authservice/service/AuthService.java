package com.ciblorgasport.authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ciblorgasport.authservice.dto.JwtResponse;
import com.ciblorgasport.authservice.dto.LoginRequest;
import com.ciblorgasport.authservice.dto.RegisterRequest;
import com.ciblorgasport.authservice.entity.Role;
import com.ciblorgasport.authservice.entity.User;
import com.ciblorgasport.authservice.repository.UserRepository;
import com.ciblorgasport.authservice.security.JwtUtils;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    @Autowired
    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        System.out.println("[DEBUG] authenticateUser called with: username=" + loginRequest.getUsername());
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            System.out.println("[DEBUG] authenticationManager.authenticate succeeded");
        } catch (Exception e) {
            System.out.println("[ERROR] authenticationManager.authenticate failed: " + e.getMessage());
            throw e;
        }
        User user = (User) authentication.getPrincipal();
        if ((user.getRole() == Role.COMMISSAIRE || user.getRole() == Role.VOLONTAIRE) && !user.isValidated()) {
            throw new RuntimeException("Votre compte doit être validé par un administrateur.");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        return new JwtResponse(jwt, user.getUsername(), user.getEmail(), user.getRole().name());
    }
    public String registerUser(RegisterRequest registerRequest) {
        System.out.println("[DEBUG] registerUser called with: " + registerRequest.getUsername() + ", " + registerRequest.getEmail() + ", role=" + registerRequest.getRole());
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            System.out.println("[DEBUG] Username already taken");
            return "Error: Username is already taken!";
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            System.out.println("[DEBUG] Email already in use");
            return "Error: Email is already in use!";
        }
        // Création de l'utilisateur avec mot de passe hashé
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole());
        user.setValidated(user.getRole() == Role.USER || user.getRole() == Role.ATHLETE || user.getRole() == Role.ADMIN);
        System.out.println("[DEBUG] Before save: " + user.getUsername() + ", " + user.getEmail() + ", role=" + user.getRole() + ", validated=" + user.isValidated());
        try {
            userRepository.save(user);
            System.out.println("[DEBUG] User saved successfully");
        } catch (Exception e) {
            System.out.println("[ERROR] Exception during save: " + e.getMessage());
            e.printStackTrace();
            return "Error: Exception during save: " + e.getMessage();
        }
        return "User registered successfully!";
    }
}
