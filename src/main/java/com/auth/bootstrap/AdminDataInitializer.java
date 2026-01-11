package com.auth.bootstrap;

import com.auth.model.Role;
import com.auth.model.User;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * This class adds sample admin user at start.
 */
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * This method runs at app start.
     *
     * @param args the start args
     */
    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
    }
}