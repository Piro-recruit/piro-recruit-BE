package com.pirogramming.recruit.domain.mail.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	private final MailService mailService;

	@Operation(summary = "단일 메일 발송", description = "지정된 수신자에게 메일을 발송합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "메일 발송 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
		@ApiResponse(responseCode = "500", description = "메일 발송 실패")
	})
	@PostMapping("/single")
	public ResponseEntity<ApiRes<String>> sendSingleMail(@Valid @RequestBody SingleMailRequestDto mailRequest) {
		mailService.sendSingleMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("메일이 성공적으로 발송되었습니다"));
	}

	@Operation(summary = "일괄 메일 발송", description = "등록된 모든 수신자에게 동일한 내용의 메일을 일괄 발송합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "일괄 메일 발송 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
		@ApiResponse(responseCode = "500", description = "메일 발송 실패")
	})
	@PostMapping("/bulk")
	public ResponseEntity<ApiRes<String>> sendBulkMail(@Valid @RequestBody BulkMailRequestDto mailRequest) {
		mailService.sendBulkMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("일괄 메일이 성공적으로 발송되었습니다"));
	}
}