package com.pirogramming.recruit.global.exception;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.code.ErrorCode;

import lombok.Getter;

@Getter
public class RecruitException extends RuntimeException {
	private final HttpStatus status;
	private final ErrorCode errorCode;

	// 기존 메시지 기반 생성자
	public RecruitException(HttpStatus status, String message) {
		super(message);
		this.status = status;
		this.errorCode = ErrorCode.INTERNAL_ERROR; // 기본값 설정 (혹은 null 허용)
	}

	// 새로 추가: ErrorCode 기반 생성자
	public RecruitException(HttpStatus status, ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.status = status;
		this.errorCode = errorCode;
	}

	// 커스텀 메시지와 ErrorCode를 함께 사용하는 생성자
	public RecruitException(HttpStatus status, ErrorCode errorCode, String customMessage) {
		super(customMessage);
		this.status = status;
		this.errorCode = errorCode;
	}
}
