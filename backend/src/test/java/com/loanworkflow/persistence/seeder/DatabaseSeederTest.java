package com.loanworkflow.persistence.seeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;
import com.loanworkflow.persistence.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class DatabaseSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Test
    void seedsUsersAndLoansSuccessfully() {
        DatabaseSeeder seeder = new DatabaseSeeder(userRepository, loanApplicationRepository, true);

        seeder.run();

        assertEquals(4, userRepository.count());
        assertTrue(userRepository.existsByUsername("alice"));
        assertTrue(userRepository.existsByUsername("bob"));
        assertTrue(userRepository.existsByUsername("carol"));
        assertTrue(userRepository.existsByUsername("dan"));

        assertEquals(6, loanApplicationRepository.count());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.SUBMITTED).size());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.DOCUMENTS_CHECKED).size());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.SCORED).size());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.APPROVED).size());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.REPAYING).size());
        assertEquals(1, loanApplicationRepository.findByStatus(LoanStatus.REJECTED).size());
    }

    @Test
    void seederIsIdempotentAndDoesNotDuplicate() {
        DatabaseSeeder seeder = new DatabaseSeeder(userRepository, loanApplicationRepository, true);

        seeder.run();
        long initialLoanCount = loanApplicationRepository.count();
        long initialUserCount = userRepository.count();

        // Run second time
        seeder.run();

        assertEquals(initialLoanCount, loanApplicationRepository.count());
        assertEquals(initialUserCount, userRepository.count());
    }

    @Test
    void doesNotSeedWhenDisabled() {
        DatabaseSeeder seeder = new DatabaseSeeder(userRepository, loanApplicationRepository, false);

        seeder.run();

        assertEquals(0, loanApplicationRepository.count());
        assertEquals(0, userRepository.count());
    }
}
