package com.maazazhar.ragservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.maazazhar.ragservice.llm.LlmUnavailableException;
import com.maazazhar.ragservice.rag.RagService;

@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void returnsAnswerWithSources() throws Exception {
        when(ragService.ask(anyString(), any())).thenReturn(new RagService.AnswerResponse(
                "Use ChatClient [1].", List.of(new RagService.Source(1, "notes.md", 0.9)), true));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How do I call a model?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.sources[0].title").value("notes.md"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTopKOutOfRange() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Hi\",\"topK\":50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns503WhenModelIsUnavailable() throws Exception {
        when(ragService.ask(anyString(), any()))
                .thenThrow(new LlmUnavailableException("The language model is unavailable", null));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Anything?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Model unavailable"));
    }
}
