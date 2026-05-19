package com.example.ticket.controller;

import com.example.ticket.dto.response.FileUploadResponse;
import com.example.ticket.exception.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private final Path uploadRoot;

    public FileUploadController(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/event-banner")
    public FileUploadResponse uploadEventBanner(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Please choose an image file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Image must be 5MB or smaller");
        }

        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WebP images are supported");
        }

        try {
            Path bannerDir = uploadRoot.resolve("event-banners").normalize();
            Files.createDirectories(bannerDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path destination = bannerDir.resolve(filename).normalize();
            if (!destination.startsWith(bannerDir)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new FileUploadResponse("/uploads/event-banners/" + filename, filename, file.getSize());
        } catch (IOException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store image");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Image file must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
