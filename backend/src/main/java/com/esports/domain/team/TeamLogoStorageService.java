package com.esports.domain.team;

import com.esports.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamLogoStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path teamLogoDir;

    public TeamLogoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.teamLogoDir = Path.of(uploadDir).toAbsolutePath().normalize().resolve("team-logos");
    }

    public TeamLogoUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Logo file is required.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("FILE_TOO_LARGE", "Logo file must be 2MB or smaller.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(
                    "UNSUPPORTED_FILE_TYPE",
                    "Only PNG, JPG, WEBP, and GIF logo files are allowed.",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(teamLogoDir);
            String filename = UUID.randomUUID() + extension;
            Path destination = teamLogoDir.resolve(filename).normalize();
            if (!destination.startsWith(teamLogoDir)) {
                throw new BusinessException("INVALID_FILE_PATH", "Invalid logo file path.", HttpStatus.BAD_REQUEST);
            }
            file.transferTo(destination);
            return new TeamLogoUploadResponse("/uploads/team-logos/" + filename);
        } catch (IOException ex) {
            throw new BusinessException(
                    "FILE_UPLOAD_FAILED",
                    "Logo upload failed.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
