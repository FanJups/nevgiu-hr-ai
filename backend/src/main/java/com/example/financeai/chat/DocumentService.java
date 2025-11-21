package com.example.financeai.chat;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final VectorStore vectorStore;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        List<String> chunks = chunk(text, 700);
        List<Document> docs = new ArrayList<>();

        for (String chunk : chunks) {
            Document doc = new Document(chunk);
            docs.add(doc);
        }

        vectorStore.add(docs);
    }

    private List<String> chunk(String text, int size) {
        List<String> result = new ArrayList<>();

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + size, length);
            result.add(text.substring(start, end));
            start = end;
        }

        return result;
    }

}
