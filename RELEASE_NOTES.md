# Venue Resource Booking System
## Release Candidate v1.0.0-rc.1

### Overview
The Venue Resource Booking System is a Spring Boot
web application designed to manage venue reservations
through an automated DevOps workflow.

### Features
- User registration and session-based authentication
- BCrypt password hashing
- Venue listing and availability checking
- Venue booking and cancellation
- Booking confirmation and administration
- Concurrency protection against double booking
- CSRF protection for state-changing requests

### Technology Stack
- Java 21
- Spring Boot 3.5.6
- Spring Data JPA
- Thymeleaf
- H2 Database
- Maven
- Git and GitHub

### Testing
- 10 booking service unit tests
- 1 booking concurrency integration test
- CSRF rejection test

### Deployment Artifact
venue-booking.war

### Release Status
Release candidate. Final validation and deployment
verification are required before production release.