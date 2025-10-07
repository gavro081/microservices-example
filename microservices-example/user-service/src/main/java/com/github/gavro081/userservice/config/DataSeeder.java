package com.github.gavro081.userservice.config;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.gavro081.userservice.models.User;
import com.github.gavro081.userservice.repositories.UserRepository;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;

    @Bean
    CommandLineRunner seedUserData() {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("Seeding user data...");

                List<User> users = List.of(
                        User.builder().username("filipgav").balance(100000.00).build(),
                        User.builder().username("gavro").balance(2300.00).build(),
                        User.builder().username("aleksandar").balance(100.00).build(),
                        User.builder().username("johndoe").balance(3000.00).build()
                );

                userRepository.saveAll(users);
                log.info("Seeded {} users successfully", users.size());
            } else {
                log.info("User data already exists, skipping seed");
            }
        };
    }
}
