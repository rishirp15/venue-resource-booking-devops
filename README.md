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
| Database | H2 / PostgreSQL (dual-profile) |
| Build Tool | Maven |
| Version Control | Git |
| Repository Hosting | GitHub |
| CI/CD | Jenkins |
| Automated Testing | Selenium WebDriver & JUnit 5 |
| Application Server | Apache Tomcat (Embedded) |
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
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   └── PULL_REQUEST_TEMPLATE.md
├── docs/
│   ├── git-conflict-demo/
│   │   └── release-status.txt
│   ├── AGILE_PLANNING.md
│   └── GIT_WORKFLOW.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
├── pom.xml
├── README.md
└── .gitignore
```

---

## Project Documentation & Engineering Guides

- **Agile Planning & Management**: [docs/AGILE_PLANNING.md](file:///r:/DevOpsProject/venue-booking-project/docs/AGILE_PLANNING.md)
  - Project Problem Statement, Objectives, and Scope
  - Functional & Non-Functional Requirements (FR-01 to FR-12, NFR-01 to NFR-08)
  - User Stories, Acceptance Criteria, and Prioritized Product Backlog
  - 4-Sprint Planning Matrix and Velocity Allocation
  - Definition of Done (DoD) Checklist
  - Kanban Workflow with strict WIP limits (WIP = 2)

- **Git & GitHub Workflow Specification**: [docs/GIT_WORKFLOW.md](file:///r:/DevOpsProject/venue-booking-project/docs/GIT_WORKFLOW.md)
  - GitFlow Branching Model (`main`, `develop`, `feature/*`, `release/*`, `hotfix/*`)
  - Conventional Commits Specification (`feat`, `fix`, `docs`, `ci`, `test`, `refactor`)
  - Pull Request Lifecycle and Code Review Process
  - Merge Strategies (Squash & Merge for features, Merge Commit for releases)
  - Documented Merge Conflict Resolution Walkthrough (using `docs/git-conflict-demo/release-status.txt`)
  - Semantic Versioning Release Tagging (`v1.0.0-rc.1` → `v1.0.0`)
  - Git History Integrity Guidelines (Zero force pushing, zero history rewriting)
