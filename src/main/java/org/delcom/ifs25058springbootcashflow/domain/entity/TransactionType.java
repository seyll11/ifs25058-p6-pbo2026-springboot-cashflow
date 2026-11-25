package org.delcom.ifs25058springbootcashflow.domain.entity;

/**
 * Jenis transaksi keuangan: pemasukan menambah saldo, pengeluaran menguranginya.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    /** Label singkat untuk ditampilkan di UI. */
    public String label() {
        return switch (this) {
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }

    /** Pengali saldo: +1 untuk pemasukan, -1 untuk pengeluaran. */
    public int sign() {
        return this == INCOME ? 1 : -1;
    }
}
