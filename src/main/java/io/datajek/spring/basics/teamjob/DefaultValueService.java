package io.datajek.spring.basics.teamjob;

import io.datajek.spring.basics.teamjob.data.Repositories.RoleRepository;
import io.datajek.spring.basics.teamjob.data.Repositories.UserRepository;
import io.datajek.spring.basics.teamjob.data.Role;
import io.datajek.spring.basics.teamjob.data.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

@Service
public class DefaultValueService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_CONFIG = "ROLE_CONFIG";

    @Autowired
    public DefaultValueService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void insert() {
        try {
            if (!roleRepository.existsByName(DefaultValueService.ROLE_CONFIG)) {
                Role roleUser = new Role();
                roleUser.setName(DefaultValueService.ROLE_CONFIG);
                roleRepository.save(roleUser);
                System.out.println("Role " + DefaultValueService.ROLE_CONFIG + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_CONFIG + " already exists");
            }

            if (!roleRepository.existsByName(DefaultValueService.ROLE_ADMIN)) {
                Role roleAdmin = new Role();
                roleAdmin.setName(DefaultValueService.ROLE_ADMIN);
                roleRepository.save(roleAdmin);
                System.out.println("Role " + DefaultValueService.ROLE_ADMIN + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_ADMIN + " already exists");
            }

            if (!roleRepository.existsByName(DefaultValueService.ROLE_USER)) {
                Role roleConfig = new Role();
                roleConfig.setName(DefaultValueService.ROLE_USER);
                roleRepository.save(roleConfig);
                System.out.println("Role " + DefaultValueService.ROLE_USER + " created");
            } else {
                System.out.println("Role " + DefaultValueService.ROLE_USER + " already exists");
            }

            // Force a flush to ensure data is committed
            roleRepository.flush();


            User adminUser = new User();
            adminUser.setUsername("adminadmin");
            adminUser.setPassword(passwordEncoder.encode("adminadmin"));
            adminUser.setEmail("adminadmin@gmail.com");
            adminUser.setRoles(new HashSet<>(roleRepository.findAll()));

            createUser(adminUser);

            User user = new User();
            user.setUsername("useruser");
            user.setPassword(passwordEncoder.encode("useruser"));
            user.setEmail("useruser@user.com");
            Role userRole = roleRepository.findByName(DefaultValueService.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRoles(new HashSet<>(Collections.singleton(userRole)));
            createUser(user);

            // Force a flush to ensure data is committed
            userRepository.flush();
        } catch (Exception e) {
            System.err.println("Error inserting default roles: " + e.getMessage() + e.getCause());
        }
    }

    private void createUser(User user) {
        if (!userRepository.existsByUsername(user.getUsername())) {
            user.setRoles(new HashSet<>(roleRepository.findAll()));
            userRepository.save(user);
            System.out.println("User " + user.getUsername() + " created");
        } else {
            System.out.println("User " + user.getUsername() + " already exists");
        }
    }
}