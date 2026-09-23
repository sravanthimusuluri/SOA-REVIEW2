package com.bibliotech.auth.config;

import com.bibliotech.auth.entity.Role;
import com.bibliotech.auth.entity.User;
import com.bibliotech.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            logger.info("Seeding initial users into Auth database...");

            // 1. System Administrator
            userRepository.save(new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Platform Administrator",
                    "admin@bibliotech.com",
                    null,
                    Role.ADMIN
            ));

            // 2. Chief Librarian
            userRepository.save(new User(
                    "librarian",
                    passwordEncoder.encode("lib123"),
                    "Chief Librarian",
                    "librarian@bibliotech.com",
                    null,
                    Role.LIBRARIAN
            ));

            // 3. Team Lead: Musuluri Sravanthi
            userRepository.save(new User(
                    "2400030661",
                    passwordEncoder.encode("pass123"),
                    "Musuluri Sravanthi",
                    "2400030661@kluniversity.in",
                    "2400030661",
                    Role.STUDENT
            ));

            // 4. Team Member: Malisetty Naga Sai Nikitha
            userRepository.save(new User(
                    "2400033191",
                    passwordEncoder.encode("pass123"),
                    "Malisetty Naga Sai Nikitha",
                    "2400033191@kluniversity.in",
                    "2400033191",
                    Role.STUDENT
            ));

            // 5. Team Member: Karri Venkata Lakshmi Khyati
            userRepository.save(new User(
                    "2400033157",
                    passwordEncoder.encode("pass123"),
                    "Karri Venkata Lakshmi Khyati",
                    "2400033157@kluniversity.in",
                    "2400033157",
                    Role.STUDENT
            ));

            // 6. Generic Student for quick testing
            userRepository.save(new User(
                    "student",
                    passwordEncoder.encode("student123"),
                    "Demo Student",
                    "student@bibliotech.edu",
                    "STD-DEMO-01",
                    Role.STUDENT
            ));

            logger.info("Successfully seeded 6 default users across ADMIN, LIBRARIAN, and STUDENT roles.");
        }
    }
}
