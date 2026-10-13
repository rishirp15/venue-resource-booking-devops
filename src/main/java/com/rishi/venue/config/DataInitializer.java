
package com.rishi.venue.config;

import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeVenues(VenueRepository venueRepository) {
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
        };
    }
}
