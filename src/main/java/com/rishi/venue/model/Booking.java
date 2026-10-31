package com.rishi.venue.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private String slot;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private Integer attendees = 1;

    @Column
    private String purpose;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<BookingApprovalHistory> history = new ArrayList<>();

    public Booking() {}

    public Booking(User user, Venue venue,
                   LocalDate bookingDate, String slot) {
        this.user = user;
        this.venue = venue;
        this.bookingDate = bookingDate;
        this.slot = slot;
        this.status = "PENDING";
        this.attendees = 1;
    }

    public Booking(User user, Venue venue,
                   LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                   Integer attendees, String slot, String purpose) {
        this.user = user;
        this.venue = venue;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendees = attendees != null && attendees > 0 ? attendees : 1;
        this.slot = slot;
        this.purpose = purpose;
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getAttendees() {
        return attendees != null ? attendees : 1;
    }

    public void setAttendees(Integer attendees) {
        this.attendees = attendees != null && attendees > 0 ? attendees : 1;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<BookingApprovalHistory> getHistory() {
        return history;
    }

    public void setHistory(List<BookingApprovalHistory> history) {
        this.history = history;
    }

    public void addHistory(BookingApprovalHistory entry) {
        if (entry != null) {
            if (this.history == null) {
                this.history = new ArrayList<>();
            }
            this.history.add(entry);
            entry.setBooking(this);
        }
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(this.status) || "CONFIRMED".equalsIgnoreCase(this.status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(this.status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(this.status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(this.status);
    }

    public LocalTime getEffectiveStartTime() {
        if (startTime != null) {
            return startTime;
        }
        if (slot == null) {
            return LocalTime.of(9, 0);
        }
        return switch (slot.toUpperCase()) {
            case "AFTERNOON" -> LocalTime.of(13, 0);
            case "EVENING" -> LocalTime.of(17, 0);
            default -> LocalTime.of(9, 0);
        };
    }

    public LocalTime getEffectiveEndTime() {
        if (endTime != null) {
            return endTime;
        }
        if (slot == null) {
            return LocalTime.of(12, 0);
        }
        return switch (slot.toUpperCase()) {
            case "AFTERNOON" -> LocalTime.of(16, 0);
            case "EVENING" -> LocalTime.of(20, 0);
            default -> LocalTime.of(12, 0);
        };
    }
}
