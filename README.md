# Loan Application and Approval Workflow

A long-term project that models the full lifecycle of a loan:

application -> document check -> credit scoring -> approval -> disbursement -> repayment schedule

## Goals
- **Workflow / state machine**: a loan moves through well-defined states, and only valid transitions are allowed.
- **Financial math**: repayment schedules using both *annuity* (equal installments) and *fixed-principal* (equal principal, decreasing interest) methods.
- **Role-based approvals**: users have roles (applicant, officer, approver) and large loans require the *four-eyes principle* (two different people must approve).

## Status
Early stage. Java 21 + Spring Boot . Currently only a minimal `POST /login` endpoint exists.

## Planned structure
- `src/main/java` - Spring Boot backend (API and business logic)
- `src/test/java` - automated tests
- `docs/` - requirements, mockups, architecture diagrams, tech stack, changelog

## Team
To be filled in (students and assigned roles).
