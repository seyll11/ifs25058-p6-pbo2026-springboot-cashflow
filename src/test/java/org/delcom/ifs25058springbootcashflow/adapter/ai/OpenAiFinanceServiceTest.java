package org.delcom.ifs25058springbootcashflow.adapter.ai;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25058springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiFinanceServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private String serverUrl;
    private String mockResponseBody;
    private int mockResponseCode;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = mockResponseBody != null ? mockResponseBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(mockResponseCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();
        mockResponseCode = 200;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Membungkus konten model ke format respons chat completions. */
    private static String chatResponse(String content) {
        return MAPPER.writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
    }

    private OpenAiFinanceService service() {
        return new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
    }

    @Test
    void generatesTransactionsSuccessfully() throws Exception {
        mockResponseBody = chatResponse(
                "[{\"description\":\"Gaji\",\"amount\":5000000,\"type\":\"INCOME\"},"
                        + "{\"description\":\"Makan siang\",\"amount\":25000,\"type\":\"expense\"}]");

        List<AiGeneratedTransaction> items = service().generateTransactions("Gaji 5 juta, makan 25 ribu");
        assertEquals(2, items.size());
        assertEquals("Gaji", items.get(0).description());
        assertEquals(5_000_000L, items.get(0).amount());
        assertEquals(TransactionType.INCOME, items.get(0).type());
        assertEquals(TransactionType.EXPENSE, items.get(1).type());
    }

    @Test
    void acceptsIndonesianTypesAndTextAmounts() throws Exception {
        mockResponseBody = chatResponse(
                "[{\"description\":\"Bonus\",\"amount\":\"1,500,000\",\"type\":\"Pemasukan\"},"
                        + "{\"description\":\"Listrik\",\"amount\":\"300000\",\"type\":\" PENGELUARAN \"}]");

        List<AiGeneratedTransaction> items = service().generateTransactions("Catatan");
        assertEquals(2, items.size());
        assertEquals(1_500_000L, items.get(0).amount());
        assertEquals(TransactionType.INCOME, items.get(0).type());
        assertEquals(300_000L, items.get(1).amount());
        assertEquals(TransactionType.EXPENSE, items.get(1).type());
    }

    @Test
    void skipsInvalidItemsAndFallsBackToDefaultModel() throws Exception {
        mockResponseBody = chatResponse("```json\n["
                + "{},"
                + "{\"description\":\"  \",\"amount\":1,\"type\":\"INCOME\"},"
                + "{\"description\":\"Tanpa jumlah\",\"type\":\"INCOME\"},"
                + "{\"description\":\"Tanpa tipe\",\"amount\":1},"
                + "{\"description\":\"Jumlah teks\",\"amount\":\"abc\",\"type\":\"INCOME\"},"
                + "{\"description\":\"Jumlah negatif\",\"amount\":-5,\"type\":\"INCOME\"},"
                + "{\"description\":\"Tipe aneh\",\"amount\":5,\"type\":\"LAIN\"},"
                + "{\"description\":\"Valid\",\"amount\":10,\"type\":\"INCOME\"}"
                + "]\n```");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/", "valid-key", "   ");
        List<AiGeneratedTransaction> items = service.generateTransactions("Catatan");
        assertEquals(1, items.size());
        assertEquals("Valid", items.get(0).description());
    }

    @Test
    void handlesCodeFenceWithoutNewlineAndClosingFenceVariants() throws Exception {
        mockResponseBody = chatResponse("```[{\"description\":\"Inline\",\"amount\":1,\"type\":\"INCOME\"}]");
        assertEquals(1, service().generateTransactions("Catatan").size());

        mockResponseBody = chatResponse("```[{\"description\":\"Inline 2\",\"amount\":1,\"type\":\"INCOME\"}]```");
        assertEquals(1, service().generateTransactions("Catatan").size());
    }

    @Test
    void handlesEndpointAlreadyEndingWithChatCompletionsAndNullModel() throws Exception {
        mockResponseBody = chatResponse("[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/chat/completions", "valid-key", null);
        assertEquals(1, service.generateTransactions("Catatan").size());
    }

    @Test
    void rejectsInvalidInputs() {
        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost", "valid-key", "model");
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));

        OpenAiFinanceService nullEndpoint = new OpenAiFinanceService(null, "valid-key", "model");
        assertThrows(ValidationException.class, () -> nullEndpoint.generateTransactions("Prompt"));

        OpenAiFinanceService blankEndpoint = new OpenAiFinanceService("   ", "valid-key", "model");
        assertThrows(ValidationException.class, () -> blankEndpoint.generateTransactions("Prompt"));

        OpenAiFinanceService nullKey = new OpenAiFinanceService("http://localhost", null, "model");
        assertThrows(ValidationException.class, () -> nullKey.generateTransactions("Prompt"));

        OpenAiFinanceService blankKey = new OpenAiFinanceService("http://localhost", "   ", "model");
        assertThrows(ValidationException.class, () -> blankKey.generateTransactions("Prompt"));
    }

    @Test
    void throwsOnHttpError() {
        mockResponseCode = 500;
        mockResponseBody = "Internal Server Error";

        DomainException ex = assertThrows(DomainException.class, () -> service().generateTransactions("Catatan"));
        assertEquals("Server AI sedang bermasalah (500). Coba lagi nanti", ex.getMessage());
    }

    @Test
    void mapsHttpStatusToFriendlyMessages() {
        assertEquals("Permintaan ke layanan AI ditolak. Periksa OPENAI_MODEL dan isi catatan Anda", errorFor(400));
        assertEquals("API key AI tidak valid atau tidak punya akses. Periksa OPENAI_KEY di file .env", errorFor(401));
        assertEquals("API key AI tidak valid atau tidak punya akses. Periksa OPENAI_KEY di file .env", errorFor(403));
        assertEquals("Endpoint atau model AI tidak ditemukan. Periksa OPENAI_ENDPOINT dan OPENAI_MODEL", errorFor(404));
        assertEquals("Batas penggunaan AI tercapai. Coba lagi beberapa saat lagi", errorFor(429));
        assertEquals("Layanan AI mengembalikan error (418)", errorFor(418));
    }

    private String errorFor(int status) {
        mockResponseCode = status;
        mockResponseBody = "{\"error\":\"detail rahasia\"}";
        DomainException ex = assertThrows(DomainException.class, () -> service().generateTransactions("Catatan"));
        return ex.getMessage();
    }

    @Test
    void throwsOnMissingChoicesOrEmptyContent() {
        OpenAiFinanceService service = service();

        mockResponseBody = "{}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": []}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
    }

    @Test
    void throwsWhenContentIsNotJsonArrayOrHasNoValidItems() {
        OpenAiFinanceService service = service();

        mockResponseBody = chatResponse("{\"single\": 1}");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = chatResponse("[{\"description\":\"\",\"amount\":1,\"type\":\"INCOME\"}]");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = chatResponse("bukan json sama sekali");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
    }

    @Test
    void handlesHttpClientExceptions() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "key", "model", mockClient, new ObjectMapper());
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));

        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Interrupted"));
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));
        Thread.interrupted();
    }

    @Test
    void handlesSerializationError() {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failure"));

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "key", "model", HttpClient.newHttpClient(), mockMapper);
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));
    }
}
