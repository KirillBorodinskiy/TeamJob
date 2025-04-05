package io.datajek.spring.basics.teamjob;

import io.datajek.spring.basics.teamjob.data.Repositories.RoleRepository;
import io.datajek.spring.basics.teamjob.data.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultValueService {
    private final RoleRepository roleRepository;

    @Autowired
    public DefaultValueService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    @Transactional
    public void insert() {
        try {
            if (!roleRepository.existsByName("ROLE_USER")) {
                Role roleUser = new Role();
                roleUser.setName("ROLE_USER");
                roleRepository.save(roleUser);
                System.out.println("Role USER created");
            }else{
                System.out.println("Role USER already exists");
            }

            if (!roleRepository.existsByName("ROLE_ADMIN")) {
                Role roleAdmin = new Role();
                roleAdmin.setName("ROLE_ADMIN");
                roleRepository.save(roleAdmin);
                System.out.println("Role ADMIN created");
            }else {
                System.out.println("Role ADMIN already exists");
            }

            if (!roleRepository.existsByName("ROLE_CONFIG")) {
                Role roleConfig = new Role();
                roleConfig.setName("ROLE_CONFIG");
                roleRepository.save(roleConfig);
                System.out.println("Role CONFIG created");
            }else{
                System.out.println("Role CONFIG already exists");
            }

            // Force a flush to ensure data is committed
            roleRepository.flush();
        } catch (Exception e) {
            System.err.println("Error inserting default roles: " + e.getMessage()+e.getCause());
        }
    }
}