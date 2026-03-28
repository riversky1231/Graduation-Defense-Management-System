package com.example.defensemanagement.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveSignatureWithSanitizedName() throws Exception {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "signatureMaxSizeBytes", 2_097_152L);
        ReflectionTestUtils.setField(service, "templateMaxSizeBytes", 10_485_760L);
        ReflectionTestUtils.setField(service, "defaultMaxSizeBytes", 5_242_880L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "signature.png",
                "image/png",
                "png".getBytes());

        String storedPath = service.save(file, "signatures", "../teacher 1");

        assertEquals("signatures/___teacher_1.png", storedPath);
        assertTrue(Files.exists(tempDir.resolve("signatures").resolve("___teacher_1.png")));
    }

    @Test
    void shouldRejectInvalidSignatureType() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "signatureMaxSizeBytes", 2_097_152L);
        ReflectionTestUtils.setField(service, "templateMaxSizeBytes", 10_485_760L);
        ReflectionTestUtils.setField(service, "defaultMaxSizeBytes", 5_242_880L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "signature.gif",
                "image/gif",
                "gif".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.save(file, "signatures", "teacher_1"));
        assertEquals("签名文件仅支持 PNG 或 JPG 图片", exception.getMessage());
    }

    @Test
    void shouldRejectPathTraversalSubDirectory() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "signatureMaxSizeBytes", 2_097_152L);
        ReflectionTestUtils.setField(service, "templateMaxSizeBytes", 10_485_760L);
        ReflectionTestUtils.setField(service, "defaultMaxSizeBytes", 5_242_880L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "doc".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.save(file, "../templates", "template"));
        assertEquals("非法子目录", exception.getMessage());
    }
}
