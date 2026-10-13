package com.sabarceria.surtek.commons;

import com.sabarceria.surtek.exceptions.InvalidDataException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@Slf4j
public class FileUploadUtil {

    public static void saveFile(String uploadDir, String fileName, InputStream stream) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);

    }

    public static void saveFile(String uploadDir, String fileName, MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            log.error("Could not save image file {} >> : {} ", fileName, ioe.getMessage());
        }
    }

    public static void saveImage(String imageUrl, String destinationFile) throws IOException {
        URL url = new URL(imageUrl);
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(destinationFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }
        is.close();
        os.close();
    }

    public static String pathToBase64(String stringUrl) throws IOException {
        URL url = new URL(stringUrl);
        return urlToBase64(url);
    }

    public static File getFile(String uploadDir, String fileName) {
        File file = new File(uploadDir + File.separator + fileName);
        if (!file.exists()) {
            throw new InvalidDataException("file " + fileName + " not found");
        }

        return file;
    }

    public static String urlToBase64(URL url) throws IOException {
        byte[] fileContent = IOUtils.toByteArray(url.openStream());
        return Base64.getEncoder().encodeToString(fileContent);
    }


    public static byte[] pathToByte(String stringUrl) throws IOException {
        URL url = new URL(stringUrl);
        return IOUtils.toByteArray(url.openStream());
    }

}
