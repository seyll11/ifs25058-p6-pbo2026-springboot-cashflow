package org.delcom.ifs25058springbootcashflow.adapter.presenter;

import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Presenter yang memformat data keuangan menjadi teks siap tampil.
 * Format uang bukan tanggung jawab entity: dipusatkan di sini agar notifikasi,
 * kartu ringkasan, dan kolom grid selalu konsisten (contoh: Rp 1,000,000.00).
 */
public class FinancePresenter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Memformat jumlah uang dengan pemisah ribuan koma dan dua digit desimal. */
    public String formatMoney(long amount) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        String body = "Rp " + decimalFormat.format(Math.abs(amount));
        return amount < 0 ? "-" + body : body;
    }

    /** Memformat saldo: {@code Saldo: Rp 4,975,000.00}. */
    public String formatBalance(long balance) {
        return "Saldo: " + formatMoney(balance);
    }

    /** Memformat satu transaksi: {@code [id] keterangan | Rp 1,000,000.00 | [Pemasukan]}. */
    public String format(Transaction transaction) {
        return String.format(
                "[%d] %s | %s | [%s]",
                transaction.getId(),
                transaction.getDescription(),
                formatMoney(transaction.getAmount()),
                transaction.getType().label()
        );
    }

    /** Memformat waktu transaksi dicatat. */
    public String formatCreatedAt(Transaction transaction) {
        return transaction.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    /** Memformat daftar transaksi menjadi teks multi-baris; pesan kosong jika tidak ada data. */
    public String formatList(List<Transaction> transactions, String header, String emptyMessage) {
        StringBuilder builder = new StringBuilder(header);
        if (transactions.isEmpty()) {
            builder.append('\n').append(emptyMessage);
            return builder.toString();
        }
        String body = transactions.stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
        builder.append('\n').append(body);
        return builder.toString();
    }

    /** Memformat daftar semua transaksi. */
    public String showTransactions(List<Transaction> transactions) {
        return formatList(transactions, "Daftar Transaksi:", "- Data transaksi belum tersedia!");
    }

    /** Memformat hasil pencarian berdasarkan kata kunci. */
    public String showSearchResults(List<Transaction> transactions, String keyword) {
        return formatList(transactions, "Hasil Pencarian: \"" + keyword + "\"", "- Transaksi tidak ditemukan!");
    }

    /** Pesan sukses setelah menambah transaksi. */
    public String showAddSuccess(Transaction transaction) {
        return "Berhasil mencatat transaksi: " + format(transaction);
    }

    /** Pesan sukses setelah menambah transaksi dari AI. */
    public String showAiSaved(int count) {
        return "Berhasil menambahkan " + count + " transaksi dari AI";
    }

    /** Pesan sukses setelah menghapus transaksi. */
    public String showRemoveSuccess() {
        return "Berhasil menghapus transaksi.";
    }

    /** Pesan gagal saat menghapus transaksi. */
    public String showRemoveFailed(int id) {
        return String.format("[!] Gagal menghapus transaksi dengan ID: %d.", id);
    }

    /** Pesan sukses setelah mengubah transaksi. */
    public String showUpdateSuccess() {
        return "Berhasil mengubah transaksi.";
    }

    /** Pesan gagal saat mengubah transaksi. */
    public String showUpdateFailed(int id) {
        return String.format("[!] Gagal mengubah transaksi dengan ID: %d.", id);
    }

    /** Pesan saat ID yang dimasukkan tidak valid. */
    public String showInvalidId() {
        return "[!] ID tidak valid!";
    }

    /** Pesan validasi umum dari domain. */
    public String showValidationError(String message) {
        return "[!] " + message;
    }
}
