package com.rishi.venue.migration;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.BookingApprovalHistory;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingApprovalHistoryRepository;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;
import com.rishi.venue.service.PasswordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflowtest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Transactional
public class DatabaseOperationsWorkflowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingApprovalHistoryRepository historyRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PasswordService passwordService;

    @Test
    @DisplayName("Verify User Registration, Password Hashing and Authentication")
    void testUserRegistrationAndAuth() {
        String rawPassword = "SecurePassword@2026";
        String encodedPassword = passwordService.hashPassword(rawPassword);

        User user = new User("Alice Migration", "alice.mig@example.com", encodedPassword, "USER");
        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertTrue(passwordService.matches(rawPassword, savedUser.getPassword()));
        assertEquals("USER", savedUser.getRole());

        // Verify lookup by email
        User fetched = userRepository.findByEmail("alice.mig@example.com").orElse(null);
        assertNotNull(fetched);
        assertEquals("Alice Migration", fetched.getName());
    }

    @Test
    @DisplayName("Verify Venue Creation with capacity and active status")
    void testVenueCreation() {
        Venue venue = new Venue("Auditorium Delta", "South Wing", 250, true, "AUDITORIUM", "Dolby Audio, Dual Projector");
        Venue savedVenue = venueRepository.save(venue);

        assertNotNull(savedVenue.getId());
        assertEquals(250, savedVenue.getCapacity());
        assertTrue(savedVenue.isActive());
        assertEquals("AUDITORIUM", savedVenue.getVenueType());

        Venue found = venueRepository.findByName("Auditorium Delta").orElse(null);
        assertNotNull(found);
        assertEquals("South Wing", found.getLocation());
    }

    @Test
    @DisplayName("Verify Booking Creation, Approval Workflow, and Audit History")
    void testBookingCreationAndApproval() {
        // Create user and venue
        User user = userRepository.save(new User("Bob Test", "bob.workflow@example.com", "pass123", "USER"));
        Venue venue = venueRepository.save(new Venue("Innovation Lab", "North Wing", 40, true, "LAB", "High-End PCs"));

        // Create booking
        LocalDate bookingDate = LocalDate.now().plusDays(5);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        Booking booking = bookingService.createBooking(
                user.getId(),
                venue.getId(),
                bookingDate,
                startTime,
                endTime,
                20,
                "MORNING",
                "Hackathon Brainstorming"
        );

        assertNotNull(booking.getId());
        assertEquals("PENDING", booking.getStatus());
        assertEquals(20, booking.getAttendees());
        List<BookingApprovalHistory> initialHistory = historyRepository.findByBookingIdOrderByTimestampDesc(booking.getId());
        assertEquals(1, initialHistory.size());

        // Approve booking
        Booking approved = bookingService.approveBooking(booking.getId(), "admin@venue.com", "Approved for hackathon");
        assertEquals("APPROVED", approved.getStatus());

        List<BookingApprovalHistory> history = historyRepository.findByBookingIdOrderByTimestampDesc(booking.getId());
        assertEquals(2, history.size());
        assertEquals("APPROVED", history.get(0).getNewStatus());
        assertEquals("PENDING", history.get(0).getPreviousStatus());
        assertEquals("admin@venue.com", history.get(0).getActionPerformedBy());
    }

    @Test
    @DisplayName("Verify Application Data Persistence and Constraint Integrity")
    void testPersistenceAndConstraints() {
        long userCountBefore = userRepository.count();
        long venueCountBefore = venueRepository.count();

        User user = userRepository.save(new User("Carol Tester", "carol.unique@example.com", "pwd", "USER"));
        Venue venue = venueRepository.save(new Venue("Workshop Room 101", "West Wing", 15, true));

        assertEquals(userCountBefore + 1, userRepository.count());
        assertEquals(venueCountBefore + 1, venueRepository.count());

        // Re-read from repository
        User reloadedUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(reloadedUser);
        assertEquals("carol.unique@example.com", reloadedUser.getEmail());
    }
}
