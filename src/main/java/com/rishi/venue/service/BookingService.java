package com.rishi.venue.service;

import com.rishi.venue.dto.AdminDashboardStats;
import com.rishi.venue.dto.UserDashboardStats;
import com.rishi.venue.dto.VenueUtilization;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;

import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    public static final Set<String> ACTIVE_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    public static final List<String> SLOTS =
            List.of("MORNING", "AFTERNOON", "EVENING");

    public static final LocalTime MORNING_START = LocalTime.of(9, 0);
    public static final LocalTime MORNING_END = LocalTime.of(12, 0);

    public static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    public static final LocalTime AFTERNOON_END = LocalTime.of(16, 0);

    public static final LocalTime EVENING_START = LocalTime.of(17, 0);
    public static final LocalTime EVENING_END = LocalTime.of(20, 0);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VenueRepository venueRepository) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    public static LocalTime getDefaultStartTime(String slot) {
        if (slot == null) {
            return MORNING_START;
        }
        return switch (slot.toUpperCase()) {
            case "AFTERNOON" -> AFTERNOON_START;
            case "EVENING" -> EVENING_START;
            default -> MORNING_START;
        };
    }

    public static LocalTime getDefaultEndTime(String slot) {
        if (slot == null) {
            return MORNING_END;
        }
        return switch (slot.toUpperCase()) {
            case "AFTERNOON" -> AFTERNOON_END;
            case "EVENING" -> EVENING_END;
            default -> MORNING_END;
        };
    }

    public static boolean isOverlapping(
            LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public boolean hasTimeConflict(
            Venue venue,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeBookingId) {

        if (venue == null || date == null || startTime == null || endTime == null) {
            return false;
        }

        List<Booking> activeBookings = bookingRepository
                .findByVenueAndBookingDateAndStatusIn(venue, date, ACTIVE_STATUSES);

        if (activeBookings == null || activeBookings.isEmpty()) {
            return false;
        }

        for (Booking existing : activeBookings) {
            if (excludeBookingId != null && existing.getId().equals(excludeBookingId)) {
                continue;
            }

            LocalTime existingStart = existing.getEffectiveStartTime();
            LocalTime existingEnd = existing.getEffectiveEndTime();

            if (isOverlapping(startTime, endTime, existingStart, existingEnd)) {
                return true;
            }
        }

        return false;
    }

    public boolean isSlotBooked(
            Venue venue,
            LocalDate date,
            String slot) {

        boolean slotExists = bookingRepository
                .existsByVenueAndBookingDateAndSlotAndStatusIn(
                        venue,
                        date,
                        slot,
                        ACTIVE_STATUSES
                );

        if (slotExists) {
            return true;
        }

        LocalTime slotStart = getDefaultStartTime(slot);
        LocalTime slotEnd = getDefaultEndTime(slot);

        return hasTimeConflict(venue, date, slotStart, slotEnd, null);
    }

    @Transactional
    public Booking createBooking(
            Long userId,
            Long venueId,
            LocalDate bookingDate,
            String slot) {

        LocalTime startTime = getDefaultStartTime(slot);
        LocalTime endTime = getDefaultEndTime(slot);

        return createBooking(
                userId,
                venueId,
                bookingDate,
                startTime,
                endTime,
                1,
                slot,
                null
        );
    }

    @Transactional
    public Booking createBooking(
            Long userId,
            Long venueId,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer attendees,
            String slot,
            String purpose) {

        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Please select a valid future or current date."
            );
        }

        if (startTime == null && slot != null) {
            startTime = getDefaultStartTime(slot);
        }
        if (endTime == null && slot != null) {
            endTime = getDefaultEndTime(slot);
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required."
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time."
            );
        }

        if (bookingDate.equals(LocalDate.now()) && startTime.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException(
                    "Booking start time cannot be in the past."
            );
        }

        if (slot == null || slot.isBlank()) {
            slot = startTime.getHour() < 12 ? "MORNING" :
                    (startTime.getHour() < 17 ? "AFTERNOON" : "EVENING");
        }

        if (attendees == null || attendees < 1) {
            attendees = 1;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );

        // Lock the venue before checking slot availability to prevent race conditions.
        Venue venue = venueRepository.findByIdForUpdate(venueId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found."
                        )
                );

        if (venue.getActive() != null && !venue.getActive()) {
            throw new IllegalArgumentException(
                    "Venue '" + venue.getName() + "' is currently inactive and cannot be booked."
            );
        }

        if (attendees > venue.getCapacity()) {
            throw new IllegalArgumentException(
                    "Requested attendees (" + attendees + ") exceeds venue capacity (" + venue.getCapacity() + ")."
            );
        }

        if (isSlotBooked(venue, bookingDate, slot) || hasTimeConflict(venue, bookingDate, startTime, endTime, null)) {
            throw new IllegalArgumentException(
                    "This slot is already booked. Please choose another."
            );
        }

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                startTime,
                endTime,
                attendees,
                slot,
                purpose
        );

        booking.setStatus("PENDING");

        return bookingRepository.save(booking);
    }

    public List<Venue> findAlternativeVenues(
            Long requestedVenueId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer attendees) {

        if (date == null || date.isBefore(LocalDate.now())) {
            return List.of();
        }

        if (startTime == null) {
            startTime = MORNING_START;
        }
        if (endTime == null) {
            endTime = MORNING_END;
        }
        if (!endTime.isAfter(startTime)) {
            return List.of();
        }

        final int requiredCapacity = (attendees != null && attendees > 0) ? attendees : 1;
        final LocalTime finalStart = startTime;
        final LocalTime finalEnd = endTime;

        List<Venue> allVenues = venueRepository.findAll();

        return allVenues.stream()
                .filter(v -> requestedVenueId == null || !v.getId().equals(requestedVenueId))
                .filter(v -> v.getActive() == null || v.getActive())
                .filter(v -> v.getCapacity() >= requiredCapacity)
                .filter(v -> !hasTimeConflict(v, date, finalStart, finalEnd, null))
                .sorted(Comparator.comparingInt(Venue::getCapacity))
                .toList();
    }

    public Booking getBookingDetails(Long bookingId, Long userId, String userRole) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        boolean isAdmin = "ADMIN".equals(userRole);
        if (!isAdmin && (userId == null || !booking.getUser().getId().equals(userId))) {
            throw new SecurityException("You are not authorized to view this booking.");
        }

        return booking;
    }

    @Transactional
    public Booking rescheduleBooking(
            Long bookingId,
            Long userId,
            LocalDate newDate,
            LocalTime newStartTime,
            LocalTime newEndTime,
            String newSlot) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("You cannot reschedule another user's booking.");
        }

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new IllegalArgumentException("Only active (pending or confirmed) bookings can be rescheduled.");
        }

        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Please select a valid future or current date.");
        }

        if (newStartTime == null && newSlot != null) {
            newStartTime = getDefaultStartTime(newSlot);
        }
        if (newEndTime == null && newSlot != null) {
            newEndTime = getDefaultEndTime(newSlot);
        }

        if (newStartTime == null || newEndTime == null || !newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        if (newDate.equals(LocalDate.now()) && newStartTime.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Rescheduled start time cannot be in the past.");
        }

        if (newSlot == null || newSlot.isBlank()) {
            newSlot = newStartTime.getHour() < 12 ? "MORNING" :
                    (newStartTime.getHour() < 17 ? "AFTERNOON" : "EVENING");
        }

        // Lock venue to prevent race conditions during rescheduling
        Venue venue = venueRepository.findByIdForUpdate(booking.getVenue().getId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found."));

        if (venue.getActive() != null && !venue.getActive()) {
            throw new IllegalArgumentException("This venue is inactive and cannot be rescheduled.");
        }

        // Check conflict excluding this current booking
        if (hasTimeConflict(venue, newDate, newStartTime, newEndTime, booking.getId())) {
            throw new IllegalArgumentException("The requested rescheduled time slot is already booked for this venue.");
        }

        booking.setBookingDate(newDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setSlot(newSlot);
        // Rescheduled bookings return to PENDING status for administrative review
        booking.setStatus("PENDING");

        return bookingRepository.save(booking);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository
                .findByUserIdOrderByBookingDateDesc(userId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository
                .findAllByOrderByBookingDateDesc();
    }

    public UserDashboardStats getUserDashboardStats(Long userId) {
        List<Booking> userBookings = getUserBookings(userId);
        LocalDate today = LocalDate.now();

        long totalBookings = userBookings.size();
        long pendingBookings = userBookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count();
        long confirmedBookings = userBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();
        long cancelledBookings = userBookings.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count();

        List<Booking> upcomingBookingList = userBookings.stream()
                .filter(b -> !b.getBookingDate().isBefore(today)
                        && ACTIVE_STATUSES.contains(b.getStatus()))
                .sorted(Comparator.comparing(Booking::getBookingDate))
                .toList();

        long upcomingBookings = upcomingBookingList.size();

        List<Booking> recentBookings = userBookings.stream()
                .limit(5)
                .toList();

        return new UserDashboardStats(
                totalBookings,
                pendingBookings,
                confirmedBookings,
                cancelledBookings,
                upcomingBookings,
                upcomingBookingList,
                recentBookings
        );
    }

    public AdminDashboardStats getAdminDashboardStats() {
        long totalUsers = userRepository.count();
        long totalVenues = venueRepository.count();
        List<Booking> allBookings = getAllBookings();
        long totalBookings = allBookings.size();

        long pendingApprovals = allBookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count();
        long confirmedBookings = allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();
        long cancelledBookings = allBookings.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count();

        List<Venue> venues = venueRepository.findAll();
        List<VenueUtilization> venueUtilizations = venues.stream().map(venue -> {
            long venueTotal = allBookings.stream()
                    .filter(b -> b.getVenue().getId().equals(venue.getId()))
                    .count();
            long venueActive = allBookings.stream()
                    .filter(b -> b.getVenue().getId().equals(venue.getId())
                            && ACTIVE_STATUSES.contains(b.getStatus()))
                    .count();
            long venueConfirmed = allBookings.stream()
                    .filter(b -> b.getVenue().getId().equals(venue.getId())
                            && "CONFIRMED".equals(b.getStatus()))
                    .count();
            return new VenueUtilization(venue, venueTotal, venueActive, venueConfirmed);
        }).toList();

        List<Booking> recentBookings = allBookings.stream()
                .limit(6)
                .toList();

        return new AdminDashboardStats(
                totalUsers,
                totalVenues,
                totalBookings,
                pendingApprovals,
                confirmedBookings,
                cancelledBookings,
                venueUtilizations,
                recentBookings
        );
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found."
                        )
                );

        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending bookings can be confirmed."
            );
        }

        booking.setStatus("CONFIRMED");

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(
            Long bookingId,
            Long userId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found."
                        )
                );

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException(
                    "You cannot cancel another user's booking."
            );
        }

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "This booking cannot be cancelled."
            );
        }

        booking.setStatus("CANCELLED");

        return bookingRepository.save(booking);
    }
}
