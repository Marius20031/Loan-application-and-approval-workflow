package com.loanworkflow.persistence.seeder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.loanworkflow.approval.Role;
import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.ApprovalRecordEntity;
import com.loanworkflow.persistence.entity.DocumentEntity;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.entity.StatusChangeEntity;
import com.loanworkflow.persistence.entity.UserEntity;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;
import com.loanworkflow.persistence.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with demo users and realistic loan applications
 * across different stages of the approval lifecycle.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final boolean enabled;

    public DatabaseSeeder(
            UserRepository userRepository,
            LoanApplicationRepository loanApplicationRepository,
            @Value("${app.seed-data.enabled:true}") boolean enabled
    ) {
        this.userRepository = userRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Database seeding is disabled by configuration (app.seed-data.enabled=false).");
            return;
        }

        if (loanApplicationRepository.count() > 0) {
            log.info("Database already contains loan applications, skipping seeding.");
            return;
        }

        log.info("Populating database with demo users and sample loan applications...");
        seed();
        log.info("Database seeding completed successfully.");
    }

    /**
     * Seeds users and loan applications. Can be called programmatically or during startup.
     */
    @Transactional
    public void seed() {
        seedUsers();
        seedLoans();
    }

    private void seedUsers() {
        List<UserEntity> defaultUsers = List.of(
                new UserEntity("alice", "password123_hash", Role.APPLICANT),
                new UserEntity("bob", "password123_hash", Role.OFFICER),
                new UserEntity("carol", "password123_hash", Role.APPROVER),
                new UserEntity("dan", "password123_hash", Role.APPROVER)
        );

        for (UserEntity user : defaultUsers) {
            if (!userRepository.existsByUsername(user.getUsername())) {
                userRepository.save(user);
            }
        }
    }

    private void seedLoans() {
        Instant now = Instant.now();

        // 1. SUBMITTED: Personal loan (EUR 4,500)
        LoanApplicationEntity loan1 = new LoanApplicationEntity("alice", new BigDecimal("4500.00"), LoanType.PERSONAL);
        loan1.setStatus(LoanStatus.SUBMITTED);
        loan1.addDocument(new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_card_alice.pdf",
                DocumentStatus.PENDING,
                now.plus(365, ChronoUnit.DAYS)
        ));
        loan1.addDocument(new DocumentEntity(
                DocumentType.PROOF_OF_INCOME,
                "salary_slip_nov.pdf",
                DocumentStatus.PENDING,
                now.plus(90, ChronoUnit.DAYS)
        ));
        loan1.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(2, ChronoUnit.HOURS),
                "Initial submission"
        ));
        loanApplicationRepository.save(loan1);

        // 2. DOCUMENTS_CHECKED: Auto loan (EUR 18,000)
        LoanApplicationEntity loan2 = new LoanApplicationEntity("alice", new BigDecimal("18000.00"), LoanType.AUTO);
        loan2.setStatus(LoanStatus.DOCUMENTS_CHECKED);
        DocumentEntity id2 = new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_verified.pdf",
                DocumentStatus.VERIFIED,
                now.plus(300, ChronoUnit.DAYS)
        );
        id2.setVerifiedBy("bob");
        id2.setVerifiedAt(now.minus(12, ChronoUnit.HOURS));

        DocumentEntity income2 = new DocumentEntity(
                DocumentType.PROOF_OF_INCOME,
                "income_verified.pdf",
                DocumentStatus.VERIFIED,
                now.plus(60, ChronoUnit.DAYS)
        );
        income2.setVerifiedBy("bob");
        income2.setVerifiedAt(now.minus(12, ChronoUnit.HOURS));

        DocumentEntity car2 = new DocumentEntity(
                DocumentType.COLLATERAL_PROOF,
                "car_contract.pdf",
                DocumentStatus.VERIFIED,
                now.plus(180, ChronoUnit.DAYS)
        );
        car2.setVerifiedBy("bob");
        car2.setVerifiedAt(now.minus(12, ChronoUnit.HOURS));

        loan2.addDocument(id2);
        loan2.addDocument(income2);
        loan2.addDocument(car2);
        loan2.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(1, ChronoUnit.DAYS),
                "Submitted"
        ));
        loan2.addStatusChange(new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                now.minus(12, ChronoUnit.HOURS),
                "Documents verified by bob"
        ));
        loanApplicationRepository.save(loan2);

        // 3. SCORED: Personal loan (EUR 8,000)
        LoanApplicationEntity loan3 = new LoanApplicationEntity("alice", new BigDecimal("8000.00"), LoanType.PERSONAL);
        loan3.setStatus(LoanStatus.SCORED);
        loan3.addDocument(new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_3.pdf",
                DocumentStatus.VERIFIED,
                now.plus(200, ChronoUnit.DAYS)
        ));
        loan3.addDocument(new DocumentEntity(
                DocumentType.PROOF_OF_INCOME,
                "income_3.pdf",
                DocumentStatus.VERIFIED,
                now.plus(60, ChronoUnit.DAYS)
        ));
        loan3.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(2, ChronoUnit.DAYS),
                "Submitted"
        ));
        loan3.addStatusChange(new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                now.minus(1, ChronoUnit.DAYS),
                "Docs verified"
        ));
        loan3.addStatusChange(new StatusChangeEntity(
                LoanStatus.DOCUMENTS_CHECKED,
                LoanStatus.SCORED,
                now.minus(6, ChronoUnit.HOURS),
                "Credit score: Low Risk (780)"
        ));
        loanApplicationRepository.save(loan3);

        // 4. APPROVED: Mortgage loan (EUR 175,000) with Four-Eyes principle (Carol & Dan)
        LoanApplicationEntity loan4 = new LoanApplicationEntity(
                "alice",
                new BigDecimal("175000.00"),
                LoanType.MORTGAGE
        );
        loan4.setStatus(LoanStatus.APPROVED);
        loan4.addDocument(new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_mortgage.pdf",
                DocumentStatus.VERIFIED,
                now.plus(180, ChronoUnit.DAYS)
        ));
        loan4.addDocument(new DocumentEntity(
                DocumentType.PROOF_OF_INCOME,
                "income_mortgage.pdf",
                DocumentStatus.VERIFIED,
                now.plus(90, ChronoUnit.DAYS)
        ));
        loan4.addDocument(new DocumentEntity(
                DocumentType.TAX_RECORD,
                "tax_record.pdf",
                DocumentStatus.VERIFIED,
                now.plus(120, ChronoUnit.DAYS)
        ));
        loan4.addDocument(new DocumentEntity(
                DocumentType.PROPERTY_DEED,
                "property_deed.pdf",
                DocumentStatus.VERIFIED,
                now.plus(365, ChronoUnit.DAYS)
        ));
        loan4.addDocument(new DocumentEntity(
                DocumentType.FINANCIAL_STATEMENT,
                "bank_statement.pdf",
                DocumentStatus.VERIFIED,
                now.plus(60, ChronoUnit.DAYS)
        ));
        loan4.addApproval(new ApprovalRecordEntity("carol", 1, now.minus(4, ChronoUnit.HOURS)));
        loan4.addApproval(new ApprovalRecordEntity("dan", 2, now.minus(2, ChronoUnit.HOURS)));
        loan4.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(3, ChronoUnit.DAYS),
                "Submitted"
        ));
        loan4.addStatusChange(new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                now.minus(2, ChronoUnit.DAYS),
                "Docs verified"
        ));
        loan4.addStatusChange(new StatusChangeEntity(
                LoanStatus.DOCUMENTS_CHECKED,
                LoanStatus.SCORED,
                now.minus(1, ChronoUnit.DAYS),
                "Scored"
        ));
        loan4.addStatusChange(new StatusChangeEntity(
                LoanStatus.SCORED,
                LoanStatus.APPROVED,
                now.minus(2, ChronoUnit.HOURS),
                "Approved by Carol and Dan"
        ));
        loanApplicationRepository.save(loan4);

        // 5. REPAYING: Business loan (EUR 60,000)
        LoanApplicationEntity loan5 = new LoanApplicationEntity(
                "alice",
                new BigDecimal("60000.00"),
                LoanType.BUSINESS
        );
        loan5.setStatus(LoanStatus.REPAYING);
        loan5.addDocument(new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_biz.pdf",
                DocumentStatus.VERIFIED,
                now.plus(180, ChronoUnit.DAYS)
        ));
        loan5.addDocument(new DocumentEntity(
                DocumentType.FINANCIAL_STATEMENT,
                "balance_sheet.pdf",
                DocumentStatus.VERIFIED,
                now.plus(90, ChronoUnit.DAYS)
        ));
        loan5.addDocument(new DocumentEntity(
                DocumentType.TAX_RECORD,
                "tax_biz.pdf",
                DocumentStatus.VERIFIED,
                now.plus(90, ChronoUnit.DAYS)
        ));
        loan5.addDocument(new DocumentEntity(
                DocumentType.COLLATERAL_PROOF,
                "collateral_biz.pdf",
                DocumentStatus.VERIFIED,
                now.plus(365, ChronoUnit.DAYS)
        ));
        loan5.addApproval(new ApprovalRecordEntity("carol", 1, now.minus(6, ChronoUnit.DAYS)));
        loan5.addApproval(new ApprovalRecordEntity("dan", 2, now.minus(5, ChronoUnit.DAYS)));
        loan5.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(10, ChronoUnit.DAYS),
                "Submitted"
        ));
        loan5.addStatusChange(new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                now.minus(8, ChronoUnit.DAYS),
                "Docs verified"
        ));
        loan5.addStatusChange(new StatusChangeEntity(
                LoanStatus.DOCUMENTS_CHECKED,
                LoanStatus.SCORED,
                now.minus(7, ChronoUnit.DAYS),
                "Scored"
        ));
        loan5.addStatusChange(new StatusChangeEntity(
                LoanStatus.SCORED,
                LoanStatus.APPROVED,
                now.minus(5, ChronoUnit.DAYS),
                "Approved"
        ));
        loan5.addStatusChange(new StatusChangeEntity(
                LoanStatus.APPROVED,
                LoanStatus.DISBURSED,
                now.minus(3, ChronoUnit.DAYS),
                "Disbursed"
        ));
        loan5.addStatusChange(new StatusChangeEntity(
                LoanStatus.DISBURSED,
                LoanStatus.REPAYING,
                now.minus(3, ChronoUnit.DAYS),
                "Repayment schedule active"
        ));
        loanApplicationRepository.save(loan5);

        // 6. REJECTED: Personal loan (EUR 50,000)
        LoanApplicationEntity loan6 = new LoanApplicationEntity(
                "alice",
                new BigDecimal("50000.00"),
                LoanType.PERSONAL
        );
        loan6.setStatus(LoanStatus.REJECTED);
        loan6.addDocument(new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_rej.pdf",
                DocumentStatus.VERIFIED,
                now.plus(180, ChronoUnit.DAYS)
        ));
        loan6.addDocument(new DocumentEntity(
                DocumentType.PROOF_OF_INCOME,
                "income_rej.pdf",
                DocumentStatus.VERIFIED,
                now.plus(60, ChronoUnit.DAYS)
        ));
        loan6.addStatusChange(new StatusChangeEntity(
                null,
                LoanStatus.SUBMITTED,
                now.minus(4, ChronoUnit.DAYS),
                "Submitted"
        ));
        loan6.addStatusChange(new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                now.minus(3, ChronoUnit.DAYS),
                "Docs verified"
        ));
        loan6.addStatusChange(new StatusChangeEntity(
                LoanStatus.DOCUMENTS_CHECKED,
                LoanStatus.SCORED,
                now.minus(2, ChronoUnit.DAYS),
                "Scoring evaluated"
        ));
        loan6.addStatusChange(new StatusChangeEntity(
                LoanStatus.SCORED,
                LoanStatus.REJECTED,
                now.minus(2, ChronoUnit.DAYS),
                "High credit risk detected"
        ));
        loanApplicationRepository.save(loan6);
    }
}
