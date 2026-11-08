package com.maazazhar.ragservice.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.maazazhar.ragservice.config.RagProperties;
import com.maazazhar.ragservice.llm.LlmGateway;

class RagServiceTest {

    private VectorStore vectorStore;
    private LlmGateway llm;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        llm = mock(LlmGateway.class);
        ragService = new RagService(vectorStore, llm,
                new RagProperties(4, 0.35, 400, Duration.ofSeconds(5), 2));
    }

    @Test
    void doesNotCallTheModelWhenNothingRelevantIsFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        RagService.AnswerResponse response = ragService.ask("What is Spring AI?", null);

        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).isEqualTo(RagService.NO_ANSWER);
        assertThat(response.sources()).isEmpty();
        verify(llm, never()).complete(anyString(), anyString());
    }

    @Test
    void sendsNumberedContextAndReturnsSources() {
        Document chunk = Document.builder()
                .text("Spring AI provides a portable ChatClient API.")
                .metadata(Map.of("title", "spring-ai-notes.md"))
                .score(0.91)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));
        when(llm.complete(eq(RagService.SYSTEM_PROMPT), anyString()))
                .thenReturn("It offers a portable ChatClient API [1].");

        RagService.AnswerResponse response = ragService.ask("What does Spring AI offer?", 3);

        assertThat(response.grounded()).isTrue();
        assertThat(response.sources()).singleElement().satisfies(source -> {
            assertThat(source.citation()).isEqualTo(1);
            assertThat(source.title()).isEqualTo("spring-ai-notes.md");
            assertThat(source.score()).isEqualTo(0.91);
        });
        verify(llm).complete(eq(RagService.SYSTEM_PROMPT),
                contains("[1] (spring-ai-notes.md)\nSpring AI provides a portable ChatClient API."));
    }

    @Test
    void marksAnswerUngroundedWhenModelSaysItDoesNotKnow() {
        Document chunk = Document.builder().text("Unrelated text").metadata(Map.of()).build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));
        when(llm.complete(anyString(), anyString())).thenReturn(RagService.NO_ANSWER);

        RagService.AnswerResponse response = ragService.ask("Capital of France?", null);

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void usesUntitledWhenTitleMetadataIsMissing() {
        Document chunk = Document.builder().text("text").metadata(Map.of()).build();
        assertThat(RagService.titleOf(chunk)).isEqualTo("untitled");
    }
}
