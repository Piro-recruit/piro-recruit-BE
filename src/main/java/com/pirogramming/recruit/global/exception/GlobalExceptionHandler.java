package com.pirogramming.recruit.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.exception.entity_exception.MemberNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(RecruitException.class)
	public ResponseEntity<ApiRes<Void>> handleRecruitException(RecruitException e) {
		log.error("RecruitException caught: {}", e.getMessage(), e);
		return ResponseEntity.status(e.getStatus())
			.body(ApiRes.failure(e.getStatus(), e.getMessage(), ErrorCode.INTERNAL_ERROR));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiRes<Void>> handleValidationException(MethodArgumentNotValidException e) {
		String detailMessage = e.getBindingResult().getFieldErrors().stream()
			.map(err -> err.getField() + ": " + err.getDefaultMessage())
			.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiRes.failure(HttpStatus.BAD_REQUEST, detailMessage, ErrorCode.INVALID_ARGUMENT));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiRes<Void>> handleEntityNotFound(EntityNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiRes<Void>> handleAccessDeniedException(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiRes.failure(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiRes<Void>> handleIllegalArgument(IllegalArgumentException e) {
		log.warn("잘못된 요청 파라미터: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiRes.failure(HttpStatus.BAD_REQUEST, e.getMessage(), ErrorCode.INVALID_ARGUMENT));
	}

	@ExceptionHandler(MemberNotFoundException.class)
	public ResponseEntity<ApiRes<Void>> handleMemberNotFound(MemberNotFoundException e) {
		return ResponseEntity.status(e.getStatus())
			.body(ApiRes.failure(e.getStatus(), e.getMessage(), e.getErrorCode()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiRes<Void>> handleRuntimeException(RuntimeException e) {
		log.error("Unexpected error: ", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiRes.failure(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR));
	}

}
