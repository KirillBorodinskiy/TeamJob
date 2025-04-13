package io.datajek.spring.basics.teamjob.controllers;

import io.datajek.spring.basics.teamjob.DefaultValueService;
import io.datajek.spring.basics.teamjob.JwtCore;
import io.datajek.spring.basics.teamjob.data.Repositories.RoleRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.Role;
import io.datajek.spring.basics.teamjob.data.SigninRequest;
import io.datajek.spring.basics.teamjob.data.SignupRequest;
import io.datajek.spring.basics.teamjob.data.User;
import io.jsonwebtoken.ExpiredJwtException;
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
    private RoleRepository roleRepository;
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
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Autowired
    public void setJwtCore(JwtCore jwtCore) {
        this.jwtCore = jwtCore;
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<String> handleExpiredJwtException(ExpiredJwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token expired" + e.getLocalizedMessage());
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
        response.addCookie(getCookie(jwt));

        return ResponseEntity.ok().build();
    }

    private static Cookie getCookie(String jwt) {
        Cookie cookie = new Cookie("jwt", jwt);
        cookie.setHttpOnly(true);     // Makes cookie inaccessible to JavaScript
        cookie.setSecure(true);       // Only sent over HTTPS
        cookie.setPath("/");          // Cookie is valid for all paths
        cookie.setMaxAge(86400);      // Cookie expires in 1 day

        return cookie;
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
        Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.addRole(userRole);
        userRepository.save(user);
        return ResponseEntity.ok("User " + signupRequest.getUsername() + " created");
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOut(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);  // for HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0);  // Expires immediately to delete the cookie!
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }
}
