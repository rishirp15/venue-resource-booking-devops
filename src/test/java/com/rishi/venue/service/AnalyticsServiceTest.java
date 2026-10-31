package com.rishi.venue.service;

import com.rishi.venue.dto.analytics.AdminAnalyticsData;
import com.rishi.venue.dto.analytics.UserAnalyticsData;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User alice;
    private User bob;
    private Venue hallA;
    private Venue roomB;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "hash", "USER");
        alice.setId(10L);

        bob = new User("Bob", "bob@example.com", "hash", "USER");
        bob.setId(20L);

        hallA = new Venue("Auditorium", "Building A", 100);
        hallA.setId(1L);

        roomB = new Venue("Board Room", "Building B", 20);
        roomB.setId(2L);
    }

    @Test
    void shouldCalculateAdminAnalyticsWithRealData() {
        LocalDate d1 = LocalDate.of(2026, 11, 2); // Monday
        LocalDate d2 = LocalDate.of(2026, 11, 3); // Tuesday

        Booking b1 = new Booking(alice, hallA, d1, LocalTime.of(9, 0), LocalTime.of(12, 0), 50, "MORNING", "Keynote");
        b1.setStatus("APPROVED");

        Booking b2 = new Booking(alice, hallA, d2, LocalTime.of(13, 0), LocalTime.of(16, 0), 60, "AFTERNOON", "Lecture");
        b2.setStatus("COMPLETED");

        Booking b3 = new Booking(bob, roomB, d1, LocalTime.of(10, 0), LocalTime.of(11, 30), 10, "MORNING", "Sync");
        b3.setStatus("PENDING");

        Booking b4 = new Booking(bob, roomB, d2, LocalTime.of(14, 0), LocalTime.of(15, 0), 8, "AFTERNOON", "Interview");
        b4.setStatus("REJECTED");

        when(venueRepository.findAll()).thenReturn(List.of(hallA, roomB));
        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3, b4));

        AdminAnalyticsData data = analyticsService.getAdminAnalytics(null, null);

        assertNotNull(data);
        assertEquals(4, data.totalBookings());
        assertEquals(1, data.approvedBookings());
        assertEquals(1, data.completedBookings());
        assertEquals(1, data.pendingBookings());
        assertEquals(1, data.rejectedBookings());

        // Most booked venues: Hall A has 2, Room B has 2
        assertEquals(2, data.mostBookedVenues().size());
        assertEquals(50.0, data.mostBookedVenues().get(0).sharePercentage());

        // Monthly trends
        assertFalse(data.monthlyTrends().isEmpty());

        // Peak days: Monday has 2, Tuesday has 2
        assertFalse(data.peakBookingDays().isEmpty());

        // Utilization metrics
        assertEquals(2, data.venueUtilizations().size());
        assertEquals(50.0, data.venueUtilizations().get(0).utilizationPercentage());
    }

    @Test
    void shouldHandleEmptyDatasetGracefully() {
        when(venueRepository.findAll()).thenReturn(List.of(hallA, roomB));
        when(bookingRepository.findAll()).thenReturn(List.of());

        AdminAnalyticsData data = analyticsService.getAdminAnalytics(null, null);

        assertNotNull(data);
        assertEquals(0, data.totalBookings());
        assertEquals(0, data.approvedBookings());
        assertEquals(0, data.completedBookings());
        assertEquals(0, data.pendingBookings());
        assertEquals(0, data.rejectedBookings());
        assertEquals(0, data.cancelledBookings());

        // Utilization percentage should be 0.0 with no division by zero error
        for (var vu : data.venueUtilizations()) {
            assertEquals(0.0, vu.utilizationPercentage());
            assertEquals(0, vu.totalBookings());
        }
    }

    @Test
    void shouldFilterAdminAnalyticsByDateRange() {
        LocalDate inRangeDate = LocalDate.of(2026, 11, 10);
        LocalDate outRangeDate = LocalDate.of(2026, 12, 10);

        Booking inRange = new Booking(alice, hallA, inRangeDate, "MORNING");
        inRange.setStatus("APPROVED");

        Booking outRange = new Booking(bob, roomB, outRangeDate, "AFTERNOON");
        outRange.setStatus("PENDING");

        when(venueRepository.findAll()).thenReturn(List.of(hallA, roomB));
        when(bookingRepository.findAll()).thenReturn(List.of(inRange, outRange));

        AdminAnalyticsData data = analyticsService.getAdminAnalytics(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        assertNotNull(data);
        assertEquals(1, data.totalBookings(), "Only the booking in November should be included");
    }

    @Test
    void shouldCalculateUserAnalyticsCorrectly() {
        LocalDate today = LocalDate.now();

        Booking upcomingApproved = new Booking(alice, hallA, today.plusDays(3), "MORNING");
        upcomingApproved.setStatus("APPROVED");

        Booking completed = new Booking(alice, hallA, today.minusDays(5), "AFTERNOON");
        completed.setStatus("COMPLETED");

        Booking cancelled = new Booking(alice, roomB, today.minusDays(10), "EVENING");
        cancelled.setStatus("CANCELLED");

        when(bookingRepository.findByUserIdOrderByBookingDateDesc(10L))
                .thenReturn(List.of(upcomingApproved, completed, cancelled));

        UserAnalyticsData data = analyticsService.getUserAnalytics(10L);

        assertNotNull(data);
        assertEquals(3, data.totalBookings());
        assertEquals(1, data.completedBookings());
        assertEquals(1, data.approvedBookings());
        assertEquals(1, data.cancelledBookings());
        assertEquals(1, data.upcomingBookingsCount());
        assertEquals(1, data.upcomingBookings().size());

        // Completion rate: 1 completed out of 3 = 33.3%
        assertEquals(33.3, data.completionRate());

        // Frequent venues: Hall A has 2 bookings (66.7%), Room B has 1 (33.3%)
        assertFalse(data.frequentlyUsedVenues().isEmpty());
        assertEquals(1L, data.frequentlyUsedVenues().get(0).venueId());
        assertEquals(2, data.frequentlyUsedVenues().get(0).bookingCount());
    }
}

