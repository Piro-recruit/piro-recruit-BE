package com.pirogramming.recruit.domain.ai_summary.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.ai_summary.port.LlmClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationProcessingService {
	private final LlmClient llmClient;



}
