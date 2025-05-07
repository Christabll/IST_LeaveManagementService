package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.service.DocumentService;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;


    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getDocument(@PathVariable String filename) {
        try {
            Resource resource = documentService.loadDocument(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
