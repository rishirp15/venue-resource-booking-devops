package com.rishi.venue.config;

import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            VenueRepository venueRepository,
            UserRepository userRepository,
            PasswordService passwordService) {

        return args -> {

            if (venueRepository.count() == 0) {

                venueRepository.save(
                    new Venue("Seminar Hall", "Main Building", 120)
                );

                venueRepository.save(
                    new Venue("Conference Room", "Admin Building", 30)
                );

                venueRepository.save(
                    new Venue("Computer Laboratory", "Technology Building", 60)
                );

                System.out.println("Sample venues initialized.");
            }

            // Seed initial administrator if none exists
            if (!userRepository.existsByEmail("admin@venue.com")) {
                User admin = new User(
                        "System Admin",
                        "admin@venue.com",
                        passwordService.hashPassword("admin123"),
                        "ADMIN"
                );
                userRepository.save(admin);
                System.out.println("Default admin user initialized (admin@venue.com).");
            }

            // Seed initial demo user if none exists
            if (!userRepository.existsByEmail("user@venue.com")) {
                User user = new User(
                        "Demo User",
                        "user@venue.com",
                        passwordService.hashPassword("user123"),
                        "USER"
                );
                userRepository.save(user);
                System.out.println("Default demo user initialized (user@venue.com).");
            }
        };
    }
}
