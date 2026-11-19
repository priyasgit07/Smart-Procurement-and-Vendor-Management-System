package com.procurement.procurement.service.initializer;

import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-users:false}")
    private boolean seedDemoUsers;

    @Value("${app.demo-admin-password:}")
    private String adminPassword;

    @Value("${app.demo-manager-password:}")
    private String managerPassword;

    @Value("${app.demo-employee-password:}")
    private String employeePassword;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ================= ROLES =================
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_ADMIN", "System Administrator")
                ));

        Role managerRole = roleRepository.findByName("ROLE_PROCUREMENT_MANAGER")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_PROCUREMENT_MANAGER", "Procurement Manager")
                ));

        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_EMPLOYEE", "Normal Employee")
                ));

        logger.info("Application roles verified/initialized: ROLE_ADMIN, ROLE_PROCUREMENT_MANAGER, ROLE_EMPLOYEE");

        // ================= DEMO USERS (Configurable via Environment) =================
        if (!seedDemoUsers) {
            logger.info("Demo user seeding is disabled (app.seed-demo-users=false).");
            return;
        }

        logger.info("Demo user seeding is enabled. Initializing configured demo accounts...");

        if (userRepository.findByUsername("admin").isEmpty()) {
            if (adminPassword != null && !adminPassword.trim().isEmpty()) {
                createUser("admin", adminPassword.trim(), "admin@procurement.com", adminRole);
                logger.info("Demo account 'admin' created.");
            } else {
                logger.warn("Skipping demo 'admin' creation: DEMO_ADMIN_PASSWORD is not configured.");
            }
        }

        if (userRepository.findByUsername("manager").isEmpty()) {
            if (managerPassword != null && !managerPassword.trim().isEmpty()) {
                createUser("manager", managerPassword.trim(), "manager@procurement.com", managerRole);
                logger.info("Demo account 'manager' created.");
            } else {
                logger.warn("Skipping demo 'manager' creation: DEMO_MANAGER_PASSWORD is not configured.");
            }
        }

        if (userRepository.findByUsername("employee").isEmpty()) {
            if (employeePassword != null && !employeePassword.trim().isEmpty()) {
                createUser("employee", employeePassword.trim(), "employee@procurement.com", employeeRole);
                logger.info("Demo account 'employee' created.");
            } else {
                logger.warn("Skipping demo 'employee' creation: DEMO_EMPLOYEE_PASSWORD is not configured.");
            }
        }
    }

    private void createUser(String username, String password, String email, Role role) {

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(username.toUpperCase());

        user.setEnabled(true);
        user.setActive(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);
    }
}