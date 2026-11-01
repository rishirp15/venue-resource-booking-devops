package com.rishi.venue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_approval_history")
public class BookingApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Column(name = "action_performed_by", nullable = false)
    private String actionPerformedBy;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    public BookingApprovalHistory() {
        this.timestamp = LocalDateTime.now();
    }

    public BookingApprovalHistory(Booking booking,
                                  String previousStatus,
                                  String newStatus,
                                  String actionPerformedBy,
                                  LocalDateTime timestamp,
                                  String remarks) {
        this.booking = booking;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actionPerformedBy = actionPerformedBy;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getActionPerformedBy() {
        return actionPerformedBy;
    }

    public void setActionPerformedBy(String actionPerformedBy) {
        this.actionPerformedBy = actionPerformedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

