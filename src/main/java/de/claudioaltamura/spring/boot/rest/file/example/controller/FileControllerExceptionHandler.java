package de.claudioaltamura.spring.boot.rest.file.example.controller;

import de.claudioaltamura.spring.boot.rest.file.example.model.ApiError;
import de.claudioaltamura.spring.boot.rest.file.example.service.StorageException;
import de.claudioaltamura.spring.boot.rest.file.example.service.StorageFileNotFoundException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class FileControllerExceptionHandler extends ResponseEntityExceptionHandler {

  static ApiError apiError(String message) {
    final var apiError = new ApiError();

    apiError.setErrorId(UUID.randomUUID().toString());
    apiError.setErrorMessage(message);

    return apiError;
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<ApiError> handleStorageException(StorageException exc) {
    final var apiError = apiError(exc.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
  }

  @ExceptionHandler(StorageFileNotFoundException.class)
  public ResponseEntity<ApiError> handleStorageFileNotFound(StorageFileNotFoundException exc) {
    final var apiError = apiError("File does not exist.");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
  }

  @ExceptionHandler(FileControllerException.class)
  public ResponseEntity<ApiError> handleStorageFileNotFound(FileControllerException exc) {
    final var apiError = apiError(exc.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
  }
}
