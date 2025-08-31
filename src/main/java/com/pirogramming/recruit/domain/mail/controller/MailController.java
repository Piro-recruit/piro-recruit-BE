package com.pirogramming.recruit.domain.mail.controller;

import com.pirogramming.recruit.domain.mail.service.SubscriptionCleanupService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pirogramming.recruit.domain.mail.dto.BulkMailRequestDto;
import com.pirogramming.recruit.domain.mail.dto.SingleMailRequestDto;
import com.pirogramming.recruit.domain.mail.service.MailService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pirogramming.recruit.global.security.RequireRoot;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
@Tag(name = "Mail", description = "메일 발송 API")
@RequireRoot
public class MailController {

	private final MailService MailService;
	private final SubscriptionCleanupService subscriptionCleanupService;

	@Operation(summary = "단일 메일 발송", description = "지정된 수신자에게 AWS SES로 메일을 발송합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "메일 발송 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "500", description = "메일 발송 실패")
	})
	@PostMapping("/single")
	public ResponseEntity<ApiRes<String>> sendSingleMail(@Valid @RequestBody SingleMailRequestDto mailRequest) {
		MailService.sendSingleMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("메일이 성공적으로 발송되었습니다"));
	}

	@Operation(summary = "일괄 메일 발송", description = "등록된 모든 수신자에게 동일한 내용의 메일을 AWS SES로 일괄 발송합니다. 200개 이상일 경우 자동으로 배치 처리됩니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "일괄 메일 발송 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "500", description = "메일 발송 실패")
	})
	@PostMapping("/bulk")
	public ResponseEntity<ApiRes<String>> sendBulkMail(@Valid @RequestBody BulkMailRequestDto mailRequest) {
		MailService.sendBulkMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("일괄 메일이 성공적으로 발송되었습니다"));
	}

	@Operation(summary = "구독자 정리 (수동)", description = "지정된 기간보다 오래된 구독자를 수동으로 정리합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "구독자 정리 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 기간 설정")
	})
	@PostMapping("/cleanup/subscribers")
	public ResponseEntity<ApiRes<String>> cleanupOldSubscribers(
			@Parameter(description = "정리할 기간 (개월, 1-24)", example = "12")
			@RequestParam(defaultValue = "12") int months) {
		int deletedCount = subscriptionCleanupService.cleanupOldSubscriptionsManually(months);
		return ResponseEntity.ok(ApiRes.success(
				String.format("구독자 정리 완료 - %d명의 구독자가 삭제되었습니다", deletedCount)));
	}

	@Operation(summary = "잘못된 이메일 정리", description = "잘못된 형식의 이메일을 가진 구독자를 정리합니다.")
	@PostMapping("/cleanup/invalid-emails")
	public ResponseEntity<ApiRes<String>> cleanupInvalidEmails() {
		int deletedCount = subscriptionCleanupService.cleanupInvalidEmails();
		return ResponseEntity.ok(ApiRes.success(
				String.format("잘못된 이메일 정리 완료 - %d명의 구독자가 삭제되었습니다", deletedCount)));
	}

	@Operation(summary = "중복 구독자 정리", description = "중복된 이메일을 가진 구독자를 정리합니다.")
	@PostMapping("/cleanup/duplicates")
	public ResponseEntity<ApiRes<String>> cleanupDuplicateSubscribers() {
		int deletedCount = subscriptionCleanupService.cleanupDuplicateSubscribers();
		return ResponseEntity.ok(ApiRes.success(
				String.format("중복 구독자 정리 완료 - %d명의 중복 구독자가 삭제되었습니다", deletedCount)));
	}

	@Operation(summary = "구독자 통계 조회", description = "구독자 현황 통계를 조회합니다.")
	@GetMapping("/stats")
	public ResponseEntity<ApiRes<SubscriptionCleanupService.SubscriptionStats>> getSubscriptionStats() {
		SubscriptionCleanupService.SubscriptionStats stats = subscriptionCleanupService.getSubscriptionStats();
		return ResponseEntity.ok(ApiRes.success(stats));
	}
}