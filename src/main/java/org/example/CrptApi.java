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

interface CrptDocument {
	public String createRFDocument(org.example.CrptApi.Document document, String signature);
}

public class CrptApi implements CrptDocument {
    private final int requestLimit;
    private final long timeInMillis;
    private long lastResetTime;
    private final AtomicInteger requestCount = new AtomicInteger(0);;
    private final Lock requestLock = new ReentrantLock();
    private final HttpClient client = HttpClient.newHttpClient(); // todo Лучше создать отдельный класс с конфигурацией авторизации и обработкой ошибок.
    private final Gson gson = new Gson();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeInMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
    }

    @Override
    public String createRFDocument(Document document, String signature) {
        waitForRequestLimit();

        Map<String, String> bodyMap = Map.of(
                "document_format", document.getFormat().name(),
                "product_document", document.getContent(),
                "type", document.getType().name(),
                "signature", signature
        );
        
        try {
        	HttpResponse<String> response = sendPostRequest(bodyMap, UrlConstants.LK_DOCUMENTS_RF_CREATE + "?pg=" + document.getProductGroup());

            if (response.statusCode() == 200) {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> resultMap = new Gson().fromJson(response.body(), mapType);
                return resultMap.get("value");
            } else {
                System.out.println(response.statusCode() + " " + response.body());
                return null;
            }

		} catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
		}
    }

    private HttpResponse<String> sendPostRequest(Map<String, String> body, String url) throws IOException, InterruptedException { // todo Этот метод также следует реализовать в классе HttpClient.
        String bearerToken = ""; // todo 
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
            long currentTime = System.currentTimeMillis();
            try {
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
                Thread.sleep(currentTime - lastResetTime);
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
