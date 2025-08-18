package com.pirogramming.recruit.domain.evaluation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import com.pirogramming.recruit.domain.evaluation.dto.ApplicationEvaluationSummaryResponse;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationRequest;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationResponse;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationUpdateRequest;
import com.pirogramming.recruit.domain.evaluation.entity.Evaluation;
import com.pirogramming.recruit.domain.evaluation.repository.EvaluationRepository;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.repository.WebhookApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final WebhookApplicationRepository webhookApplicationRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public EvaluationResponse createEvaluation(EvaluationRequest request, Long evaluatorId) {
        WebhookApplication application = webhookApplicationRepository.findById(request.getApplicationId())
            .orElseThrow(() -> new IllegalArgumentException("지원서를 찾을 수 없습니다: " + request.getApplicationId()));

        Admin evaluator = adminRepository.findById(evaluatorId)
            .orElseThrow(() -> new IllegalArgumentException("평가자를 찾을 수 없습니다: " + evaluatorId));

        if (evaluationRepository.existsByApplicationIdAndEvaluatorId(request.getApplicationId(), evaluatorId)) {
            throw new IllegalStateException("이미 해당 지원서에 대한 평가를 등록하셨습니다");
        }

        Evaluation evaluation = Evaluation.builder()
            .application(application)
            .evaluator(evaluator)
            .score(request.getScore())
            .comment(request.getComment())
            .build();

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        return new EvaluationResponse(savedEvaluation);
    }

    @Transactional
    public EvaluationResponse updateEvaluation(Long evaluationId, EvaluationUpdateRequest request, Long evaluatorId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("평가를 찾을 수 없습니다: " + evaluationId));

        if (!evaluation.getEvaluator().getId().equals(evaluatorId)) {
            throw new IllegalStateException("본인이 작성한 평가만 수정할 수 있습니다");
        }

        evaluation.updateEvaluation(request.getScore(), request.getComment());
        return new EvaluationResponse(evaluation);
    }

    @Transactional
    public void deleteEvaluation(Long evaluationId, Long evaluatorId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("평가를 찾을 수 없습니다: " + evaluationId));

        if (!evaluation.getEvaluator().getId().equals(evaluatorId)) {
            throw new IllegalStateException("본인이 작성한 평가만 삭제할 수 있습니다");
        }

        evaluationRepository.delete(evaluation);
    }

    public EvaluationResponse getEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new IllegalArgumentException("평가를 찾을 수 없습니다: " + evaluationId));

        return new EvaluationResponse(evaluation);
    }

    public List<EvaluationResponse> getEvaluationsByApplication(Long applicationId) {
        List<Evaluation> evaluations = evaluationRepository.findByApplicationId(applicationId);
        return evaluations.stream()
            .map(EvaluationResponse::new)
            .collect(Collectors.toList());
    }

    public List<EvaluationResponse> getEvaluationsByEvaluator(Long evaluatorId) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
        return evaluations.stream()
            .map(EvaluationResponse::new)
            .collect(Collectors.toList());
    }

    public ApplicationEvaluationSummaryResponse getApplicationEvaluationSummary(Long applicationId) {
        WebhookApplication application = webhookApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("지원서를 찾을 수 없습니다: " + applicationId));

        List<EvaluationResponse> evaluations = getEvaluationsByApplication(applicationId);
        Double averageScore = evaluationRepository.findAverageScoreByApplicationId(applicationId);
        Long evaluationCount = evaluationRepository.countByApplicationId(applicationId);

        return new ApplicationEvaluationSummaryResponse(
            applicationId,
            application.getApplicantName(),
            averageScore,
            evaluationCount,
            evaluations
        );
    }

    @Transactional
    public void deleteEvaluationsByApplication(Long applicationId) {
        evaluationRepository.deleteByApplicationId(applicationId);
    }
}