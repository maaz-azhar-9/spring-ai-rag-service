package com.maazazhar.ragservice.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import com.maazazhar.ragservice.config.RagProperties;

class DocumentServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void splitsDocumentIntoChunksThatKeepTitleMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        DocumentService service = new DocumentService(vectorStore,
                new RagProperties(4, 0.35, 100, Duration.ofSeconds(5), 2));
        String longText = "Spring AI makes it simple to build RAG pipelines in Java. ".repeat(120);

        DocumentService.IngestResponse response = service.ingest("guide.md", longText);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> chunks = captor.getValue();

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(response.chunks()).isEqualTo(chunks.size());
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getMetadata()).containsEntry("title", "guide.md");
            assertThat(chunk.getMetadata()).containsEntry("documentId", response.documentId());
        });
    }
}
