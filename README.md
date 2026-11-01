# Loan Application and Approval Workflow

A long-term project that models the full lifecycle of a loan:

application -> document check -> credit scoring -> approval -> disbursement -> repayment schedule

## Goals
- **Workflow / state machine**: a loan moves through well-defined states, and only valid transitions are allowed.
- **Financial math**: repayment schedules using both *annuity* (equal installments) and *fixed-principal* (equal principal, decreasing interest) methods.
- **Role-based approvals**: users have roles (applicant, officer, approver) and large loans require the *four-eyes principle* (two different people must approve).

## Backend features
Java 21 + Spring Boot (Maven), in `backend/`.

- **Login**: `POST /login` checks a username and password (demo user only) and answers "user connected" or 401.
- **Create a loan**: `POST /loans` takes an applicant and an amount and returns an id with status `SUBMITTED`. Missing applicant or an amount <= 0 returns 400. Loans are not stored yet.
- **Loan status state machine**: a loan moves SUBMITTED -> DOCUMENTS_CHECKED -> SCORED -> APPROVED -> DISBURSED -> REPAYING -> CLOSED. It can be rejected before approval. Invalid moves throw an exception.
- **Status history and listeners**: every change is recorded with the old status, the new status and the time. Listeners are notified after each change.
- **Repayment schedules**: annuity (equal installments) and fixed-principal (equal principal, decreasing payments). The last balance is 0.
- **Credit scoring**: score from 300 to 850 based on debt-to-income ratio, years employed and missed payments. Risk is LOW, MEDIUM or HIGH.
- **Four-eyes approval**: loans up to 10,000 need 1 approval, larger loans need 2 different approvers. Only approvers can approve, never the applicant, never twice.
- **LoanService**: one entry point that runs the flow: check documents, score (HIGH risk is rejected), approve, disburse, get the repayment schedule.

## Run and test
```
cd backend
mvn spring-boot:run
mvn test
```

## Structure
- `backend/` - Spring Boot backend (API and business logic)
- `frontend/` - Angular web client (see `frontend/README.md`)
- `docs/` - requirements, mockups, architecture diagrams, tech stack, changelog

## Team
Ciochina Marius, Savu Bogdan, Croitoriu Cosmin Mircea
