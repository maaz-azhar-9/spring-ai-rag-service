package com.maazazhar.ragservice.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maazazhar.ragservice.rag.RagService;
import com.maazazhar.ragservice.rag.RagService.AnswerResponse;

@RestController
@RequestMapping("/api")
public class AskController {

    private final RagService ragService;

    public AskController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AnswerResponse ask(@Valid @RequestBody AskRequest request) {
        return ragService.ask(request.question(), request.topK());
    }

    public record AskRequest(
            @NotBlank @Size(max = 2000) String question,
            @Min(1) @Max(10) Integer topK) {
    }
}
