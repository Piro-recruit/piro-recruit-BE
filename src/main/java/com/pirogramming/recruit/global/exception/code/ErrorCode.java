package com.pirogramming.recruit.global.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
	SUCCESS(0, "성공"),
	INVALID_ARGUMENT(1001, "요청 인자값이 유효하지 않습니다."),
	AUTH_FAIL(1002, "인증 실패"),
	NOT_FOUND(1003, "리소스를 찾을 수 없습니다."),
	FORBIDDEN(1004, "권한이 올바르지 않습니다."),
	INTERNAL_ERROR(1000, "서버 내부 오류"),
	OAUTH_AUTH_FAILED(1005, "OAuth 인증에 실패했습니다."),

	// --- 도메인 기반 상세 오류 ---
	MEMBER_NOT_FOUND(2001, "해당 사용자를 찾을 수 없습니다."),


	// -- admin 로그인 관련 오류 --
	INVALID_LOGIN_CODE(3001, "로그인 코드가 유효하지 않습니다."),
	EXPIRED_ADMIN(3002, "해당 관리자는 리쿠르팅 기간이 만료되어 삭제되었습니다."),
	INVALID_REFRESH_TOKEN(3003, "유효하지 않은 리프레시 토큰입니다."),
	ADMIN_NOT_FOUND(3004, "해당 관리자를 찾을 수 없습니다.");




	private final int code;
	private final String message;
}