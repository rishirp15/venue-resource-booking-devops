package com.rishi.venue.service;

import com.rishi.venue.dto.AdminDashboardStats;
import com.rishi.venue.dto.UserDashboardStats;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceDashboardTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Venue venue1;
    private Venue venue2;

    @BeforeEach
    void setUp() {
        user = new User("Alice", "alice@example.com", "hash", "USER");
        user.setId(10L);

        venue1 = new Venue("Hall A", "Building 1", 100);
        venue1.setId(1L);

        venue2 = new Venue("Room B", "Building 2", 30);
        venue2.setId(2L);
    }

    @Test
    void shouldCalculateUserDashboardStatsCorrectly() {
        LocalDate today = LocalDate.now();

        Booking upcomingPending = new Booking(user, venue1, today.plusDays(3), "MORNING");
        upcomingPending.setId(101L);
        upcomingPending.setStatus("PENDING");

        Booking upcomingConfirmed = new Booking(user, venue2, today.plusDays(5), "AFTERNOON");
        upcomingConfirmed.setId(102L);
        upcomingConfirmed.setStatus("CONFIRMED");

        Booking pastCancelled = new Booking(user, venue1, today.minusDays(2), "EVENING");
        pastCancelled.setId(103L);
        pastCancelled.setStatus("CANCELLED");

        when(bookingRepository.findByUserIdOrderByBookingDateDesc(10L))
                .thenReturn(List.of(upcomingConfirmed, upcomingPending, pastCancelled));

        UserDashboardStats stats = bookingService.getUserDashboardStats(10L);

        assertNotNull(stats);
        assertEquals(3, stats.totalBookings());
        assertEquals(1, stats.pendingBookings());
        assertEquals(1, stats.confirmedBookings());
        assertEquals(1, stats.cancelledBookings());
        assertEquals(2, stats.upcomingBookings());
        assertEquals(2, stats.upcomingBookingList().size());
        assertEquals(3, stats.recentBookings().size());

        // Upcoming should be sorted chronologically (soonest first)
        assertEquals(101L, stats.upcomingBookingList().get(0).getId());
        assertEquals(102L, stats.upcomingBookingList().get(1).getId());
    }

    @Test
    void shouldCalculateAdminDashboardStatsCorrectly() {
        when(userRepository.count()).thenReturn(5L);
        when(venueRepository.count()).thenReturn(2L);
        when(venueRepository.findAll()).thenReturn(List.of(venue1, venue2));

        LocalDate today = LocalDate.now();
        Booking b1 = new Booking(user, venue1, today.plusDays(1), "MORNING");
        b1.setId(1L);
        b1.setStatus("PENDING");

        Booking b2 = new Booking(user, venue1, today.plusDays(2), "AFTERNOON");
        b2.setId(2L);
        b2.setStatus("CONFIRMED");

        Booking b3 = new Booking(user, venue2, today.plusDays(1), "EVENING");
        b3.setId(3L);
        b3.setStatus("CANCELLED");

        when(bookingRepository.findAllByOrderByBookingDateDesc())
                .thenReturn(List.of(b2, b1, b3));

        AdminDashboardStats stats = bookingService.getAdminDashboardStats();

        assertNotNull(stats);
        assertEquals(5, stats.totalUsers());
        assertEquals(2, stats.totalVenues());
        assertEquals(3, stats.totalBookings());
        assertEquals(1, stats.pendingApprovals());
        assertEquals(1, stats.confirmedBookings());
        assertEquals(1, stats.cancelledBookings());

        // Verify venue utilizations
        assertEquals(2, stats.venueUtilizations().size());
        var util1 = stats.venueUtilizations().stream()
                .filter(u -> u.venue().getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertEquals(2, util1.totalBookings());
        assertEquals(2, util1.activeBookings());
        assertEquals(1, util1.confirmedBookings());

        var util2 = stats.venueUtilizations().stream()
                .filter(u -> u.venue().getId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertEquals(1, util2.totalBookings());
        assertEquals(0, util2.activeBookings());
    }
}
