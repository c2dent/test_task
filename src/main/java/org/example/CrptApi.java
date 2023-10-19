package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final int requestLimit;
    private final long timeInMillis;
    private final AtomicInteger requestCount;
    private final Lock requestLock;
    private long lastResetTime;
    private final HttpClient client = HttpClient.newHttpClient(); // Лучше создать отдельный класс с конфигурацией авторизации и обработкой ошибок.
    private final Gson gson = new Gson();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeInMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.requestLock = new ReentrantLock();
        this.lastResetTime = System.currentTimeMillis();
    }

    public String createRFDocument(Document document, String signature) throws InterruptedException, IOException {
        waitForRequestLimit();

        Map<String, String> bodyMap = Map.of(
                "document_format", document.getFormat().name(),
                "product_document", document.getContent(),
                "type", document.getType().name(),
                "signature", signature
        );

        HttpResponse<String> response = sendPostRequest(bodyMap, UrlConstants.LK_DOCUMENTS_RF_CREATE + "?pg=" + document.getProductGroup());
        if (response.statusCode() == 200) {
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> resultMap = new Gson().fromJson(response.body(), mapType);
            return resultMap.get("value");
        } else {
            System.out.println(response.statusCode() + " " + response.body());
            return null;
        }
    }

    private HttpResponse<String> sendPostRequest(Map<String, String> body, String url) throws IOException, InterruptedException { // Этот метод также следует реализовать в классе HttpClient.
        String bearerToken = "";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/" + url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void waitForRequestLimit() {
        while (true) {
            requestLock.lock();
            try {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastResetTime >= timeInMillis) {
                    requestCount.set(0);
                    lastResetTime = currentTime;
                }

                if (requestCount.get() < requestLimit) {
                    requestCount.incrementAndGet();
                    return;
                }
            } finally {
                requestLock.unlock();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class UrlConstants {
        public static final String LK_DOCUMENTS_RF_CREATE = "lk/documents/commissioning/contract/create";
    }

    public enum DocumentFormat {
        MANUAL,
        XML,
        CSV
    }

    public enum DocumentType {
        AGGREGATION_DOCUMENT,
        REAGGREGATION_DOCUMENT,
        REAGGREGATION_DOCUMENT_XML,
        LP_INTRODUCE_GOODS_CSV,
        LK_RECEIPT_XML
        // оставлные типы
    }


    public static class Document {
        private final String content;
        private final DocumentFormat format;
        private final DocumentType type;

        private final String productGroup;

        public Document(String content, DocumentFormat format, DocumentType type, String productGroup) {
            this.content = content;
            this.format = format;
            this.type = type;
            this.productGroup = productGroup;
        }

        public String getContent() {
            return content;
        }

        public DocumentFormat getFormat() {
            return format;
        }

        public DocumentType getType() {
            return type;
        }

        public String getProductGroup() {
            return productGroup;
        }
    }
}
