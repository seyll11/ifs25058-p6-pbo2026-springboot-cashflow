package org.delcom.ifs25058springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionTypeTest {

    @Test
    void labelsAndSigns() {
        assertEquals("Pemasukan", TransactionType.INCOME.label());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.label());
        assertEquals(1, TransactionType.INCOME.sign());
        assertEquals(-1, TransactionType.EXPENSE.sign());
    }
}
