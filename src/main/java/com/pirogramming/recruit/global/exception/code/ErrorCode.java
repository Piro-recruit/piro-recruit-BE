package com.pirogramming.recruit.global.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

	// --- 리크루팅 관련 오류 ---
	RECRUITMENT_NOT_FOUND(2101, "해당 리크루팅을 찾을 수 없습니다."),
	RECRUITMENT_NOT_ACTIVE(2102, "현재 모집 중인 리크루팅이 없습니다."),
	RECRUITMENT_PERIOD_EXPIRED(2103, "지원 기간이 종료되었습니다."),
	RECRUITMENT_PERIOD_NOT_STARTED(2104, "지원 기간이 아직 시작되지 않았습니다."),

	// --- 웹훅 관련 오류 (새로 추가) ---
	WEBHOOK_DUPLICATE_FORM_RESPONSE(3001, "이미 처리된 구글 폼 응답입니다."),
	WEBHOOK_DUPLICATE_EMAIL(3002, "이미 지원서가 제출된 이메일입니다."),
	WEBHOOK_APPLICATION_NOT_FOUND(3003, "해당 지원서를 찾을 수 없습니다."),
	WEBHOOK_PROCESSING_FAILED(3004, "웹훅 데이터 처리에 실패했습니다.");

	private final int code;
	private final String message;
}