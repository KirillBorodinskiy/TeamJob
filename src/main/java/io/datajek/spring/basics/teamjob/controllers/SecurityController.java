package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.JwtCore;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.SigninRequest;
import io.datajek.spring.basics.teamjob.data.SignupRequest;
import io.datajek.spring.basics.teamjob.data.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class SecurityController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtCore jwtCore;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setJwtCore(JwtCore jwtCore) {
        this.jwtCore = jwtCore;
    }

    @PostMapping("/signin")
    ResponseEntity<?> signin(@RequestBody SigninRequest signinRequest,
                             HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signinRequest.getUsername(), signinRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtCore.generateToken(authentication);

        // Create secure cookie with JWT token
        String cookieHeader = getCookieHeader(jwt);

        response.setHeader("Set-Cookie", cookieHeader);

        return ResponseEntity.ok().build();
    }

    private static String getCookieHeader(String jwt) {
        Cookie cookie = new Cookie("jwt", jwt);
        cookie.setHttpOnly(true);     // Makes cookie inaccessible to JavaScript
        cookie.setSecure(true);       // Only sent over HTTPS
        cookie.setPath("/");          // Cookie is valid for all paths
        cookie.setMaxAge(86400);      // Cookie expires in 1 day

        return String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; Secure; SameSite=Strict",
                cookie.getName(),
                cookie.getValue(),
                cookie.getMaxAge(),
                cookie.getPath());
    }

    @PostMapping("/signup")
    ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        if (userRepository.findByUsername(signupRequest.getUsername()).isPresent() || userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>("User with that email or name already exists", HttpStatus.CONFLICT);
        }
        User user = new User();
        String hashedPassword = passwordEncoder.encode(signupRequest.getPassword());
        user.setPassword(hashedPassword);
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        userRepository.save(user);
        return ResponseEntity.ok("User " + signupRequest.getUsername() + " created");
    }

    @GetMapping("/signout")
    ResponseEntity<?> signout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("User signed out");
    }
}
