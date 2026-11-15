package com.loanworkflow.persistence.adapter;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.loanworkflow.approval.ApprovalRequest;
import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.loan.LoanApplication;
import com.loanworkflow.loan.LoanRepository;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.StatusChange;
import com.loanworkflow.persistence.entity.ApprovalRecordEntity;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.entity.StatusChangeEntity;
import com.loanworkflow.persistence.entity.UserEntity;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;
import com.loanworkflow.persistence.repository.UserRepository;
import com.loanworkflow.scoring.RuleBasedScoringStrategy;
import com.loanworkflow.scoring.ScoringStrategy;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter implementation of {@link LoanRepository}.
 * Bridges domain {@link LoanService} instances with relational database entities
 * persisted via {@link LoanApplicationRepository}.
 */
@Repository
@Primary
public class JpaLoanRepositoryAdapter implements LoanRepository {

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ScoringStrategy scoringStrategy;
    private final Map<String, LoanService> activeLoans = new ConcurrentHashMap<>();

    public JpaLoanRepositoryAdapter(
            LoanApplicationRepository loanApplicationRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.scoringStrategy = new RuleBasedScoringStrategy();
    }

    @Override
    @Transactional
    public String save(LoanService loan) {
        String id = loan.id();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            loan.setId(id);
        }

        ensureUserExists(loan.applicant());

        LoanApplicationEntity entity = loanApplicationRepository.findById(id)
                .orElseGet(() -> {
                    LoanApplicationEntity newEntity = new LoanApplicationEntity(
                            loan.applicant().name(),
                            loan.amount()
                    );
                    newEntity.setId(loan.id());
                    return newEntity;
                });

        entity.setAmount(loan.amount());
        entity.setStatus(loan.loan().status());

        if (loan.approval() != null) {
            int seq = entity.getApprovals().size() + 1;
            for (String approverName : loan.approval().approvedBy()) {
                boolean alreadyRecorded = entity.getApprovals().stream()
                        .anyMatch(a -> approverName.equals(a.getApproverName()));
                if (!alreadyRecorded) {
                    ApprovalRecordEntity record = new ApprovalRecordEntity(
                            approverName,
                            seq++,
                            clock.instant()
                    );
                    entity.addApproval(record);
                }
            }
        }

        List<StatusChange> domainHistory = loan.loan().history();
        int existingCount = entity.getStatusHistory().size();
        if (domainHistory.size() > existingCount) {
            for (int i = existingCount; i < domainHistory.size(); i++) {
                StatusChange change = domainHistory.get(i);
                StatusChangeEntity changeEntity = new StatusChangeEntity(
                        change.from(),
                        change.to(),
                        change.at(),
                        null
                );
                entity.addStatusChange(changeEntity);
            }
        }

        loanApplicationRepository.save(entity);
        activeLoans.put(id, loan);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanService> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        LoanService cached = activeLoans.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }

        return loanApplicationRepository.findById(id).map(this::reconstructLoanService);
    }

    /**
     * Clears the in-memory identity map cache.
     */
    public void clearCache() {
        activeLoans.clear();
    }

    private LoanService reconstructLoanService(LoanApplicationEntity entity) {
        User applicant = new User(entity.getApplicantUsername(), Role.APPLICANT);
        ApprovalRequest approval = new ApprovalRequest(applicant, entity.getAmount());

        if (entity.getApprovals() != null) {
            for (ApprovalRecordEntity record : entity.getApprovals()) {
                approval.restoreApprover(record.getApproverName());
            }
        }

        List<StatusChange> history = new ArrayList<>();
        if (entity.getStatusHistory() != null) {
            for (StatusChangeEntity changeEntity : entity.getStatusHistory()) {
                history.add(new StatusChange(
                        changeEntity.getFromStatus(),
                        changeEntity.getToStatus(),
                        changeEntity.getChangedAt()
                ));
            }
        }

        LoanApplication loanApp = new LoanApplication(clock, entity.getStatus(), history);
        LoanService loanService = new LoanService(
                loanApp,
                approval,
                entity.getAmount(),
                scoringStrategy,
                clock
        );
        loanService.setId(entity.getId());
        activeLoans.put(entity.getId(), loanService);
        return loanService;
    }

    private void ensureUserExists(User user) {
        if (user == null || user.name() == null || user.name().isBlank()) {
            return;
        }
        if (!userRepository.existsByUsername(user.name())) {
            UserEntity userEntity = new UserEntity(
                    user.name(),
                    "hash_" + user.name(),
                    user.role() != null ? user.role() : Role.APPLICANT
            );
            userRepository.save(userEntity);
        }
    }
}
