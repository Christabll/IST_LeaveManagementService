package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.service.DocumentService;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/uploads/documents/{filename:.+}")
    public ResponseEntity<Resource> getDocument(
            @PathVariable String filename,
            @RequestParam(defaultValue = "inline") String disposition) {
        try {
            Resource resource = documentService.loadDocument(filename);
            String contentType = documentService.detectContentType(filename);
            String contentDisposition = disposition.equals("attachment")
                    ? "attachment; filename=\"" + filename + "\""
                    : "inline; filename=\"" + filename + "\"";
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}