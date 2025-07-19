package com.pirogramming.recruit.global.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.code.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiRes<T> {
	private boolean success;
	private T data;
	private String message;
	private int status;
	private int code;
	private LocalDateTime time;

	public static <T> ApiRes<T> success(T data) {
		return new ApiRes<>(true, data, ErrorCode.SUCCESS.getMessage(), 200, ErrorCode.SUCCESS.getCode(),
			LocalDateTime.now());
	}

	public static <T> ApiRes<T> success(T data, String message) {
		return new ApiRes<>(true, data, message, 200, ErrorCode.SUCCESS.getCode(), LocalDateTime.now());
	}

	public static <T> ApiRes<T> failure(HttpStatus status, ErrorCode errorCode) {
		return new ApiRes<>(false, null, errorCode.getMessage(), status.value(), errorCode.getCode(),
			LocalDateTime.now());
	}

	public static <T> ApiRes<T> failure(HttpStatus status, String message, ErrorCode errorCode) {
		return new ApiRes<>(false, null, message, status.value(), errorCode.getCode(), LocalDateTime.now());
	}
}
