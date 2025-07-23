package com.pirogramming.recruit.global.exception.entity_exception;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.exception.code.ErrorCode;

public class MemberNotFoundException extends RecruitException {
	public MemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND);
	}
}