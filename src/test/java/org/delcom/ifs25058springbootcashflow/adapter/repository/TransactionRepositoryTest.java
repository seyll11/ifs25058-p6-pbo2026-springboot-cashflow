package org.delcom.ifs25058springbootcashflow.adapter.repository;

import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
    }

    @Test
    void saveAssignsIncrementalIds() {
        Transaction first = repository.save(1, "Satu", 10L, TransactionType.INCOME);
        Transaction second = repository.save(1, "Dua", 20L, TransactionType.EXPENSE);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(2, repository.findAll().size());
        assertEquals(2, repository.findByUserId(1).size());
        assertTrue(repository.findByUserId(2).isEmpty());
    }

    @Test
    void findByIdAndDelete() {
        Transaction created = repository.save(1, "Hapus saya", 5L, TransactionType.EXPENSE);

        assertTrue(repository.findById(created.getId()).isPresent());
        assertTrue(repository.deleteById(created.getId()));
        assertFalse(repository.deleteById(created.getId()));
        assertTrue(repository.findById(created.getId()).isEmpty());
    }

    @Test
    void findByPredicateAndUpdateNoOp() {
        repository.save(1, "Gaji", 100L, TransactionType.INCOME);
        repository.save(1, "Makan", 10L, TransactionType.EXPENSE);

        List<Transaction> expenses = repository.findBy(item -> item.getType() == TransactionType.EXPENSE);
        assertEquals(1, expenses.size());

        Transaction item = expenses.get(0);
        item.changeDescription("Makan malam");
        repository.update(item);
        assertEquals("Makan malam", repository.findById(item.getId()).orElseThrow().getDescription());
    }

    @Test
    void findAllReturnsDefensiveCopy() {
        repository.save(1, "A", 1L, TransactionType.INCOME);
        List<Transaction> all = repository.findAll();
        all.clear();
        assertEquals(1, repository.findAll().size());
    }
}
