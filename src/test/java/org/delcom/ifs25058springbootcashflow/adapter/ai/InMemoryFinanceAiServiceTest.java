package org.delcom.ifs25058springbootcashflow.adapter.ai;

import org.delcom.ifs25058springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryFinanceAiServiceTest {

    @Test
    void generatesDefaultTransactions() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        List<AiGeneratedTransaction> items = service.generateTransactions("Catatan");
        assertEquals(2, items.size());
        assertEquals(TransactionType.INCOME, items.get(0).type());
        assertEquals(TransactionType.EXPENSE, items.get(1).type());
    }

    @Test
    void usesCannedResponsesWhenProvided() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(List.of(
                new AiGeneratedTransaction("Custom 1", 100L, TransactionType.INCOME)
        ));
        service.addCannedResponse("Custom 2", 50L, TransactionType.EXPENSE);

        List<AiGeneratedTransaction> items = service.generateTransactions("Apa saja");
        assertEquals(2, items.size());
        assertEquals("Custom 1", items.get(0).description());
        assertEquals("Custom 2", items.get(1).description());
    }

    @Test
    void handlesNullCannedListConstructor() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(null);
        assertEquals(2, service.generateTransactions("Test").size());
    }

    @Test
    void rejectsBlankPrompt() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));
    }

    @Test
    void simulatesFailure() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        service.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> service.generateTransactions("Test"));
    }
}
