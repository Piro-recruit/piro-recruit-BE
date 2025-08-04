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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {

	private final MailService mailService;

	@PostMapping("/single")
	public ResponseEntity<ApiRes<String>> sendSingleMail(@Valid @RequestBody SingleMailRequestDto mailRequest) {
		mailService.sendSingleMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("메일이 성공적으로 발송되었습니다"));
	}

	@PostMapping("/bulk")
	public ResponseEntity<ApiRes<String>> sendBulkMail(@Valid @RequestBody BulkMailRequestDto mailRequest) {
		mailService.sendBulkMail(mailRequest);
		return ResponseEntity.ok(ApiRes.success("일괄 메일이 성공적으로 발송되었습니다"));
	}
}