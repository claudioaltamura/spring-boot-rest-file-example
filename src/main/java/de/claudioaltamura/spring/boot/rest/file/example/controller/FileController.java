package de.claudioaltamura.spring.boot.rest.file.example.controller;

import de.claudioaltamura.spring.boot.rest.file.example.api.FileApi;
import de.claudioaltamura.spring.boot.rest.file.example.model.ApplicationError;
import de.claudioaltamura.spring.boot.rest.file.example.model.FileMetaInfo;
import de.claudioaltamura.spring.boot.rest.file.example.service.StorageFileNotFoundException;
import de.claudioaltamura.spring.boot.rest.file.example.service.StorageService;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class FileController implements FileApi {

    @Autowired
    private StorageService storageService;

    @Autowired
    private ServletContext servletContext;


    @Override
    public ResponseEntity<byte[]> downloadFile(String fileName) {
        final var resource = storageService.loadAsResource(fileName);

        String contentType = null;
        try {
            contentType = servletContext.getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not determine file type.", e);
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource.getContentAsByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public ResponseEntity<List<FileMetaInfo>> listFiles() {
        final var files = storageService.loadAll()
                .map(this::buildFileMetaInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(files);
    }

    @Override
    public ResponseEntity<FileMetaInfo> uploadFile(MultipartFile attachment) {
        return ResponseEntity.ok(storeFile(attachment));
    }

    private FileMetaInfo storeFile(MultipartFile attachment) {
        final var fileName = storageService.store(attachment);
        log.info("storing file '{}'", fileName);

        final var fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(fileName)
                .toUriString();

        final var fileMetaInfo =  new FileMetaInfo();
        fileMetaInfo.setFileName(fileName);
        fileMetaInfo.setDownloadUrl(fileDownloadUri);
        fileMetaInfo.setContentType(attachment.getContentType());
        fileMetaInfo.setSize(attachment.getSize());

        return fileMetaInfo;
    }

    @Override
    public ResponseEntity<List<FileMetaInfo>> uploadFiles(List<MultipartFile> attachments) {
        final var fileMetaInfoList = attachments.stream()
                .map(this::storeFile)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileMetaInfoList);
    }

    private FileMetaInfo buildFileMetaInfo(Path path) {
        final var fileMetaInfo =  new FileMetaInfo();

        try {
            fileMetaInfo.setFileName(path.getFileName().toString());
            fileMetaInfo.setDownloadUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/download/")
                    .path(path.getFileName().toString())
                    .toUriString());
            fileMetaInfo.setContentType(Files.probeContentType(path.getFileName()));
            final var filePath = Paths.get(path.toAbsolutePath().toString());
            fileMetaInfo.setSize(Files.size(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileMetaInfo;
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<ApplicationError> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        final var error = new ApplicationError();
        error.setErrorId(UUID.randomUUID().toString());
        error.setErrorMessage("file does not exist.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
