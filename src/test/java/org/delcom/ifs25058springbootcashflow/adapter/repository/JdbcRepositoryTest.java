package org.delcom.ifs25058springbootcashflow.adapter.repository;

import org.delcom.ifs25058springbootcashflow.domain.entity.Role;
import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.entity.User;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRepositoryTest {

    private JdbcUserRepository userRepository;
    private JdbcTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        SchemaInitializer.initialize(dataSource);
        userRepository = new JdbcUserRepository(dataSource);
        transactionRepository = new JdbcTransactionRepository(dataSource);
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jdbc_repo_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Test
    void userCrudAndFinders() {
        User saved = userRepository.saveNew("andi", "hash", "Andi", Role.USER);
        assertTrue(saved.getId() > 0);

        assertTrue(userRepository.findByUsername("ANDI").isPresent());
        assertEquals(1, userRepository.findAll().size());
        assertTrue(userRepository.findById(saved.getId()).isPresent());
        assertEquals(1, userRepository.findBy(user -> user.getUsername().equals("andi")).size());

        saved.changeFullName("Andi Baru");
        saved.changePhotoPath("profiles/a.png");
        userRepository.update(saved);
        assertEquals("Andi Baru", userRepository.findById(saved.getId()).orElseThrow().getFullName());

        User viaSave = userRepository.save(new User(0, "budi", "h", "Budi", Role.USER, null));
        assertTrue(viaSave.getId() > 0);

        assertTrue(userRepository.deleteById(viaSave.getId()));
        assertFalse(userRepository.deleteById(viaSave.getId()));
        assertTrue(userRepository.findByUsername("tidakada").isEmpty());
        assertTrue(userRepository.findById(999).isEmpty());
    }

    @Test
    void transactionCrudAndFinders() {
        User owner = userRepository.saveNew("owner", "hash", "Owner", Role.USER);
        Transaction created = transactionRepository.save(owner.getId(), "Gaji", 5_000_000L, TransactionType.INCOME);
        assertEquals(5_000_000L, created.getAmount());
        assertEquals(TransactionType.INCOME, created.getType());
        assertNull(created.getProofPath());

        Transaction viaEntity = transactionRepository.save(
                new Transaction(0, owner.getId(), "Makan", 25_000L, TransactionType.EXPENSE));
        assertTrue(viaEntity.getId() > 0);

        assertEquals(2, transactionRepository.findAll().size());
        assertEquals(2, transactionRepository.findByUserId(owner.getId()).size());
        assertTrue(transactionRepository.findByUserId(999).isEmpty());
        assertTrue(transactionRepository.findById(created.getId()).isPresent());
        assertTrue(transactionRepository.findById(999).isEmpty());
        assertEquals(1, transactionRepository.findBy(item -> item.getDescription().equals("Gaji")).size());

        created.changeDescription("Gaji Baru");
        created.changeAmount(6_000_000L);
        created.changeType(TransactionType.EXPENSE);
        created.changeProofPath("proofs/x.png");
        transactionRepository.update(created);
        Transaction updated = transactionRepository.findById(created.getId()).orElseThrow();
        assertEquals("Gaji Baru", updated.getDescription());
        assertEquals(6_000_000L, updated.getAmount());
        assertEquals(TransactionType.EXPENSE, updated.getType());
        assertEquals("proofs/x.png", updated.getProofPath());

        assertTrue(transactionRepository.deleteById(created.getId()));
        assertFalse(transactionRepository.deleteById(created.getId()));
        List<Transaction> remaining = transactionRepository.findByUserId(owner.getId());
        assertEquals(1, remaining.size());
    }

    @Test
    void transactionsAreOrderedNewestFirst() {
        User owner = userRepository.saveNew("urut", "hash", "Urut", Role.USER);
        Transaction first = transactionRepository.save(owner.getId(), "Pertama", 1L, TransactionType.INCOME);
        Transaction second = transactionRepository.save(owner.getId(), "Kedua", 2L, TransactionType.INCOME);

        List<Transaction> all = transactionRepository.findByUserId(owner.getId());
        assertEquals(second.getId(), all.get(0).getId());
        assertEquals(first.getId(), all.get(1).getId());
    }
}
