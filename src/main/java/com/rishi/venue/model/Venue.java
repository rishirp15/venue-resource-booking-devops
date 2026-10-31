package com.rishi.venue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "venue_type")
    private String venueType = "GENERAL";

    @Column(name = "facilities")
    private String facilities = "Standard Amenities";

    public Venue() {
    }

    public Venue(String name, String location, Integer capacity) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.active = true;
        this.venueType = "GENERAL";
        this.facilities = "Standard Amenities";
    }

    public Venue(String name, String location, Integer capacity, Boolean active) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.active = active != null ? active : true;
        this.venueType = "GENERAL";
        this.facilities = "Standard Amenities";
    }

    public Venue(String name, String location, Integer capacity, Boolean active, String venueType, String facilities) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.active = active != null ? active : true;
        this.venueType = (venueType != null && !venueType.isBlank()) ? venueType : "GENERAL";
        this.facilities = (facilities != null && !facilities.isBlank()) ? facilities : "Standard Amenities";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active != null ? active : true;
    }

    public boolean isActive() {
        return getActive();
    }

    public String getVenueType() {
        return venueType != null ? venueType : "GENERAL";
    }

    public void setVenueType(String venueType) {
        this.venueType = (venueType != null && !venueType.isBlank()) ? venueType : "GENERAL";
    }

    public String getFacilities() {
        return facilities != null ? facilities : "Standard Amenities";
    }

    public void setFacilities(String facilities) {
        this.facilities = (facilities != null && !facilities.isBlank()) ? facilities : "Standard Amenities";
    }
}
