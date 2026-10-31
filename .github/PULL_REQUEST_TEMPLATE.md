## Pull Request Description

### Summary of Changes
- Provide a concise summary of changes introduced in this PR.

### Related Backlog Item / Issue
- Closes #[issue-number]
- Relates to Backlog Item: [e.g. PB-05 / US-03]

### Type of Change
- [ ] `feat`: New feature (backward-compatible)
- [ ] `fix`: Bug fix
- [ ] `docs`: Documentation updates
- [ ] `test`: Unit or integration test additions
- [ ] `refactor`: Code refactoring with no behavioral changes
- [ ] `ci`: CI/CD pipeline or deployment updates
- [ ] `perf`: Performance improvement

---

## Verification & Testing

### Test Suite Execution
- [ ] `mvn clean verify` executed locally with zero failures.
- Total Tests: _____ | Failures: 0 | Errors: 0

### Functional Verification
- [ ] Verified manually in local browser (`http://localhost:8080`).
- [ ] Verified database state in H2 console or PostgreSQL.

---

## Definition of Done (DoD) Checklist

- [ ] All acceptance criteria in the user story are satisfied.
- [ ] Code adheres to Google Java Style and clean architecture standards.
- [ ] Unit test coverage on new service methods meets or exceeds 80%.
- [ ] Endpoints protected with Spring Security authorization and CSRF validation.
- [ ] Database schema changes compatible with both H2 and PostgreSQL profiles.
- [ ] No hardcoded passwords, secrets, or API keys committed.
- [ ] Docker container build succeeds (`docker build .`).
- [ ] Documentation updated (`README.md`, docs files where applicable).
