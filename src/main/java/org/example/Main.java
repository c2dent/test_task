package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.CrptApi.Document;
import org.example.CrptApi.DocumentFormat;
import org.example.CrptApi.DocumentType;

public class Main {
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 3);

        List<CrptApi.Document> documents = List.of(
                new Document("content1", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content2", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content3", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content4", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content5", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content6", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content7", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content8", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content9", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content10", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk"),
                new Document("content11", DocumentFormat.MANUAL, DocumentType.AGGREGATION_DOCUMENT, "milk")
                );

        documents.forEach(document -> {
            new Thread(() -> {
                try {
                    String value = crptApi.createRFDocument(document, document.getContent() + " signature");
                    System.out.println(value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });
    }
}