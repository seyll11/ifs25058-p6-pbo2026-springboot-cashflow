package org.delcom.ifs25058springbootcashflow.usecase;

import org.delcom.ifs25058springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25058springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25058springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiUseCaseTest {

    private TransactionRepository repository;
    private FinanceAiUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        useCase = new FinanceAiUseCase(new InMemoryFinanceAiService(), repository);
    }

    @Test
    void generateTransactionsReturnsList() throws Exception {
        List<AiGeneratedTransaction> result = useCase.generateTransactions("Gaji 1 juta, makan 25 ribu");
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    void generateTransactionsRejectsBlankPrompt() {
        assertThrows(ValidationException.class, () -> useCase.generateTransactions(null));
        assertThrows(ValidationException.class, () -> useCase.generateTransactions("   "));
    }

    @Test
    void saveGeneratedTransactionsPersistsToRepository() throws Exception {
        List<AiGeneratedTransaction> items = List.of(
                new AiGeneratedTransaction("Gaji", 1_000L, TransactionType.INCOME),
                new AiGeneratedTransaction("Makan", 25L, TransactionType.EXPENSE)
        );

        List<Transaction> saved = useCase.saveGeneratedTransactions(1, items);
        assertEquals(2, saved.size());
        assertEquals(2, repository.findByUserId(1).size());
    }

    @Test
    void saveGeneratedTransactionsRejectsEmptyOrNull() {
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, null));
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, Collections.emptyList()));
    }
}
