package com.rishi.venue.service;

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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingConflictTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Venue venue;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        user = new User("Alice", "alice@example.com", "hash", "USER");
        user.setId(1L);

        venue = new Venue("Auditorium", "Main Building", 100);
        venue.setId(10L);
        venue.setActive(true);

        bookingDate = LocalDate.now().plusDays(5);
    }

    @Test
    void shouldRejectWhenEndTimeIsBeforeStartTime() {
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(12, 0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, 10L, bookingDate, start, end, 20, "AFTERNOON", "Seminar")
        );
        assertTrue(ex.getMessage().contains("End time must be after start time"));
    }

    @Test
    void shouldRejectWhenAttendeesExceedVenueCapacity() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(venueRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venue));

        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, 10L, bookingDate, start, end, 150, "MORNING", "Oversized Event")
        );
        assertTrue(ex.getMessage().contains("exceeds venue capacity"));
    }

    @Test
    void shouldRejectWhenVenueIsInactive() {
        venue.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(venueRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venue));

        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, 10L, bookingDate, start, end, 20, "MORNING", "Test")
        );
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void shouldRejectOverlappingTimeBooking() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(venueRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venue));

        // Existing booking from 09:00 to 12:00
        Booking existing = new Booking(user, venue, bookingDate, LocalTime.of(9, 0), LocalTime.of(12, 0), 20, "MORNING", null);
        existing.setId(99L);
        existing.setStatus("CONFIRMED");

        when(bookingRepository.findByVenueAndBookingDateAndStatusIn(eq(venue), eq(bookingDate), anySet()))
                .thenReturn(List.of(existing));

        // New requested booking overlaps: 10:00 to 11:30
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(11, 30);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, 10L, bookingDate, newStart, newEnd, 20, "MORNING", "Conflict Test")
        );
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    void shouldAllowConsecutiveNonOverlappingBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(venueRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venue));

        // Existing booking ends exactly at 12:00
        Booking existing = new Booking(user, venue, bookingDate, LocalTime.of(9, 0), LocalTime.of(12, 0), 20, "MORNING", null);
        existing.setId(99L);
        existing.setStatus("CONFIRMED");

        when(bookingRepository.findByVenueAndBookingDateAndStatusIn(eq(venue), eq(bookingDate), anySet()))
                .thenReturn(List.of(existing));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // New booking starts exactly at 12:00 (back-to-back)
        LocalTime newStart = LocalTime.of(12, 0);
        LocalTime newEnd = LocalTime.of(14, 0);

        Booking result = bookingService.createBooking(1L, 10L, bookingDate, newStart, newEnd, 20, "AFTERNOON", "Back-to-back");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(newStart, result.getStartTime());
        assertEquals(newEnd, result.getEndTime());
    }

    @Test
    void shouldSuggestSmartAlternativeVenues() {
        Venue venueA = new Venue("Auditorium A", "Building 1", 100);
        venueA.setId(1L);

        Venue venueB = new Venue("Seminar Room B", "Building 2", 50);
        venueB.setId(2L);

        Venue smallRoom = new Venue("Small Room", "Building 3", 10);
        smallRoom.setId(3L);

        when(venueRepository.findAll()).thenReturn(List.of(venueA, venueB, smallRoom));

        // Auditorium A is the requested one, so alternatives should exclude A
        // Required attendees: 40, so smallRoom (10) should be excluded
        // Seminar Room B has capacity 50 and is free
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 0);

        List<Venue> alternatives = bookingService.findAlternativeVenues(1L, bookingDate, start, end, 40);

        assertEquals(1, alternatives.size());
        assertEquals(2L, alternatives.get(0).getId());
        assertEquals("Seminar Room B", alternatives.get(0).getName());
    }

    @Test
    void shouldRescheduleActiveBookingSuccessfully() {
        Booking booking = new Booking(user, venue, bookingDate, LocalTime.of(9, 0), LocalTime.of(12, 0), 10, "MORNING", null);
        booking.setId(50L);
        booking.setStatus("CONFIRMED");

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(venueRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(venue));
        when(bookingRepository.findByVenueAndBookingDateAndStatusIn(eq(venue), any(LocalDate.class), anySet()))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDate = bookingDate.plusDays(2);
        LocalTime newStart = LocalTime.of(14, 0);
        LocalTime newEnd = LocalTime.of(16, 0);

        Booking rescheduled = bookingService.rescheduleBooking(50L, 1L, newDate, newStart, newEnd, "AFTERNOON");

        assertNotNull(rescheduled);
        assertEquals(newDate, rescheduled.getBookingDate());
        assertEquals(newStart, rescheduled.getStartTime());
        assertEquals(newEnd, rescheduled.getEndTime());
        assertEquals("PENDING", rescheduled.getStatus()); // Rescheduled resets to PENDING
    }
}

