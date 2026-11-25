package org.delcom.ifs25058springbootcashflow.framework.view;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import org.delcom.ifs25058springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25058springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25058springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25058springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25058springbootcashflow.domain.entity.Role;
import org.delcom.ifs25058springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.entity.User;
import org.delcom.ifs25058springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25058springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25058springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25058springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class FinanceViewTest {

    private static final int USER_ID = 1;

    @TempDir
    Path tempDir;

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        TransactionRepository repository = new TransactionRepository();
        useCase = spy(new FinanceUseCase(repository));
        FinanceAiUseCase aiUseCase = new FinanceAiUseCase(new InMemoryFinanceAiService(), repository);
        MockVaadin.setup();
        AuthSession.login(new User(USER_ID, "andi", "hash", "Andi", Role.USER, null));
        UI.getCurrent().add(new FinanceView(
                useCase, new FinancePresenter(), new FileStorage(tempDir.toString()), aiUseCase));
    }

    @AfterEach
    void tearDown() {
        AuthSession.logout();
        MockVaadin.tearDown();
    }

    @Test
    void addSearchSortAndDeleteFlow() throws Exception {
        assertTrue(_get(Paragraph.class).getText().contains("Data transaksi belum tersedia"));
        assertSummary("0", "Rp 0.00", "Rp 0.00", "Rp 0.00");

        addTransaction("Pemasukan", "Gaji bulanan", "5000000");
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
        assertEquals("Daftar Transaksi · Terbaru", _get(Paragraph.class).getText());

        addTransaction("Pengeluaran", "Makan siang", "25000");
        assertEquals(2, useCase.getTransactionsByUser(USER_ID).size());
        assertSummary("2", "Rp 5,000,000.00", "Rp 25,000.00", "Rp 4,975,000.00");

        TextField searchField = _get(TextField.class, spec -> spec.withPlaceholder("Cari keterangan..."));
        _setValue(searchField, "tidakada");
        clickButton("Cari");
        assertTrue(_get(Paragraph.class).getText().contains("Transaksi tidak ditemukan"));

        _setValue(searchField, "makan");
        clickButton("Cari");
        assertEquals("Hasil: \"makan\" · Terbaru", _get(Paragraph.class).getText());

        @SuppressWarnings("unchecked")
        Grid<Transaction> grid = _get(Grid.class);
        assertEquals(1, grid.getListDataView().getItemCount());

        clickButton("Muat Ulang");
        assertEquals(2, grid.getListDataView().getItemCount());

        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("description"), SortDirection.ASCENDING)));
        assertEquals("Gaji bulanan", grid.getListDataView().getItems().toList().get(0).getDescription());
        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("amount"), SortDirection.DESCENDING)));
        assertEquals("Gaji bulanan", grid.getListDataView().getItems().toList().get(0).getDescription());
        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("type"), SortDirection.ASCENDING)));
        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("id"), SortDirection.ASCENDING)));
        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("createdAt"), SortDirection.ASCENDING)));

        int row = indexOfDescription(grid, "Makan siang");
        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, row, "tindakan");
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        Dialog deleteDialog = _get(Dialog.class);
        clickDialogButton(deleteDialog, "Hapus");

        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
        assertSummary("1", "Rp 5,000,000.00", "Rp 0.00", "Rp 5,000,000.00");
    }

    @Test
    void handlesValidationCancelAndDeleteCancel() throws Exception {
        clickButton("Pengeluaran");
        Dialog addDialog = _get(Dialog.class);
        _setValue(fieldIn(addDialog, "Keterangan"), " ");
        _setValue(fieldIn(addDialog, "Jumlah (Rp)"), "1000");
        clickDialogButton(addDialog, "Simpan");
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());

        _setValue(fieldIn(addDialog, "Keterangan"), "Parkir");
        _setValue(fieldIn(addDialog, "Jumlah (Rp)"), "abc");
        clickDialogButton(addDialog, "Simpan");
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());
        clickDialogButton(addDialog, "Batal");

        TextField searchField = _get(TextField.class, spec -> spec.withPlaceholder("Cari keterangan..."));
        _setValue(searchField, "   ");
        clickButton("Cari");
        assertTrue(_get(Paragraph.class).getText().contains("Data transaksi belum tersedia"));

        useCase.addTransaction(USER_ID, "Ada", 10L, TransactionType.INCOME);
        clickButton("Muat Ulang");

        @SuppressWarnings("unchecked")
        Grid<Transaction> grid = _get(Grid.class);
        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, 0, "tindakan");
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Batal");
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
    }

    @Test
    void handlesNotFoundAndValidationOnDelete() throws Exception {
        useCase.addTransaction(USER_ID, "Temp", 10L, TransactionType.EXPENSE);
        clickButton("Muat Ulang");

        Transaction selected = useCase.getTransactionsByUser(USER_ID).get(0);

        @SuppressWarnings("unchecked")
        Grid<Transaction> grid = _get(Grid.class);
        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, 0, "tindakan");

        doThrow(new EntityNotFoundException(selected.getId()))
                .when(useCase).removeTransactionForUser(USER_ID, selected.getId());
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Hapus");

        doThrow(new ValidationException("bukan milik"))
                .when(useCase).removeTransactionForUser(anyInt(), anyInt());
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Hapus");

        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
    }

    @Test
    void aiDialogRejectsBlankPromptAndCanBeCancelled() {
        clickButton("Generate AI");
        Dialog aiDialog = _get(Dialog.class);
        _setValue(_get(aiDialog, TextArea.class), "  ");
        clickDialogButton(aiDialog, "Proses AI");
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());
        clickDialogButton(aiDialog, "Batal");
    }

    private void addTransaction(String buttonCaption, String description, String amount) {
        clickButton(buttonCaption);
        Dialog dialog = _get(Dialog.class);
        _setValue(fieldIn(dialog, "Keterangan"), description);
        _setValue(fieldIn(dialog, "Jumlah (Rp)"), amount);
        clickDialogButton(dialog, "Simpan");
    }

    private TextField fieldIn(Dialog dialog, String label) {
        return _get(dialog, TextField.class, spec -> spec.withLabel(label));
    }

    private int indexOfDescription(Grid<Transaction> grid, String description) {
        List<Transaction> items = grid.getListDataView().getItems().toList();
        for (int i = 0; i < items.size(); i++) {
            if (description.equals(items.get(i).getDescription())) {
                return i;
            }
        }
        throw new AssertionError("Transaksi tidak ditemukan: " + description);
    }

    private void assertSummary(String count, String income, String expense, String balance) {
        FinanceView view = _get(FinanceView.class);
        HorizontalLayout summary = findFirst(view, HorizontalLayout.class);
        List<String> values = summary.getChildren()
                .map(card -> ((Div) card).getChildren()
                        .skip(1)
                        .map(component -> ((Span) component).getText())
                        .findFirst()
                        .orElse(""))
                .toList();
        assertEquals(List.of(count, income, expense, balance), values);
    }

    private <T extends com.vaadin.flow.component.Component> T findFirst(
            com.vaadin.flow.component.Component root,
            Class<T> type
    ) {
        T found = findFirstOrNull(root, type);
        if (found == null) {
            throw new java.util.NoSuchElementException(type.getSimpleName() + " not found");
        }
        return found;
    }

    private <T extends com.vaadin.flow.component.Component> T findFirstOrNull(
            com.vaadin.flow.component.Component root,
            Class<T> type
    ) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        return root.getChildren()
                .map(child -> findFirstOrNull(child, type))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void clickButton(String caption) {
        _click(_get(Button.class, spec -> spec.withCaption(caption)));
    }

    private void clickDialogButton(Dialog dialog, String caption) {
        _click(_get(dialog, Button.class, spec -> spec.withCaption(caption)));
    }
}
