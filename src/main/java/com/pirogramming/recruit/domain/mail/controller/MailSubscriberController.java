package com.pirogramming.recruit.domain.mail.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.mail.dto.MailSubscriberDto;
import com.pirogramming.recruit.domain.mail.service.MailSubscribeService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pirogramming.recruit.global.security.RequireRoot;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mail/subscribers")
@RequiredArgsConstructor
@Tag(name = "Mail Subscriber", description = "메일 구독자 관리 API")
public class MailSubscriberController {

	private final MailSubscribeService mailSubscribeService;

	@Operation(summary = "구독자 생성", description = "새로운 메일 구독자를 등록합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 등록 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이미 구독 중인 이메일"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping
	public ResponseEntity<ApiRes<MailSubscriberDto.Response>> createSubscriber(
			@Valid @RequestBody MailSubscriberDto.CreateRequest request) {
		MailSubscriberDto.Response response = mailSubscribeService.createSubscriber(request);
		return ResponseEntity.ok(ApiRes.success(response, "구독자가 성공적으로 등록되었습니다"));
	}

	@Operation(summary = "구독자 조회", description = "이메일로 구독자 정보를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 조회 성공"),
		@ApiResponse(responseCode = "404", description = "구독자를 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@GetMapping("/{email}")
	@RequireRoot
	public ResponseEntity<ApiRes<MailSubscriberDto.Response>> getSubscriber(@PathVariable String email) {
		MailSubscriberDto.Response response = mailSubscribeService.getSubscriber(email);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@Operation(summary = "구독자 조회", description = "페이징으로 구독자 목록을 조회합니다. 이메일 검색 기능을 포함합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 목록 조회 성공"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@GetMapping
	@RequireRoot
	public ResponseEntity<ApiRes<Page<MailSubscriberDto.Response>>> getAllSubscribers(
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String email) {
		Page<MailSubscriberDto.Response> response = mailSubscribeService.getAllSubscribers(pageable, email);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@Operation(summary = "구독자 수정", description = "구독자의 이메일을 수정합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이미 사용 중인 이메일"),
		@ApiResponse(responseCode = "404", description = "구독자를 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PutMapping("/{email}")
	@RequireRoot
	public ResponseEntity<ApiRes<MailSubscriberDto.Response>> updateSubscriber(
			@PathVariable String email,
			@Valid @RequestBody MailSubscriberDto.UpdateRequest request) {
		MailSubscriberDto.Response response = mailSubscribeService.updateSubscriber(email, request);
		return ResponseEntity.ok(ApiRes.success(response, "구독자 정보가 성공적으로 수정되었습니다"));
	}

	@Operation(summary = "구독자 삭제", description = "구독자를 삭제합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 삭제 성공"),
		@ApiResponse(responseCode = "404", description = "구독자를 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@DeleteMapping("/{email}")
	@RequireRoot
	public ResponseEntity<ApiRes<String>> deleteSubscriber(@PathVariable String email) {
		mailSubscribeService.deleteSubscriber(email);
		return ResponseEntity.ok(ApiRes.success("구독자가 성공적으로 삭제되었습니다"));
	}

	@Operation(summary = "구독자 수 조회", description = "전체 구독자 수를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구독자 수 조회 성공"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@GetMapping("/count")
	@RequireRoot
	public ResponseEntity<ApiRes<Long>> getSubscriberCount() {
		long count = mailSubscribeService.getSubscriberCount();
		return ResponseEntity.ok(ApiRes.success(count));
	}
}