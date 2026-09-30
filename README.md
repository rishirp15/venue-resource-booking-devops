# Venue Resource Booking System

An Ansible-Provisioned Venue Resource Booking System developed as an individual DevOps project.

The project demonstrates the complete software delivery lifecycle using application development, source control, continuous integration, automated testing, containerization, continuous deployment and configuration management.

---

## Project Overview

Organizations such as educational institutions and offices manage shared resources including seminar halls, conference rooms, laboratories and meeting rooms.

Manual booking processes can make it difficult to determine availability, prevent conflicting bookings and track the status of booking requests.

The Venue Resource Booking System provides a centralized web application through which users can:

- Register and login
- View available venues
- View booking slots
- Select a booking date
- Submit booking requests
- Track booking status
- Cancel bookings

Administrators can:

- View booking requests
- Confirm pending bookings
- Monitor booking status

---

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java 21 |
| Framework | Spring Boot |
| Frontend | Thymeleaf, HTML, CSS |
| Persistence | Spring Data JPA |
| Database | H2 |
| Build Tool | Maven |
| Version Control | Git |
| Repository Hosting | GitHub |
| CI/CD | Jenkins |
| Automated Testing | Selenium WebDriver |
| Application Server | Apache Tomcat |
| Containerization | Docker |
| Container Registry | Docker Hub |
| Configuration Management | Ansible |

---

## MVP Scope

The project contains a frozen 15-item MVP:

1. Application Landing Page
2. User Registration
3. User Login and Session
4. Venue Catalogue
5. Venue Details
6. Slot Availability View
7. Booking Date Selection
8. Booking Request
9. Booking Conflict Prevention
10. Pending Booking Status
11. My Bookings / Status Tracking
12. Administrator Booking View
13. Booking Confirmation
14. Booking Cancellation
15. Application Health Endpoint

---

## DevOps Lifecycle

The project follows the lifecycle:

Plan → Code → Version Control → Build → Continuous Integration → Test → Package → Containerize → Deploy → Configure → Operate → Monitor

Tools used across this lifecycle include Git, GitHub, Maven, Jenkins, Selenium, Docker and Ansible.

---

## Current Application Structure

```text
venue-booking-project/
├── docs/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
├── pom.xml
├── README.md
└── .gitignore