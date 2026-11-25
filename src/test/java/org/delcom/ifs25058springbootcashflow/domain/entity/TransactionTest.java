package org.delcom.ifs25058springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    @Test
    void shortConstructorSetsDefaults() {
        Transaction transaction = new Transaction(1, 10, "Gaji", 5_000_000L, TransactionType.INCOME);

        assertEquals(1, transaction.getId());
        assertEquals(10, transaction.getUserId());
        assertEquals("Gaji", transaction.getDescription());
        assertEquals(5_000_000L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertNull(transaction.getProofPath());
        assertNotNull(transaction.getCreatedAt());
        assertTrue(transaction.isIncome());
    }

    @Test
    void fullConstructorHandlesNullDescription() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 1, 8, 0);
        Transaction transaction = new Transaction(2, 10, null, 100L, TransactionType.EXPENSE, "proofs/a.png", created);

        assertEquals("", transaction.getDescription());
        assertEquals("proofs/a.png", transaction.getProofPath());
        assertEquals(created, transaction.getCreatedAt());
        assertFalse(transaction.isIncome());
    }

    @Test
    void signedAmountDependsOnType() {
        Transaction income = new Transaction(1, 1, "A", 500L, TransactionType.INCOME);
        Transaction expense = new Transaction(2, 1, "B", 500L, TransactionType.EXPENSE);
        assertEquals(500L, income.signedAmount());
        assertEquals(-500L, expense.signedAmount());
    }

    @Test
    void changeFields() {
        Transaction transaction = new Transaction(3, 1, "Lama", 100L, TransactionType.EXPENSE);

        transaction.changeDescription("Baru");
        transaction.changeAmount(250L);
        transaction.changeType(TransactionType.INCOME);
        transaction.changeProofPath("proofs/x.png");

        assertEquals("Baru", transaction.getDescription());
        assertEquals(250L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals("proofs/x.png", transaction.getProofPath());

        transaction.changeDescription(null);
        assertEquals("", transaction.getDescription());
    }
}
