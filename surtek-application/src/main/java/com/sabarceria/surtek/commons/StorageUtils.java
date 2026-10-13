package com.sabarceria.surtek.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class StorageUtils {

    private static final Logger log = LoggerFactory.getLogger(StorageUtils.class);

    private static final Path rootPath = Paths.get("upload");

    public static void upload(MultipartFile file) {
        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectory(rootPath);
            }

            String fileName = file.getOriginalFilename();
            Files.copy(file.getInputStream(), Path.of(rootPath.toString(), fileName), StandardCopyOption.REPLACE_EXISTING);

        }catch (Exception e) {
            e.printStackTrace();
            log.error("Error while uploading file =>{}", e.getMessage());
        }
    }

}
