package com.esports.domain.player;

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
public class PlayerImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path playerImageDir;

    public PlayerImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.playerImageDir = Path.of(uploadDir).toAbsolutePath().normalize().resolve("player-images");
    }

    public PlayerImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "프로필 이미지 파일을 선택해주세요.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("FILE_TOO_LARGE", "프로필 이미지는 2MB 이하만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(
                    "UNSUPPORTED_FILE_TYPE",
                    "프로필 이미지는 PNG, JPG, WEBP, GIF 형식만 업로드할 수 있습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(playerImageDir);
            String filename = UUID.randomUUID() + extension;
            Path destination = playerImageDir.resolve(filename).normalize();
            if (!destination.startsWith(playerImageDir)) {
                throw new BusinessException("INVALID_FILE_PATH", "올바르지 않은 프로필 이미지 경로입니다.", HttpStatus.BAD_REQUEST);
            }
            file.transferTo(destination);
            return new PlayerImageUploadResponse("/uploads/player-images/" + filename);
        } catch (IOException ex) {
            throw new BusinessException(
                    "FILE_UPLOAD_FAILED",
                    "프로필 이미지 업로드에 실패했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
