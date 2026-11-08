package com.maazazhar.ragservice.document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.maazazhar.ragservice.config.RagProperties;

/** Splits documents into token-sized chunks, embeds them and stores them in the vector store. */
@Service
public class DocumentService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public DocumentService(VectorStore vectorStore, RagProperties properties) {
        this.vectorStore = vectorStore;
        // chunk size, min chunk chars, min length to embed, max chunks, keep separators
        this.splitter = new TokenTextSplitter(properties.chunkSize(), 200, 10, 10_000, true);
    }

    public IngestResponse ingest(String title, String content) {
        String documentId = UUID.randomUUID().toString();
        Document document = new Document(content, Map.of("title", title, "documentId", documentId));

        List<Document> chunks = splitter.apply(List.of(document));
        vectorStore.add(chunks);

        return new IngestResponse(documentId, title, chunks.size());
    }

    public record IngestResponse(String documentId, String title, int chunks) {
    }
}
