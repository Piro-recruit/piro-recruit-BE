package com.pirogramming.recruit.global.exception.entity_exception;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.RecruitException;

public class DuplicateResourceException extends RecruitException {
	public DuplicateResourceException(String resourceName, String value) {
		super(HttpStatus.CONFLICT, String.format("중복된 %s입니다: %s", resourceName, value));
	}

	public DuplicateResourceException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
