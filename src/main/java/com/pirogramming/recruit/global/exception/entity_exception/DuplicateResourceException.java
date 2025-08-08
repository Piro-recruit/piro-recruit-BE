package com.pirogramming.recruit.global.exception.entity_exception;

import com.pirogramming.recruit.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.RecruitException;

public class DuplicateResourceException extends RecruitException {

	/**
	 * ErrorCode를 사용한 중복 예외
	 * @param errorCode 미리 정의된 에러 코드
	 */
	public DuplicateResourceException(ErrorCode errorCode) {
		super(HttpStatus.CONFLICT, errorCode);
	}

	/**
	 * 리소스명과 값을 포함한 중복 예외
	 * @param resourceName 중복된 리소스명 (예: "이메일", "구글폼 응답 ID")
	 * @param value 중복된 값
	 */
	public DuplicateResourceException(String resourceName, String value) {
		super(HttpStatus.CONFLICT, String.format("중복된 %s입니다: %s", resourceName, value));
	}

	/**
	 * 사용자 정의 메시지로 중복 예외
	 * @param message 사용자 정의 오류 메시지
	 */
	public DuplicateResourceException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
