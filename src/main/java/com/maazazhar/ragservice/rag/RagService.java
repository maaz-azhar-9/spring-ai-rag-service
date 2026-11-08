package com.maazazhar.ragservice.rag;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.maazazhar.ragservice.config.RagProperties;
import com.maazazhar.ragservice.llm.LlmGateway;

/**
 * Retrieval-augmented question answering.
 *
 * <p>Retrieves the most similar chunks, and only calls the model when something relevant was
 * found. That keeps answers grounded and avoids paying for a model call that can only guess.
 */
@Service
public class RagService {

    static final String NO_ANSWER =
            "I couldn't find anything about that in the indexed documents.";

    static final String SYSTEM_PROMPT = """
            You are a precise assistant that answers questions using ONLY the numbered context
            passages provided. Cite the passages you used like [1] or [2][3].
            If the context does not contain the answer, reply exactly: "%s"
            Never use outside knowledge and never invent citations.
            """.formatted(NO_ANSWER);

    private final VectorStore vectorStore;
    private final LlmGateway llm;
    private final RagProperties properties;

    public RagService(VectorStore vectorStore, LlmGateway llm, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.llm = llm;
        this.properties = properties;
    }

    public AnswerResponse ask(String question, Integer topK) {
        List<Document> hits = retrieve(question, topK == null ? properties.defaultTopK() : topK);
        if (hits.isEmpty()) {
            return new AnswerResponse(NO_ANSWER, List.of(), false);
        }

        String answer = llm.complete(SYSTEM_PROMPT, buildUserPrompt(question, hits));
        boolean grounded = !answer.contains(NO_ANSWER);
        return new AnswerResponse(answer, grounded ? toSources(hits) : List.of(), grounded);
    }

    public List<Document> retrieve(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(properties.similarityThreshold())
                .build());
        return results == null ? List.of() : results;
    }

    static String buildContext(List<Document> hits) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            Document hit = hits.get(i);
            context.append('[').append(i + 1).append("] (")
                    .append(titleOf(hit)).append(")\n")
                    .append(hit.getText()).append("\n\n");
        }
        return context.toString();
    }

    private static String buildUserPrompt(String question, List<Document> hits) {
        return "Context:\n" + buildContext(hits) + "Question: " + question;
    }

    private static List<Source> toSources(List<Document> hits) {
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Document hit = hits.get(i);
            sources.add(new Source(i + 1, titleOf(hit), hit.getScore()));
        }
        return sources;
    }

    static String titleOf(Document document) {
        Object title = document.getMetadata().get("title");
        return title == null ? "untitled" : title.toString();
    }

    public record AnswerResponse(String answer, List<Source> sources, boolean grounded) {
    }

    public record Source(int citation, String title, Double score) {
    }
}
