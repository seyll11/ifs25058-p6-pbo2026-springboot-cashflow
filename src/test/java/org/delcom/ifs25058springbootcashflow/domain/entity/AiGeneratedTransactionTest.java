package org.delcom.ifs25058springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGeneratedTransactionTest {

    @Test
    void trimsDescription() {
        AiGeneratedTransaction item = new AiGeneratedTransaction("  Gaji  ", 100L, TransactionType.INCOME);
        assertEquals("Gaji", item.description());
        assertEquals(100L, item.amount());
        assertEquals(TransactionType.INCOME, item.type());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction(null, 1L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("  ", 1L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("A", 0L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("A", -5L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("A", 1L, null));
    }
}
