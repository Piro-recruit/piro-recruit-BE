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
import com.pirogramming.recruit.domain.evaluation.exception.EvaluationException;
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
            .orElseThrow(() -> EvaluationException.applicationNotFound(request.getApplicationId()));

        Admin evaluator = adminRepository.findById(evaluatorId)
            .orElseThrow(() -> EvaluationException.evaluatorNotFound(evaluatorId));

        if (evaluationRepository.existsByApplicationIdAndEvaluatorId(request.getApplicationId(), evaluatorId)) {
            throw EvaluationException.alreadyExists(request.getApplicationId(), evaluator.getIdentifierName());
        }

        Evaluation evaluation = Evaluation.builder()
            .application(application)
            .evaluatorId(evaluatorId)
            .evaluatorName(evaluator.getIdentifierName())
            .score(request.getScore())
            .comment(request.getComment())
            .build();

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        updateApplicationAverageScore(application.getId());
        return new EvaluationResponse(savedEvaluation);
    }

    @Transactional
    public EvaluationResponse updateEvaluation(Long evaluationId, EvaluationUpdateRequest request, Long evaluatorId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> EvaluationException.notFound(evaluationId));

        if (!evaluation.getEvaluatorId().equals(evaluatorId)) {
            throw EvaluationException.permissionDenied(evaluationId, evaluation.getEvaluatorName());
        }

        evaluation.updateEvaluation(request.getScore(), request.getComment());
        updateApplicationAverageScore(evaluation.getApplication().getId());
        return new EvaluationResponse(evaluation);
    }

    @Transactional
    public void deleteEvaluation(Long evaluationId, Long evaluatorId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> EvaluationException.notFound(evaluationId));

        if (!evaluation.getEvaluatorId().equals(evaluatorId)) {
            throw EvaluationException.permissionDenied(evaluationId, evaluation.getEvaluatorName());
        }

        Long applicationId = evaluation.getApplication().getId();
        evaluationRepository.delete(evaluation);
        updateApplicationAverageScore(applicationId);
    }

    public EvaluationResponse getEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> EvaluationException.notFound(evaluationId));

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
            .orElseThrow(() -> EvaluationException.applicationNotFound(applicationId));

        List<EvaluationResponse> evaluations = getEvaluationsByApplication(applicationId);

        return new ApplicationEvaluationSummaryResponse(
            applicationId,
            application.getApplicantName(),
            application.getAverageScore(),
            application.getEvaluationCount().longValue(),
            evaluations
        );
    }

    @Transactional
    public void deleteEvaluationsByApplication(Long applicationId) {
        evaluationRepository.deleteByApplicationId(applicationId);
    }

    private void updateApplicationAverageScore(Long applicationId) {
        WebhookApplication application = webhookApplicationRepository.findById(applicationId)
            .orElseThrow(() -> EvaluationException.applicationNotFound(applicationId));

        Double averageScore = evaluationRepository.findAverageScoreByApplicationId(applicationId);
        Long evaluationCount = evaluationRepository.countByApplicationId(applicationId);

        application.updateEvaluationStatistics(averageScore, evaluationCount.intValue());
        webhookApplicationRepository.save(application);
    }
}