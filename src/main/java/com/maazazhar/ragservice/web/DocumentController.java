package com.maazazhar.ragservice.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.maazazhar.ragservice.document.DocumentService;
import com.maazazhar.ragservice.document.DocumentService.IngestResponse;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestResponse ingest(@Valid @RequestBody IngestRequest request) {
        return documentService.ingest(request.title(), request.content());
    }

    /** Upload a plain-text or Markdown file. */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        if (!(name.endsWith(".txt") || name.endsWith(".md"))) {
            throw new IllegalArgumentException("Only .txt and .md files are supported");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) {
            throw new IllegalArgumentException("The file is empty");
        }
        return documentService.ingest(name, content);
    }

    public record IngestRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 200_000) String content) {
    }
}
