package com.rishi.venue.service;

import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final AuditLogService auditLogService;

    public VenueService(
            VenueRepository venueRepository,
            @Autowired(required = false) AuditLogService auditLogService) {
        this.venueRepository = venueRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Venue> getVenueById(Long id) {
        if (id == null) return Optional.empty();
        return venueRepository.findById(id);
    }

    @Transactional
    public Venue createVenue(String name, String location, Integer capacity, Boolean active,
                             String venueType, String facilities, String performedBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name is required.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue location is required.");
        }
        if (capacity == null || capacity < 1) {
            throw new IllegalArgumentException("Venue capacity must be at least 1.");
        }

        name = name.trim();
        if (venueRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("A venue with name '" + name + "' already exists.");
        }

        Venue venue = new Venue(
                name,
                location.trim(),
                capacity,
                active != null ? active : true,
                (venueType != null && !venueType.isBlank()) ? venueType.trim().toUpperCase() : "GENERAL",
                (facilities != null && !facilities.isBlank()) ? facilities.trim() : "Standard Amenities"
        );

        Venue saved = venueRepository.save(venue);

        if (auditLogService != null) {
            auditLogService.log(
                    performedBy != null ? performedBy : "System Admin",
                    "VENUE_CREATED",
                    "VENUE",
                    saved.getId(),
                    String.format("Created venue '%s' (Capacity: %d, Location: %s, Type: %s)",
                            saved.getName(), saved.getCapacity(), saved.getLocation(), saved.getVenueType())
            );
        }

        return saved;
    }

    @Transactional
    public Venue updateVenue(Long id, String name, String location, Integer capacity, Boolean active,
                             String venueType, String facilities, String performedBy) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with ID: " + id));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name is required.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue location is required.");
        }
        if (capacity == null || capacity < 1) {
            throw new IllegalArgumentException("Venue capacity must be at least 1.");
        }

        name = name.trim();
        Optional<Venue> existing = venueRepository.findByName(name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Another venue with name '" + name + "' already exists.");
        }

        String oldSummary = String.format("Name: %s, Cap: %d, Active: %s", venue.getName(), venue.getCapacity(), venue.getActive());

        venue.setName(name);
        venue.setLocation(location.trim());
        venue.setCapacity(capacity);
        if (active != null) {
            venue.setActive(active);
        }
        if (venueType != null && !venueType.isBlank()) {
            venue.setVenueType(venueType.trim().toUpperCase());
        }
        if (facilities != null && !facilities.isBlank()) {
            venue.setFacilities(facilities.trim());
        }

        Venue updated = venueRepository.save(venue);

        if (auditLogService != null) {
            auditLogService.log(
                    performedBy != null ? performedBy : "System Admin",
                    "VENUE_UPDATED",
                    "VENUE",
                    updated.getId(),
                    String.format("Updated venue '%s'. Previous: [%s] -> New: [Name: %s, Cap: %d, Active: %s]",
                            updated.getName(), oldSummary, updated.getName(), updated.getCapacity(), updated.getActive())
            );
        }

        return updated;
    }

    @Transactional
    public Venue toggleActiveStatus(Long id, String performedBy) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found with ID: " + id));

        boolean newStatus = !venue.isActive();
        venue.setActive(newStatus);
        Venue updated = venueRepository.save(venue);

        if (auditLogService != null) {
            auditLogService.log(
                    performedBy != null ? performedBy : "System Admin",
                    "VENUE_UPDATED",
                    "VENUE",
                    updated.getId(),
                    String.format("Toggled status of venue '%s' to %s",
                            updated.getName(), newStatus ? "ACTIVE" : "INACTIVE")
            );
        }

        return updated;
    }
}
