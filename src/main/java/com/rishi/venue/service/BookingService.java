package com.rishi.venue.service;

import com.rishi.venue.dto.AdminDashboardStats;
import com.rishi.venue.dto.UserDashboardStats;
import com.rishi.venue.dto.VenueUtilization;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.BookingApprovalHistory;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingApprovalHistoryRepository;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class BookingService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final Set<String> ACTIVE_STATUSES =
            Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_CONFIRMED);

    public static final List<String> HOURLY_SLOTS = List.of(
            "08:00 - 09:00",
            "09:00 - 10:00",
            "10:00 - 11:00",
            "11:00 - 12:00",
            "12:00 - 13:00",
            "13:00 - 14:00",
            "14:00 - 15:00",
            "15:00 - 16:00",
            "16:00 - 17:00",
            "17:00 - 18:00",
            "18:00 - 19:00",
            "19:00 - 20:00"
    );

    public static final List<String> BLOCK_SLOTS = List.of(
            "MORNING", "AFTERNOON", "EVENING", "FULL_DAY"
    );

    public static final List<String> SLOTS;
    static {
        List<String> list = new ArrayList<>(HOURLY_SLOTS);
        list.addAll(BLOCK_SLOTS);
        SLOTS = Collections.unmodifiableList(list);
    }

    public static final LocalTime OPERATING_OPEN = LocalTime.of(8, 0);
    public static final LocalTime OPERATING_CLOSE = LocalTime.of(20, 0);

    public static final LocalTime MORNING_START = LocalTime.of(9, 0);
    public static final LocalTime MORNING_END = LocalTime.of(12, 0);

    public static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    public static final LocalTime AFTERNOON_END = LocalTime.of(16, 0);

    public static final LocalTime EVENING_START = LocalTime.of(17, 0);
    public static final LocalTime EVENING_END = LocalTime.of(20, 0);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingApprovalHistoryRepository historyRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VenueRepository venueRepository) {
        this(bookingRepository, userRepository, venueRepository, null, null, null);
    }

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VenueRepository venueRepository,
            BookingApprovalHistoryRepository historyRepository) {
        this(bookingRepository, userRepository, venueRepository, historyRepository, null, null);
    }

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VenueRepository venueRepository,
            @Autowired(required = false) BookingApprovalHistoryRepository historyRepository,
            @Autowired(required = false) AuditLogService auditLogService,
            @Autowired(required = false) NotificationService notificationService) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.historyRepository = historyRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public boolean isApprovedStatus(String status) {
        return STATUS_APPROVED.equalsIgnoreCase(status) || STATUS_CONFIRMED.equalsIgnoreCase(status);
    }

    public static LocalTime getDefaultStartTime(String slot) {
        if (slot == null || slot.isBlank()) return MORNING_START;
        String s = slot.trim().toUpperCase();
        if (s.startsWith("AFTERNOON")) return AFTERNOON_START;
        if (s.startsWith("EVENING")) return EVENING_START;
        if (s.startsWith("MORNING")) return MORNING_START;
        if (s.startsWith("FULL_DAY") || s.startsWith("FULL DAY")) return OPERATING_OPEN;
        if (s.contains("-")) {
            try {
                String startPart = s.split("-")[0].trim();
                return LocalTime.parse(startPart);
            } catch (Exception ignored) {}
        }
        return MORNING_START;
    }

    public static LocalTime getDefaultEndTime(String slot) {
        if (slot == null || slot.isBlank()) return MORNING_END;
        String s = slot.trim().toUpperCase();
        if (s.startsWith("AFTERNOON")) return AFTERNOON_END;
        if (s.startsWith("EVENING")) return EVENING_END;
        if (s.startsWith("MORNING")) return MORNING_END;
        if (s.startsWith("FULL_DAY") || s.startsWith("FULL DAY")) return OPERATING_CLOSE;
        if (s.contains("-")) {
            try {
                String endPart = s.split("-")[1].trim();
                return LocalTime.parse(endPart);
            } catch (Exception ignored) {}
        }
        return MORNING_END;
    }

    public static boolean isOverlapping(
            LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    public boolean isSlotBooked(
            Venue venue,
            LocalDate bookingDate,
            String slot) {

        if (venue == null || bookingDate == null) {
            return false;
        }

        boolean directSlotBooked = bookingRepository
                .existsByVenueAndBookingDateAndSlotAndStatusIn(
                        venue,
                        bookingDate,
                        slot,
                        ACTIVE_STATUSES
                );

        if (directSlotBooked) {
            return true;
        }

        LocalTime start = getDefaultStartTime(slot);
        LocalTime end = getDefaultEndTime(slot);
        return hasTimeConflict(venue, bookingDate, start, end, null);
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

        List<Booking> bookingsOnDate = bookingRepository.findByVenueAndBookingDateAndStatusIn(
                venue,
                date,
                ACTIVE_STATUSES
        );

        for (Booking existing : bookingsOnDate) {
            if (excludeBookingId != null && excludeBookingId.equals(existing.getId())) {
                continue;
            }

            LocalTime existStart = existing.getEffectiveStartTime();
            LocalTime existEnd = existing.getEffectiveEndTime();

            if (isOverlapping(startTime, endTime, existStart, existEnd)) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public Booking createBooking(
            Long userId,
            Long venueId,
            LocalDate bookingDate,
            String slot) {

        if (slot == null || (!SLOTS.contains(slot.toUpperCase()) && !slot.contains("-"))) {
            throw new IllegalArgumentException("Please select a valid booking slot.");
        }

        LocalTime startTime = getDefaultStartTime(slot);
        LocalTime endTime = getDefaultEndTime(slot);

        return createBooking(userId, venueId, bookingDate, startTime, endTime, 1, slot, null);
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

        if (startTime.isBefore(OPERATING_OPEN) || endTime.isAfter(OPERATING_CLOSE)) {
            throw new IllegalArgumentException(
                    "Booking times must be within venue operating hours (08:00 - 20:00)."
            );
        }

        if (slot == null || slot.isBlank() || "CUSTOM".equalsIgnoreCase(slot)) {
            slot = startTime + " - " + endTime;
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

        booking.setStatus(STATUS_PENDING);

        Booking saved = bookingRepository.save(booking);

        recordHistory(saved, null, STATUS_PENDING,
                user.getEmail() + " (" + user.getRole() + ")",
                "Initial booking request submitted.");

        if (notificationService != null) {
            notificationService.notify(
                    user,
                    "Booking Request Submitted (#" + saved.getId() + ")",
                    "Your booking request for venue '" + venue.getName() + "' on " + bookingDate + " (" + slot + ") has been submitted and is pending administrator approval.",
                    "BOOKING_SUBMITTED",
                    saved.getId()
            );
        }

        return saved;
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

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        if (!isAdmin && (userId == null || !booking.getUser().getId().equals(userId))) {
            throw new SecurityException("You are not authorized to view this booking.");
        }

        return booking;
    }

    public List<BookingApprovalHistory> getBookingHistory(Long bookingId) {
        if (historyRepository == null || bookingId == null) {
            return List.of();
        }
        return historyRepository.findByBookingIdOrderByTimestampDesc(bookingId);
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
            throw new IllegalArgumentException("Only active (pending or approved) bookings can be rescheduled.");
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

        Venue venue = venueRepository.findByIdForUpdate(booking.getVenue().getId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found."));

        if (venue.getActive() != null && !venue.getActive()) {
            throw new IllegalArgumentException("This venue is inactive and cannot be rescheduled.");
        }

        if (hasTimeConflict(venue, newDate, newStartTime, newEndTime, booking.getId())) {
            throw new IllegalArgumentException("The requested rescheduled time slot is already booked for this venue.");
        }

        String prevStatus = booking.getStatus();
        booking.setBookingDate(newDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setSlot(newSlot);
        booking.setStatus(STATUS_PENDING);
        booking.setRejectionReason(null);

        Booking saved = bookingRepository.save(booking);

        recordHistory(saved, prevStatus, STATUS_PENDING,
                booking.getUser().getEmail() + " (" + booking.getUser().getRole() + ")",
                "Rescheduled to " + newDate + " (" + newStartTime + " - " + newEndTime + ", " + newSlot + "). Returned to PENDING for review.");

        if (notificationService != null && saved.getUser() != null) {
            notificationService.notify(
                    saved.getUser(),
                    "Booking Rescheduled (#" + saved.getId() + ")",
                    "Your booking #" + saved.getId() + " for '" + saved.getVenue().getName() + "' was rescheduled to " + newDate + " (" + newSlot + ") and is pending administrator approval.",
                    "BOOKING_RESCHEDULED",
                    saved.getId()
            );
        }

        return saved;
    }

    @Transactional
    public Booking approveBooking(Long bookingId, String adminIdentifier, String remarks) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be approved. Current status: " + booking.getStatus());
        }

        if (hasTimeConflict(booking.getVenue(), booking.getBookingDate(),
                booking.getEffectiveStartTime(), booking.getEffectiveEndTime(), booking.getId())) {
            throw new IllegalArgumentException("Cannot approve booking: time conflict with another active reservation.");
        }

        String prevStatus = booking.getStatus();
        booking.setStatus(STATUS_APPROVED);
        booking.setRejectionReason(null);
        Booking saved = bookingRepository.save(booking);

        String performer = (adminIdentifier != null && !adminIdentifier.isBlank())
                ? adminIdentifier : "Administrator";
        String note = (remarks != null && !remarks.isBlank())
                ? remarks : "Booking approved by administrator";
        recordHistory(saved, prevStatus, STATUS_APPROVED, performer, note);

        if (notificationService != null && saved.getUser() != null) {
            notificationService.notify(
                    saved.getUser(),
                    "Booking Approved (#" + saved.getId() + ")",
                    "Great news! Your booking request for '" + saved.getVenue().getName() + "' on " + saved.getBookingDate() + " (" + saved.getSlot() + ") has been approved." + ((remarks != null && !remarks.isBlank()) ? " Remarks: " + remarks : ""),
                    "BOOKING_APPROVED",
                    saved.getId()
            );
        }

        return saved;
    }

    @Transactional
    public Booking rejectBooking(Long bookingId, String adminIdentifier, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be rejected. Current status: " + booking.getStatus());
        }

        String prevStatus = booking.getStatus();
        booking.setStatus(STATUS_REJECTED);
        booking.setRejectionReason(rejectionReason.trim());
        Booking saved = bookingRepository.save(booking);

        String performer = (adminIdentifier != null && !adminIdentifier.isBlank())
                ? adminIdentifier : "Administrator";
        recordHistory(saved, prevStatus, STATUS_REJECTED, performer, rejectionReason.trim());

        if (notificationService != null && saved.getUser() != null) {
            notificationService.notify(
                    saved.getUser(),
                    "Booking Declined (#" + saved.getId() + ")",
                    "Your booking request for '" + saved.getVenue().getName() + "' on " + saved.getBookingDate() + " was declined. Reason: " + rejectionReason.trim(),
                    "BOOKING_REJECTED",
                    saved.getId()
            );
        }

        return saved;
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!STATUS_PENDING.equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only pending bookings can be confirmed.");
        }

        String prevStatus = booking.getStatus();
        booking.setStatus(STATUS_CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        recordHistory(saved, prevStatus, STATUS_CONFIRMED, "Administrator", "Booking confirmed");

        return saved;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {
        return cancelBooking(bookingId, userId, "Booking cancelled by user");
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        boolean isOwner = booking.getUser() != null && booking.getUser().getId() != null && booking.getUser().getId().equals(userId);
        User actor = null;
        if (userId != null && !isOwner) {
            actor = userRepository.findById(userId).orElse(null);
        }

        boolean isAdmin = actor != null && "ADMIN".equalsIgnoreCase(actor.getRole());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("You cannot cancel another user's booking.");
        }

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new IllegalArgumentException("This booking cannot be cancelled. Current status: " + booking.getStatus());
        }

        String prevStatus = booking.getStatus();
        booking.setStatus(STATUS_CANCELLED);
        Booking saved = bookingRepository.save(booking);

        String actorLabel;
        if (isOwner) {
            actorLabel = (booking.getUser().getEmail() != null)
                    ? (booking.getUser().getEmail() + " (" + booking.getUser().getRole() + ")") : "Owner";
        } else if (actor != null) {
            actorLabel = actor.getEmail() + " (" + actor.getRole() + ")";
        } else {
            actorLabel = "User";
        }
        String remarks = (reason != null && !reason.isBlank()) ? reason : "Booking cancelled";
        recordHistory(saved, prevStatus, STATUS_CANCELLED, actorLabel, remarks);

        if (notificationService != null && saved.getUser() != null) {
            notificationService.notify(
                    saved.getUser(),
                    "Booking Cancelled (#" + saved.getId() + ")",
                    "Your booking #" + saved.getId() + " for '" + saved.getVenue().getName() + "' on " + saved.getBookingDate() + " was cancelled." + ((reason != null && !reason.isBlank()) ? " Reason: " + reason : ""),
                    "BOOKING_CANCELLED",
                    saved.getId()
            );
        }

        return saved;
    }

    @Transactional
    public Booking completeBooking(Long bookingId, String actorIdentifier) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!isApprovedStatus(booking.getStatus())) {
            throw new IllegalArgumentException("Only approved bookings can be completed. Current status: " + booking.getStatus());
        }

        String prevStatus = booking.getStatus();
        booking.setStatus(STATUS_COMPLETED);
        Booking saved = bookingRepository.save(booking);

        String performer = (actorIdentifier != null && !actorIdentifier.isBlank())
                ? actorIdentifier : "Administrator";
        recordHistory(saved, prevStatus, STATUS_COMPLETED, performer, "Booking marked as completed.");

        return saved;
    }

    @Transactional
    public int autoMarkCompletedBookings() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Booking> allBookings = bookingRepository.findAll();
        int count = 0;
        for (Booking b : allBookings) {
            if (isApprovedStatus(b.getStatus())) {
                boolean isPast = b.getBookingDate().isBefore(today)
                        || (b.getBookingDate().equals(today) && b.getEffectiveEndTime().isBefore(now));
                if (isPast) {
                    String prevStatus = b.getStatus();
                    b.setStatus(STATUS_COMPLETED);
                    bookingRepository.save(b);
                    recordHistory(b, prevStatus, STATUS_COMPLETED, "System Scheduler",
                            "Automatically completed after scheduled time");
                    count++;
                }
            }
        }
        return count;
    }

    public List<Booking> getUserBookings(Long userId) {
        autoMarkCompletedBookings();
        return bookingRepository.findByUserIdOrderByBookingDateDesc(userId);
    }

    public List<Booking> getAllBookings() {
        autoMarkCompletedBookings();
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    public List<Booking> getBookingsWithFilters(String status, LocalDate date, Long venueId) {
        autoMarkCompletedBookings();
        String normalizedStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? null : status.trim().toUpperCase();

        return bookingRepository.findWithFilters(normalizedStatus, date, venueId);
    }

    public Map<String, Long> getBookingStatusCounts() {
        List<Booking> all = bookingRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        counts.put("ALL", (long) all.size());
        counts.put("PENDING", all.stream().filter(b -> STATUS_PENDING.equalsIgnoreCase(b.getStatus())).count());
        counts.put("APPROVED", all.stream().filter(b -> isApprovedStatus(b.getStatus())).count());
        counts.put("REJECTED", all.stream().filter(b -> STATUS_REJECTED.equalsIgnoreCase(b.getStatus())).count());
        counts.put("CANCELLED", all.stream().filter(b -> STATUS_CANCELLED.equalsIgnoreCase(b.getStatus())).count());
        counts.put("COMPLETED", all.stream().filter(b -> STATUS_COMPLETED.equalsIgnoreCase(b.getStatus())).count());
        return counts;
    }

    public UserDashboardStats getUserDashboardStats(Long userId) {
        List<Booking> userBookings = getUserBookings(userId);
        LocalDate today = LocalDate.now();

        long totalBookings = userBookings.size();
        long pendingBookings = userBookings.stream()
                .filter(b -> STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                .count();
        long confirmedBookings = userBookings.stream()
                .filter(b -> isApprovedStatus(b.getStatus()))
                .count();
        long cancelledBookings = userBookings.stream()
                .filter(b -> STATUS_CANCELLED.equalsIgnoreCase(b.getStatus()))
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
                .filter(b -> STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                .count();
        long confirmedBookings = allBookings.stream()
                .filter(b -> isApprovedStatus(b.getStatus()))
                .count();
        long cancelledBookings = allBookings.stream()
                .filter(b -> STATUS_CANCELLED.equalsIgnoreCase(b.getStatus()))
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
                            && isApprovedStatus(b.getStatus()))
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

    private void recordHistory(
            Booking booking,
            String previousStatus,
            String newStatus,
            String actionPerformedBy,
            String remarks) {

        if (historyRepository != null && booking != null) {
            try {
                BookingApprovalHistory entry = new BookingApprovalHistory(
                        booking,
                        previousStatus,
                        newStatus,
                        actionPerformedBy != null ? actionPerformedBy : "System",
                        LocalDateTime.now(),
                        remarks
                );
                historyRepository.save(entry);
            } catch (Exception ignored) {
                // Ignore any approval history failure to ensure main transaction completes
            }
        }

        if (auditLogService != null && booking != null) {
            try {
                String action = switch (newStatus != null ? newStatus.toUpperCase() : "UNKNOWN") {
                    case "APPROVED" -> "BOOKING_APPROVED";
                    case "CONFIRMED" -> "BOOKING_CONFIRMED";
                    case "REJECTED" -> "BOOKING_REJECTED";
                    case "CANCELLED" -> "BOOKING_CANCELLED";
                    case "COMPLETED" -> "BOOKING_COMPLETED";
                    case "PENDING" -> (previousStatus == null) ? "BOOKING_CREATED" : "BOOKING_RESCHEDULED";
                    default -> "BOOKING_" + newStatus;
                };

                String details = (remarks != null && !remarks.isBlank())
                        ? remarks
                        : String.format("Booking status transitioned from %s to %s", previousStatus, newStatus);

                auditLogService.log(
                        actionPerformedBy != null ? actionPerformedBy : "System",
                        action,
                        "BOOKING",
                        booking.getId(),
                        details
                );
            } catch (Exception ignored) {
                // Ignore any audit log failure to ensure main transaction completes
            }
        }
    }
}
