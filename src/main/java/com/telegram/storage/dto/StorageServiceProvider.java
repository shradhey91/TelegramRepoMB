package com.telegram.storage.dto;

import org.springframework.web.multipart.MultipartFile;

public interface StorageServiceProvider {

    UploadResult upload(
            MultipartFile file,
            StorageFolder folder,
            Long userId
    );

    byte[] download(String fileName);

    void delete(String fileName, String fileId);
}
