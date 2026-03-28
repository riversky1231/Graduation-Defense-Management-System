package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.service.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> SIGNATURE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Set<String> SIGNATURE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final Set<String> TEMPLATE_EXTENSIONS = Set.of("docx");
    private static final Set<String> TEMPLATE_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream");

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    @Value("${app.upload.signature-max-size-bytes:2097152}")
    private long signatureMaxSizeBytes;

    @Value("${app.upload.template-max-size-bytes:10485760}")
    private long templateMaxSizeBytes;

    @Value("${app.upload.default-max-size-bytes:5242880}")
    private long defaultMaxSizeBytes;

    /**
     * 获取上传目录的绝对路径
     */
    private Path getBasePath() {
        Path basePath = Paths.get(baseDir);
        // 如果是相对路径，转换为相对于项目根目录的绝对路径
        if (!basePath.isAbsolute()) {
            // 获取项目根目录（通常是工作目录）
            String userDir = System.getProperty("user.dir");
            basePath = Paths.get(userDir, baseDir);
        }
        return basePath;
    }

    @Override
    @SuppressWarnings("null")
    public String save(MultipartFile file, String subDir, String filename) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        try {
            validateFile(file, subDir);

            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            String safeName = sanitizeBaseName(StringUtils.hasText(filename) ? filename : UUID.randomUUID().toString());
            if (StringUtils.hasText(ext)) {
                safeName = safeName + "." + ext;
            }

            Path basePath = getBasePath();
            Path dirPath = resolveSubDirectory(basePath, subDir);
            Files.createDirectories(dirPath);

            Path target = dirPath.resolve(safeName).normalize();
            if (!target.startsWith(dirPath)) {
                throw new IllegalArgumentException("非法文件路径");
            }
            java.io.File targetFile = target.toFile();

            java.io.File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            file.transferTo(targetFile);

            Path relativePath = basePath.relativize(target);
            return relativePath.toString().replace("\\", "/");
        } catch (IOException e) {
            Path basePath = getBasePath();
            Path dirPath = resolveSubDirectory(basePath, subDir);
            throw new RuntimeException("保存文件失败: " + e.getMessage() + 
                " (目标目录: " + dirPath.toAbsolutePath() + ")", e);
        }
    }

    private void validateFile(MultipartFile file, String subDir) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        long fileSize = file.getSize();

        if (subDir != null && subDir.contains("signatures")) {
            if (!SIGNATURE_EXTENSIONS.contains(normalizedExtension) || !SIGNATURE_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("签名文件仅支持 PNG 或 JPG 图片");
            }
            if (fileSize > signatureMaxSizeBytes) {
                throw new IllegalArgumentException("签名文件不能超过 2MB");
            }
            return;
        }

        if (subDir != null && subDir.contains("templates")) {
            if (!TEMPLATE_EXTENSIONS.contains(normalizedExtension) || !TEMPLATE_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("模板文件仅支持 .docx 格式");
            }
            if (fileSize > templateMaxSizeBytes) {
                throw new IllegalArgumentException("模板文件不能超过 10MB");
            }
            return;
        }

        if (fileSize > defaultMaxSizeBytes) {
            throw new IllegalArgumentException("上传文件超过大小限制");
        }
    }

    private Path resolveSubDirectory(Path basePath, String subDir) {
        if (!StringUtils.hasText(subDir)) {
            return basePath;
        }
        Path resolved = basePath.resolve(subDir).normalize();
        if (!resolved.startsWith(basePath.normalize())) {
            throw new IllegalArgumentException("非法子目录");
        }
        return resolved;
    }

    private String sanitizeBaseName(String filename) {
        String sanitized = filename.replaceAll("[^a-zA-Z0-9_-]", "_");
        return StringUtils.hasText(sanitized) ? sanitized : UUID.randomUUID().toString();
    }
}

