package org.delcom.ifs25058springbootcashflow.adapter.ai;

import org.delcom.ifs25058springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25058springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25058springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25058springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25058springbootcashflow.domain.repository.IFinanceAiService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementasi IFinanceAiService yang memanggil endpoint OpenAI-compatible HTTP API.
 * Respons AI diharapkan berupa JSON array objek {description, amount, type}.
 */
public class OpenAiFinanceService implements IFinanceAiService {

    /** Model bawaan jika OPENAI_MODEL kosong (Gemini lewat endpoint OpenAI-compatible). */
    static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiFinanceService(String endpoint, String apiKey, String model) {
        this(endpoint, apiKey, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    public OpenAiFinanceService(String endpoint, String apiKey, String model, HttpClient httpClient, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Prompt catatan keuangan tidak boleh kosong");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_ENDPOINT belum diatur");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_KEY belum diatur");
        }

        String targetUrl = normalizeEndpoint(endpoint.trim());
        String requestJson = buildRequestBody(prompt.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Pemanggilan API AI terinterupsi: " + e.getMessage());
        } catch (IOException e) {
            throw new DomainException("Gagal menghubungi server AI: " + e.getMessage());
        }

        if (response.statusCode() != 200) {
            throw new DomainException(describeHttpError(response.statusCode()));
        }

        return parseAiResponse(response.body());
    }

    /** Menerjemahkan status HTTP menjadi pesan yang ramah pengguna (tanpa membocorkan body mentah). */
    private String describeHttpError(int status) {
        return switch (status) {
            case 400 -> "Permintaan ke layanan AI ditolak. Periksa OPENAI_MODEL dan isi catatan Anda";
            case 401, 403 -> "API key AI tidak valid atau tidak punya akses. Periksa OPENAI_KEY di file .env";
            case 404 -> "Endpoint atau model AI tidak ditemukan. Periksa OPENAI_ENDPOINT dan OPENAI_MODEL";
            case 429 -> "Batas penggunaan AI tercapai. Coba lagi beberapa saat lagi";
            default -> status >= 500
                    ? "Server AI sedang bermasalah (" + status + "). Coba lagi nanti"
                    : "Layanan AI mengembalikan error (" + status + ")";
        };
    }

    private String normalizeEndpoint(String ep) {
        String base = ep.endsWith("/") ? ep.substring(0, ep.length() - 1) : ep;
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String buildRequestBody(String prompt) throws DomainException {
        try {
            String systemInstruction = "Anda adalah asisten pencatat keuangan pribadi. "
                    + "Tugas Anda mengubah catatan keuangan pengguna menjadi daftar transaksi. "
                    + "Kembalikan HANYA JSON array valid tanpa markdown atau teks tambahan. "
                    + "Setiap elemen wajib berupa objek dengan key 'description' (string singkat), "
                    + "'amount' (bilangan bulat positif dalam Rupiah, tanpa pemisah ribuan), "
                    + "dan 'type' (string 'INCOME' untuk pemasukan atau 'EXPENSE' untuk pengeluaran). "
                    + "Contoh format: [{\"description\":\"Gaji bulanan\",\"amount\":5000000,\"type\":\"INCOME\"},"
                    + "{\"description\":\"Makan siang\",\"amount\":25000,\"type\":\"EXPENSE\"}]";

            String selectedModel = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
            Map<String, Object> payload = Map.of(
                    "model", selectedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new DomainException("Gagal menyusun request AI: " + e.getMessage());
        }
    }

    private List<AiGeneratedTransaction> parseAiResponse(String responseBody) throws DomainException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new DomainException("Respon AI tidak memiliki pilihan (choices)");
            }

            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new DomainException("Konten respon AI kosong");
            }

            String content = stripMarkdownCodeFence(message.get("content").asText().trim());

            JsonNode arrayNode = objectMapper.readTree(content);
            if (!arrayNode.isArray()) {
                throw new DomainException("Format konten respon AI bukan merupakan JSON array");
            }

            List<AiGeneratedTransaction> result = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                toTransaction(item).ifPresent(result::add);
            }

            if (result.isEmpty()) {
                throw new DomainException("AI tidak menghasilkan daftar transaksi yang valid");
            }

            return result;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Gagal memproses respon dari AI: " + e.getMessage());
        }
    }

    /** Mengubah satu elemen JSON menjadi transaksi; elemen tidak valid dilewati (Optional kosong). */
    private Optional<AiGeneratedTransaction> toTransaction(JsonNode item) {
        JsonNode descriptionNode = item.get("description");
        JsonNode amountNode = item.get("amount");
        JsonNode typeNode = item.get("type");
        if (descriptionNode == null || descriptionNode.asText().isBlank()
                || amountNode == null || typeNode == null) {
            return Optional.empty();
        }
        long amount = parseAmount(amountNode);
        TransactionType type = parseType(typeNode.asText());
        if (amount <= 0 || type == null) {
            return Optional.empty();
        }
        return Optional.of(new AiGeneratedTransaction(descriptionNode.asText().trim(), amount, type));
    }

    private long parseAmount(JsonNode node) {
        if (node.isNumber()) {
            return node.asLong();
        }
        try {
            return Long.parseLong(node.asText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private TransactionType parseType(String text) {
        String normalized = text.trim().toUpperCase(Locale.ROOT);
        if ("INCOME".equals(normalized) || "PEMASUKAN".equals(normalized)) {
            return TransactionType.INCOME;
        }
        if ("EXPENSE".equals(normalized) || "PENGELUARAN".equals(normalized)) {
            return TransactionType.EXPENSE;
        }
        return null;
    }

    private String stripMarkdownCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = (firstNewline != -1) ? trimmed.substring(firstNewline + 1) : trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
