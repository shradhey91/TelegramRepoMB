package com.telegram.storage.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.telegram.storage.dto.StorageFolder;
import com.telegram.storage.dto.StorageServiceProvider;
import com.telegram.storage.dto.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BackblazeStorageServiceProvider implements StorageServiceProvider {

    private final B2StorageClient storageClient;

    @Value("${b2.bucket-id}")
    private String bucketId;

    @Value("${b2.bucket-name}")
    private String bucketName;

    @Override
    public UploadResult upload(MultipartFile file, StorageFolder folder, Long userId) {

        try {
            String extension = getExtension(file.getOriginalFilename());
            String fileName = folder.getFolder() + "/user-" + userId + "/" + UUID.randomUUID() + "." + extension;
            B2ContentSource source = B2ByteArrayContentSource.build(file.getBytes());
            B2UploadFileRequest request =
                    B2UploadFileRequest.builder(
                            bucketId,
                            fileName,
                            file.getContentType(),
                            source
                    ).build();
            storageClient.uploadSmallFile(request);

            var response = storageClient.uploadSmallFile(request);
            String url = String.format("https://f000.backblazeb2.com/file/%s/%s", bucketName, fileName);

            return new UploadResult(
                    response.getFileId(),
                    fileName,
                    url,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public void delete(String fileName, String fileId) {

        try {
            storageClient.deleteFileVersion(fileName, fileId);
        } catch (Exception e) {
            throw new RuntimeException("Delete failed", e);
        }
    }

    private String getExtension(String originalFileName) {

        if (originalFileName == null) {
            return "bin";
        }
        int index = originalFileName.lastIndexOf('.');

        if (index < 0) {
            return "bin";
        }
        return originalFileName.substring(index + 1);
    }

    @Override
    public byte[] download(String fileName) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            storageClient.downloadByName(B2DownloadByNameRequest
                            .builder(bucketName, fileName)
                            .build(),
                    (headers, inputStream) -> {
                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
            );

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Download failed", e);
        }
    }
}
