package com.rishi.venue.service;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.BookingApprovalHistory;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingApprovalHistoryRepository;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingApprovalWorkflowTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private BookingApprovalHistoryRepository historyRepository;

    @InjectMocks
    private BookingService bookingService;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private Venue venue;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        regularUser = new User("Alice Requester", "alice@example.com", "hash", "USER");
        regularUser.setId(10L);

        otherUser = new User("Mallory Malicious", "mallory@example.com", "hash", "USER");
        otherUser.setId(99L);

        adminUser = new User("Admin Bob", "admin@venue.com", "hash", "ADMIN");
        adminUser.setId(1L);

        venue = new Venue("Executive Hall", "Main Tower", 100);
        venue.setId(5L);
        venue.setActive(true);

        bookingDate = LocalDate.now().plusDays(5);
    }

    // 1. Newly created bookings have PENDING status and initial history entry
    @Test
    void shouldCreateBookingInPendingStatusWithHistoryEntry() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(regularUser));
        when(venueRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(venue));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(201L);
            return b;
        });

        Booking created = bookingService.createBooking(
                10L, 5L, bookingDate, LocalTime.of(9, 0), LocalTime.of(12, 0), 20, "MORNING", "Team Planning"
        );

        assertNotNull(created);
        assertEquals(BookingService.STATUS_PENDING, created.getStatus());

        ArgumentCaptor<BookingApprovalHistory> historyCaptor = ArgumentCaptor.forClass(BookingApprovalHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        BookingApprovalHistory history = historyCaptor.getValue();
        assertEquals(BookingService.STATUS_PENDING, history.getNewStatus());
        assertNull(history.getPreviousStatus());
        assertTrue(history.getActionPerformedBy().contains("alice@example.com"));
    }

    // 2. Admin approves PENDING booking successfully
    @Test
    void shouldApprovePendingBookingSuccessfully() {
        Booking booking = new Booking(regularUser, venue, bookingDate, "MORNING");
        booking.setId(301L);
        booking.setStatus(BookingService.STATUS_PENDING);

        when(bookingRepository.findById(301L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking approved = bookingService.approveBooking(301L, "admin@venue.com (ADMIN)", "Approved for department event");

        assertEquals(BookingService.STATUS_APPROVED, approved.getStatus());
        assertNull(approved.getRejectionReason());

        ArgumentCaptor<BookingApprovalHistory> historyCaptor = ArgumentCaptor.forClass(BookingApprovalHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        BookingApprovalHistory history = historyCaptor.getValue();
        assertEquals(BookingService.STATUS_PENDING, history.getPreviousStatus());
        assertEquals(BookingService.STATUS_APPROVED, history.getNewStatus());
        assertEquals("admin@venue.com (ADMIN)", history.getActionPerformedBy());
        assertEquals("Approved for department event", history.getRemarks());
    }

    // 3. Admin rejects PENDING booking with required reason
    @Test
    void shouldRejectPendingBookingWithReason() {
        Booking booking = new Booking(regularUser, venue, bookingDate, "MORNING");
        booking.setId(302L);
        booking.setStatus(BookingService.STATUS_PENDING);

        when(bookingRepository.findById(302L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking rejected = bookingService.rejectBooking(302L, "admin@venue.com (ADMIN)", "Hall booked for university maintenance");

        assertEquals(BookingService.STATUS_REJECTED, rejected.getStatus());
        assertEquals("Hall booked for university maintenance", rejected.getRejectionReason());

        ArgumentCaptor<BookingApprovalHistory> historyCaptor = ArgumentCaptor.forClass(BookingApprovalHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        BookingApprovalHistory history = historyCaptor.getValue();
        assertEquals(BookingService.STATUS_PENDING, history.getPreviousStatus());
        assertEquals(BookingService.STATUS_REJECTED, history.getNewStatus());
        assertEquals("Hall booked for university maintenance", history.getRemarks());
    }

    // 4. Rejecting without a reason is prevented
    @Test
    void shouldRequireReasonWhenRejectingBooking() {
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.rejectBooking(303L, "admin@venue.com", "")
        );
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.rejectBooking(303L, "admin@venue.com", "   ")
        );
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.rejectBooking(303L, "admin@venue.com", null)
        );
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // 5. Invalid status transitions are prevented
    @Test
    void shouldPreventInvalidTransitionsOnAlreadyTerminalBookings() {
        Booking cancelled = new Booking(regularUser, venue, bookingDate, "MORNING");
        cancelled.setId(401L);
        cancelled.setStatus(BookingService.STATUS_CANCELLED);
        when(bookingRepository.findById(401L)).thenReturn(Optional.of(cancelled));

        // Cannot approve a cancelled booking
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.approveBooking(401L, "admin@venue.com", "trying to approve")
        );
        // Cannot reject a cancelled booking
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.rejectBooking(401L, "admin@venue.com", "trying to reject")
        );

        Booking rejected = new Booking(regularUser, venue, bookingDate, "MORNING");
        rejected.setId(402L);
        rejected.setStatus(BookingService.STATUS_REJECTED);
        when(bookingRepository.findById(402L)).thenReturn(Optional.of(rejected));

        // Cannot approve an already rejected booking
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.approveBooking(402L, "admin@venue.com", "approve rejected")
        );

        Booking completed = new Booking(regularUser, venue, bookingDate, "MORNING");
        completed.setId(403L);
        completed.setStatus(BookingService.STATUS_COMPLETED);
        when(bookingRepository.findById(403L)).thenReturn(Optional.of(completed));

        // Cannot cancel a completed booking
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.cancelBooking(403L, 10L, "cancel completed")
        );
    }

    // 6. Slots released by REJECTED and CANCELLED bookings
    @Test
    void shouldReleaseSlotsForRejectedAndCancelledBookings() {
        assertFalse(BookingService.ACTIVE_STATUSES.contains(BookingService.STATUS_REJECTED),
                "REJECTED bookings must not be in active statuses");
        assertFalse(BookingService.ACTIVE_STATUSES.contains(BookingService.STATUS_CANCELLED),
                "CANCELLED bookings must not be in active statuses");
        assertFalse(BookingService.ACTIVE_STATUSES.contains(BookingService.STATUS_COMPLETED),
                "COMPLETED bookings must not be in active statuses");

        assertTrue(BookingService.ACTIVE_STATUSES.contains(BookingService.STATUS_PENDING));
        assertTrue(BookingService.ACTIVE_STATUSES.contains(BookingService.STATUS_APPROVED));
    }

    // 7. Cancellation security: only owner or admin can cancel
    @Test
    void shouldRejectCancellationByUnauthorizedUser() {
        Booking booking = new Booking(regularUser, venue, bookingDate, "MORNING");
        booking.setId(501L);
        booking.setStatus(BookingService.STATUS_PENDING);

        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherUser));

        assertThrows(SecurityException.class, () ->
                bookingService.cancelBooking(501L, 99L, "Unauthorized cancel attempt")
        );
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // 8. Admin can cancel an approved booking
    @Test
    void shouldAllowAdminToCancelApprovedBooking() {
        Booking booking = new Booking(regularUser, venue, bookingDate, "MORNING");
        booking.setId(502L);
        booking.setStatus(BookingService.STATUS_APPROVED);

        when(bookingRepository.findById(502L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(502L, 1L, "Cancelled by Admin due to emergency");

        assertEquals(BookingService.STATUS_CANCELLED, cancelled.getStatus());
        verify(historyRepository).save(any(BookingApprovalHistory.class));
    }

    // 9. Rescheduling returns active booking to PENDING status
    @Test
    void shouldReturnRescheduledBookingToPendingStatus() {
        Booking approved = new Booking(regularUser, venue, bookingDate, "MORNING");
        approved.setId(601L);
        approved.setStatus(BookingService.STATUS_APPROVED);

        LocalDate newDate = bookingDate.plusDays(3);
        when(bookingRepository.findById(601L)).thenReturn(Optional.of(approved));
        when(venueRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(venue));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking rescheduled = bookingService.rescheduleBooking(
                601L, 10L, newDate, LocalTime.of(13, 0), LocalTime.of(16, 0), "AFTERNOON"
        );

        assertEquals(BookingService.STATUS_PENDING, rescheduled.getStatus());
        assertEquals(newDate, rescheduled.getBookingDate());
        verify(historyRepository).save(any(BookingApprovalHistory.class));
    }

    // 10. Admin completes an approved booking
    @Test
    void shouldCompleteApprovedBooking() {
        Booking approved = new Booking(regularUser, venue, bookingDate, "MORNING");
        approved.setId(701L);
        approved.setStatus(BookingService.STATUS_APPROVED);

        when(bookingRepository.findById(701L)).thenReturn(Optional.of(approved));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking completed = bookingService.completeBooking(701L, "admin@venue.com (ADMIN)");

        assertEquals(BookingService.STATUS_COMPLETED, completed.getStatus());
        verify(historyRepository).save(any(BookingApprovalHistory.class));
    }

    // 11. Filtering bookings by status, date and venue
    @Test
    void shouldFilterBookingsCorrectly() {
        when(bookingRepository.findWithFilters(eq("PENDING"), eq(bookingDate), eq(5L)))
                .thenReturn(List.of(new Booking(regularUser, venue, bookingDate, "MORNING")));

        List<Booking> filtered = bookingService.getBookingsWithFilters("PENDING", bookingDate, 5L);

        assertEquals(1, filtered.size());
        verify(bookingRepository).findWithFilters(eq("PENDING"), eq(bookingDate), eq(5L));
    }
}

