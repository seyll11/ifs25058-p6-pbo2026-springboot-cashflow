package org.delcom.ifs25058springbootcashflow.usecase;

import org.delcom.ifs25058springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25058springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25058springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Use case logika bisnis catatan keuangan.
 * Tidak melakukan I/O UI; hanya memproses data dan melempar checked exception domain.
 */
public class FinanceUseCase {
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Mengambil semua transaksi satu pengguna, terbaru di atas. */
    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    /**
     * Mengambil transaksi pengguna dengan pencarian keterangan, filter tipe, dan urutan.
     * Keyword kosong/null diabaikan. Filter/sort null memakai default (semua + terbaru).
     */
    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter typeFilter,
            SortOption sortOption
    ) {
        TransactionTypeFilter filter = typeFilter == null ? TransactionTypeFilter.ALL : typeFilter;
        SortOption sort = sortOption == null ? SortOption.NEWEST_FIRST : sortOption;

        Stream<Transaction> stream = transactionRepository.findByUserId(userId).stream()
                .filter(filter::matches);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            stream = stream.filter(item -> item.getDescription().toLowerCase().contains(lowerKeyword));
        }

        return stream.sorted(sort.comparator()).toList();
    }

    /** Mengambil semua transaksi yang tersedia (admin). */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /** Total pemasukan milik satu pengguna. */
    public long getTotalIncome(int userId) {
        return sumByType(userId, TransactionType.INCOME);
    }

    /** Total pengeluaran milik satu pengguna. */
    public long getTotalExpense(int userId) {
        return sumByType(userId, TransactionType.EXPENSE);
    }

    /** Saldo = total pemasukan dikurangi total pengeluaran. */
    public long getBalance(int userId) {
        return getTotalIncome(userId) - getTotalExpense(userId);
    }

    /** Mengubah teks jumlah menjadi angka positif; melempar ValidationException jika tidak valid. */
    public long parseAmount(String amountText) throws ValidationException {
        if (amountText == null || amountText.isBlank()) {
            throw new ValidationException("Jumlah tidak boleh kosong");
        }
        String cleaned = amountText.trim().replace(",", "").replace(" ", "");
        long value;
        try {
            value = Long.parseLong(cleaned);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Jumlah harus berupa angka bulat");
        }
        validateAmount(value);
        return value;
    }

    /** Menambahkan transaksi baru dari jumlah berupa teks (input form). */
    public Transaction addTransaction(int userId, String description, String amountText, TransactionType type)
            throws ValidationException {
        return addTransaction(userId, description, parseAmount(amountText), type);
    }

    /** Menambahkan transaksi baru setelah validasi. */
    public Transaction addTransaction(int userId, String description, long amount, TransactionType type)
            throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
        validateAmount(amount);
        if (type == null) {
            throw new ValidationException("Tipe transaksi harus dipilih");
        }
        return transactionRepository.save(userId, description.trim(), amount, type);
    }

    /** Menghapus transaksi berdasarkan ID; melempar exception jika tidak valid/tidak ditemukan. */
    public void removeTransaction(int id) throws EntityNotFoundException, ValidationException {
        validateId(id);
        if (!transactionRepository.deleteById(id)) {
            throw new EntityNotFoundException(id);
        }
    }

    /** Menghapus transaksi milik pengguna tertentu. */
    public void removeTransactionForUser(int userId, int id) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        removeTransaction(transaction.getId());
    }

    /**
     * Mengubah keterangan, jumlah, tipe, dan/atau bukti transaksi milik pengguna.
     * Parameter {@code null} berarti field tidak diubah (kecuali {@code clearProof=true}).
     */
    public void updateTransactionForUser(
            int userId,
            int id,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        if (description != null) {
            if (description.trim().isEmpty()) {
                throw new ValidationException("Keterangan tidak boleh kosong");
            }
            transaction.changeDescription(description.trim());
        }
        if (amount != null) {
            validateAmount(amount);
            transaction.changeAmount(amount);
        }
        if (type != null) {
            transaction.changeType(type);
        }
        if (clearProof) {
            transaction.changeProofPath(null);
        } else if (proofPath != null) {
            transaction.changeProofPath(proofPath);
        }
        transactionRepository.update(transaction);
    }

    private long sumByType(int userId, TransactionType type) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(item -> item.getType() == type)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    private Transaction requireOwnedTransaction(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        validateId(id);
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction transaction = found.get();
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        return transaction;
    }

    private static void validateId(int id) throws ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID transaksi tidak valid");
        }
    }

    private static void validateAmount(long amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari nol");
        }
    }
}
