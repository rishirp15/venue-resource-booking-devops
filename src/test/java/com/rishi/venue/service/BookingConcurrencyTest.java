
package com.rishi.venue.service;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;

import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencytest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.open-in-view=false"
})
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldAllowOnlyOneConcurrentBooking()
            throws Exception {

        User user1 = new User();
        user1.setName("Concurrent User One");
        user1.setEmail("concurrent1@example.com");
        user1.setPassword("test-only-password");
        user1.setRole("USER");
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setName("Concurrent User Two");
        user2.setEmail("concurrent2@example.com");
        user2.setPassword("test-only-password");
        user2.setRole("USER");
        user2 = userRepository.save(user2);

        Venue venue = new Venue();
        venue.setName("Concurrency Test Hall");
        venue.setLocation("Test Building");
        venue.setCapacity(50);
        venue = venueRepository.save(venue);

        final Long userId1 = user1.getId();
        final Long userId2 = user2.getId();
        final Long venueId = venue.getId();

        LocalDate date = LocalDate.now().plusDays(15);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> {
            ready.countDown();
            start.await();

            try {
                bookingService.createBooking(
                        userId1, venueId, date, "MORNING"
                );
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        };

        Callable<Boolean> task2 = () -> {
            ready.countDown();
            start.await();

            try {
                bookingService.createBooking(
                        userId2, venueId, date, "MORNING"
                );
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        };

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            futures.add(executor.submit(task1));
            futures.add(executor.submit(task2));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(20, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertEquals(
                    1,
                    successCount,
                    "Exactly one booking should succeed."
            );

            List<Booking> activeBookings =
                    bookingRepository.findAll().stream()
                            .filter(b ->
                                    b.getVenue().getId().equals(venueId)
                                    && b.getBookingDate().equals(date)
                                    && b.getSlot().equals("MORNING")
                                    && ("PENDING".equals(b.getStatus())
                                        || "CONFIRMED".equals(b.getStatus())))
                            .toList();

            assertEquals(
                    1,
                    activeBookings.size(),
                    "Only one active booking should exist."
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
