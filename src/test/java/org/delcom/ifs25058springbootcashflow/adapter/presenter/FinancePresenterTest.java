package org.delcom.ifs25058springbootcashflow.adapter.presenter;

import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancePresenterTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 1, 8, 5);

    private FinancePresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new FinancePresenter();
    }

    private static Transaction tx(int id, String description, long amount, TransactionType type) {
        return new Transaction(id, 1, description, amount, type, null, CREATED);
    }

    @Test
    void formatMoneyUsesThousandsSeparatorAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1_000_000L));
        assertEquals("Rp 4,975,000.00", presenter.formatMoney(4_975_000L));
        assertEquals("Rp 0.00", presenter.formatMoney(0L));
        assertEquals("Rp 500.00", presenter.formatMoney(500L));
        assertEquals("-Rp 1,500.00", presenter.formatMoney(-1_500L));
    }

    @Test
    void formatBalance() {
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4_975_000L));
        assertEquals("Saldo: -Rp 100.00", presenter.formatBalance(-100L));
    }

    @Test
    void formatTransactionLine() {
        assertEquals(
                "[1] Gaji | Rp 1,000,000.00 | [Pemasukan]",
                presenter.format(tx(1, "Gaji", 1_000_000L, TransactionType.INCOME)));
        assertEquals(
                "[2] Makan | Rp 25,000.00 | [Pengeluaran]",
                presenter.format(tx(2, "Makan", 25_000L, TransactionType.EXPENSE)));
    }

    @Test
    void formatCreatedAt() {
        assertEquals("01/11/2026 08:05", presenter.formatCreatedAt(tx(1, "A", 1L, TransactionType.INCOME)));
    }

    @Test
    void formatListEmptyAndFilled() {
        assertEquals("Header:\n- kosong", presenter.formatList(List.of(), "Header:", "- kosong"));

        String filled = presenter.formatList(
                List.of(tx(1, "A", 10L, TransactionType.INCOME), tx(2, "B", 20L, TransactionType.EXPENSE)),
                "Header:",
                "- kosong");
        assertTrue(filled.startsWith("Header:\n[1] A"));
        assertTrue(filled.contains("\n[2] B | Rp 20.00 | [Pengeluaran]"));
    }

    @Test
    void showMessages() {
        Transaction transaction = tx(1, "Belanja", 100L, TransactionType.EXPENSE);
        assertTrue(presenter.showTransactions(List.of()).contains("belum tersedia"));
        assertTrue(presenter.showTransactions(List.of(transaction)).contains("Belanja"));
        assertTrue(presenter.showSearchResults(List.of(), "x").contains("tidak ditemukan"));
        assertTrue(presenter.showSearchResults(List.of(transaction), "Belanja").contains("Hasil Pencarian"));
        assertTrue(presenter.showAddSuccess(transaction).startsWith("Berhasil mencatat transaksi: [1] Belanja"));
        assertEquals("Berhasil menambahkan 3 transaksi dari AI", presenter.showAiSaved(3));
        assertEquals("Berhasil menghapus transaksi.", presenter.showRemoveSuccess());
        assertEquals("[!] Gagal menghapus transaksi dengan ID: 9.", presenter.showRemoveFailed(9));
        assertEquals("Berhasil mengubah transaksi.", presenter.showUpdateSuccess());
        assertEquals("[!] Gagal mengubah transaksi dengan ID: 8.", presenter.showUpdateFailed(8));
        assertEquals("[!] ID tidak valid!", presenter.showInvalidId());
        assertEquals("[!] keterangan kosong", presenter.showValidationError("keterangan kosong"));
    }
}
