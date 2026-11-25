package org.delcom.ifs25058springbootcashflow.usecase;

import org.delcom.ifs25058springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25058springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25058springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceUseCaseTest {

    private static final int USER_A = 1;
    private static final int USER_B = 2;

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinanceUseCase(new TransactionRepository());
    }

    @Test
    void addAndGetTransactionsByUser() throws ValidationException {
        Transaction created = useCase.addTransaction(USER_A, "  Gaji  ", 5_000_000L, TransactionType.INCOME);
        assertEquals(1, created.getId());
        assertEquals("Gaji", created.getDescription());
        assertEquals(1, useCase.getTransactionsByUser(USER_A).size());
        assertTrue(useCase.getTransactionsByUser(USER_B).isEmpty());
        assertEquals(1, useCase.getAllTransactions().size());
    }

    @Test
    void addTransactionValidation() {
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, " ", 10L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, null, 10L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "A", 0L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "A", -1L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "A", 10L, null));
    }

    @Test
    void parseAmountAcceptsNumbersAndRejectsInvalid() throws ValidationException {
        assertEquals(1_000_000L, useCase.parseAmount("1000000"));
        assertEquals(1_500_000L, useCase.parseAmount(" 1,500,000 "));
        assertEquals(2500L, useCase.parseAmount("2 500"));

        assertThrows(ValidationException.class, () -> useCase.parseAmount(null));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("   "));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("abc"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("12.5"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("0"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("-100"));
    }

    @Test
    void addTransactionFromTextAmount() throws ValidationException {
        Transaction created = useCase.addTransaction(USER_A, "Makan", "25000", TransactionType.EXPENSE);
        assertEquals(25_000L, created.getAmount());
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "Makan", "x", TransactionType.EXPENSE));
    }

    @Test
    void balanceAndTotals() throws ValidationException {
        useCase.addTransaction(USER_A, "Gaji", 5_000_000L, TransactionType.INCOME);
        useCase.addTransaction(USER_A, "Kos", 1_200_000L, TransactionType.EXPENSE);
        useCase.addTransaction(USER_A, "Makan", 25_000L, TransactionType.EXPENSE);
        useCase.addTransaction(USER_B, "Lain", 999L, TransactionType.INCOME);

        assertEquals(5_000_000L, useCase.getTotalIncome(USER_A));
        assertEquals(1_225_000L, useCase.getTotalExpense(USER_A));
        assertEquals(3_775_000L, useCase.getBalance(USER_A));
        assertEquals(999L, useCase.getBalance(USER_B));
        assertEquals(0L, useCase.getBalance(99));
    }

    @Test
    void balanceCanBeNegative() throws ValidationException {
        useCase.addTransaction(USER_A, "Belanja", 100L, TransactionType.EXPENSE);
        assertEquals(-100L, useCase.getBalance(USER_A));
    }

    @Test
    void removeTransactionSuccessAndErrors() throws Exception {
        Transaction created = useCase.addTransaction(USER_A, "Hapus", 10L, TransactionType.EXPENSE);
        useCase.removeTransactionForUser(USER_A, created.getId());
        assertTrue(useCase.getTransactionsByUser(USER_A).isEmpty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(99));
        assertEquals(99, ex.getId());
        assertThrows(ValidationException.class, () -> useCase.removeTransaction(0));
        assertThrows(ValidationException.class, () -> useCase.removeTransaction(-3));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(USER_A, 99));
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(USER_A, 0));
    }

    @Test
    void removeRejectsOtherOwner() throws Exception {
        Transaction created = useCase.addTransaction(USER_A, "Milik A", 10L, TransactionType.INCOME);
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(USER_B, created.getId()));
        assertEquals(1, useCase.getTransactionsByUser(USER_A).size());
    }

    @Test
    void updateTransactionPartialAndValidation() throws Exception {
        Transaction created = useCase.addTransaction(USER_A, "Lama", 100L, TransactionType.EXPENSE);
        int id = created.getId();

        useCase.updateTransactionForUser(USER_A, id, "Baru", 200L, TransactionType.INCOME, "proofs/a.png", false);
        Transaction updated = useCase.getTransactionsByUser(USER_A).get(0);
        assertEquals("Baru", updated.getDescription());
        assertEquals(200L, updated.getAmount());
        assertEquals(TransactionType.INCOME, updated.getType());
        assertEquals("proofs/a.png", updated.getProofPath());

        useCase.updateTransactionForUser(USER_A, id, null, null, null, null, true);
        assertNull(useCase.getTransactionsByUser(USER_A).get(0).getProofPath());

        useCase.updateTransactionForUser(USER_A, id, null, null, null, null, false);
        assertEquals("Baru", useCase.getTransactionsByUser(USER_A).get(0).getDescription());

        assertThrows(ValidationException.class, () ->
                useCase.updateTransactionForUser(USER_A, id, "  ", null, null, null, false));
        assertThrows(ValidationException.class, () ->
                useCase.updateTransactionForUser(USER_A, id, null, 0L, null, null, false));
        assertThrows(ValidationException.class, () ->
                useCase.updateTransactionForUser(USER_B, id, "X", null, null, null, false));
        assertThrows(EntityNotFoundException.class, () ->
                useCase.updateTransactionForUser(USER_A, 999, "X", null, null, null, false));
        assertThrows(ValidationException.class, () ->
                useCase.updateTransactionForUser(USER_A, 0, "X", null, null, null, false));
    }

    @Test
    void listTransactionsFiltersSearchesAndDefaultsToNewest() throws Exception {
        Transaction first = useCase.addTransaction(USER_A, "Gaji bulanan", 5_000L, TransactionType.INCOME);
        Transaction second = useCase.addTransaction(USER_A, "Makan siang", 25L, TransactionType.EXPENSE);

        List<Transaction> newest = useCase.getTransactionsByUser(USER_A);
        assertEquals(second.getId(), newest.get(0).getId());

        List<Transaction> incomes = useCase.listTransactions(USER_A, null, TransactionTypeFilter.INCOME, SortOption.NEWEST_FIRST);
        assertEquals(1, incomes.size());
        assertEquals(first.getId(), incomes.get(0).getId());

        List<Transaction> expenses = useCase.listTransactions(USER_A, "", TransactionTypeFilter.EXPENSE, SortOption.AMOUNT_DESC);
        assertEquals(1, expenses.size());

        List<Transaction> keyword = useCase.listTransactions(USER_A, " MAKAN ", TransactionTypeFilter.ALL, SortOption.DESCRIPTION_ASC);
        assertEquals(1, keyword.size());
        assertEquals("Makan siang", keyword.get(0).getDescription());

        assertTrue(useCase.listTransactions(USER_A, "tidakada", TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST).isEmpty());

        List<Transaction> defaults = useCase.listTransactions(USER_A, null, null, null);
        assertEquals(2, defaults.size());
        assertEquals(second.getId(), defaults.get(0).getId());
    }
}
