package org.delcom.ifs25058springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortOptionTest {

    private static Transaction tx(int id, String description, long amount, TransactionType type, LocalDateTime at) {
        return new Transaction(id, 1, description, amount, type, null, at);
    }

    @Test
    void sortsByNewestAndOldest() {
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 1, 2, 8, 0);
        List<Transaction> items = new ArrayList<>();
        items.add(tx(1, "Lama", 1, TransactionType.INCOME, older));
        items.add(tx(2, "Baru", 1, TransactionType.INCOME, newer));

        items.sort(SortOption.NEWEST_FIRST.comparator());
        assertEquals("Baru", items.get(0).getDescription());

        items.sort(SortOption.OLDEST_FIRST.comparator());
        assertEquals("Lama", items.get(0).getDescription());
    }

    @Test
    void sortsByAmount() {
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 8, 0);
        List<Transaction> items = new ArrayList<>();
        items.add(tx(1, "Kecil", 10, TransactionType.INCOME, at));
        items.add(tx(2, "Besar", 1000, TransactionType.EXPENSE, at));

        items.sort(SortOption.AMOUNT_DESC.comparator());
        assertEquals("Besar", items.get(0).getDescription());

        items.sort(SortOption.AMOUNT_ASC.comparator());
        assertEquals("Kecil", items.get(0).getDescription());
    }

    @Test
    void sortsByDescription() {
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 8, 0);
        List<Transaction> items = new ArrayList<>();
        items.add(tx(1, "beta", 1, TransactionType.INCOME, at));
        items.add(tx(2, "Alpha", 1, TransactionType.INCOME, at));

        items.sort(SortOption.DESCRIPTION_ASC.comparator());
        assertEquals("Alpha", items.get(0).getDescription());

        items.sort(SortOption.DESCRIPTION_DESC.comparator());
        assertEquals("beta", items.get(0).getDescription());
    }

    @Test
    void sortsByTypeFirst() {
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 8, 0);
        List<Transaction> items = new ArrayList<>();
        items.add(tx(1, "Keluar", 1, TransactionType.EXPENSE, at));
        items.add(tx(2, "Masuk", 1, TransactionType.INCOME, at));

        items.sort(SortOption.INCOME_FIRST.comparator());
        assertTrue(items.get(0).isIncome());

        items.sort(SortOption.EXPENSE_FIRST.comparator());
        assertEquals(TransactionType.EXPENSE, items.get(0).getType());
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Terbaru", SortOption.NEWEST_FIRST.label());
        assertEquals("Terlama", SortOption.OLDEST_FIRST.label());
        assertEquals("Jumlah terbesar", SortOption.AMOUNT_DESC.label());
        assertEquals("Jumlah terkecil", SortOption.AMOUNT_ASC.label());
        assertEquals("Keterangan A-Z", SortOption.DESCRIPTION_ASC.label());
        assertEquals("Keterangan Z-A", SortOption.DESCRIPTION_DESC.label());
        assertEquals("Pemasukan dulu", SortOption.INCOME_FIRST.label());
        assertEquals("Pengeluaran dulu", SortOption.EXPENSE_FIRST.label());
    }
}
