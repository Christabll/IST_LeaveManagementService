package com.christabella.africahr.leavemanagement.service;


import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads/documents/";


    public Resource loadDocument(String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists() || file.isDirectory()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return new FileSystemResource(file);
    }

    public String storeFile(MultipartFile file) {
        try {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/documents/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store document", e);
        }
    }


    public boolean requiresDocument(String leaveTypeName) {
        return leaveTypeName.equalsIgnoreCase("Sick Leave")
                || leaveTypeName.equalsIgnoreCase("Maternity Leave")
                || leaveTypeName.equalsIgnoreCase("Paternity Leave")
                || leaveTypeName.equalsIgnoreCase("Compassionate Leave");
    }

    public String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return "application/octet-stream";
    }

}
