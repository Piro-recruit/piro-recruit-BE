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

	// --- 구글폼 관련 오류 ---
	GOOGLE_FORM_NOT_FOUND(2201, "해당 구글 폼을 찾을 수 없습니다."),
	GOOGLE_FORM_NOT_ACTIVE(2202, "현재 활성화된 구글 폼이 없습니다."),
	GOOGLE_FORM_DUPLICATE_FORM_ID(2203, "이미 등록된 구글폼 ID입니다."),
	GOOGLE_FORM_ACTIVE_CANNOT_DELETE(2204, "현재 활성화된 구글 폼은 삭제할 수 없습니다."),

	// -- admin 로그인 관련 오류 --
	INVALID_LOGIN_CODE(3001, "로그인 코드가 유효하지 않습니다."),
	EXPIRED_ADMIN(3002, "해당 관리자는 리쿠르팅 기간이 만료되어 삭제되었습니다."),
	INVALID_REFRESH_TOKEN(3003, "유효하지 않은 리프레시 토큰입니다."),
	ADMIN_NOT_FOUND(3004, "해당 관리자를 찾을 수 없습니다."),
	INVALID_API_KEY(3005, "유효하지 않은 API Key입니다."),



	// --- 웹훅 관련 오류 (새로 추가) ---
	WEBHOOK_DUPLICATE_FORM_RESPONSE(3001, "이미 처리된 구글 폼 응답입니다."),
	WEBHOOK_DUPLICATE_EMAIL(3002, "이미 지원서가 제출된 이메일입니다."),
	WEBHOOK_APPLICATION_NOT_FOUND(3003, "해당 지원서를 찾을 수 없습니다."),
	WEBHOOK_PROCESSING_FAILED(3004, "웹훅 데이터 처리에 실패했습니다."),

	// --- 평가 관련 오류 ---
	EVALUATION_NOT_FOUND(4001, "해당 평가를 찾을 수 없습니다."),
	EVALUATION_ALREADY_EXISTS(4002, "이미 해당 지원서에 대한 평가를 등록하셨습니다."),
	EVALUATION_PERMISSION_DENIED(4003, "본인이 작성한 평가만 수정/삭제할 수 있습니다."),
	EVALUATION_INVALID_SCORE(4004, "평가 점수는 0점 이상 100점 이하여야 합니다."),
	EVALUATION_APPLICATION_NOT_FOUND(4005, "평가하려는 지원서를 찾을 수 없습니다."),
	EVALUATION_EVALUATOR_NOT_FOUND(4006, "평가자 정보를 찾을 수 없습니다.");

	private final int code;
	private final String message;
}