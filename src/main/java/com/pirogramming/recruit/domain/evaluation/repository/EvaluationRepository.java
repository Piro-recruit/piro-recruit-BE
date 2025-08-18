package com.pirogramming.recruit.domain.evaluation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pirogramming.recruit.domain.evaluation.entity.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByApplicationId(Long applicationId);

    List<Evaluation> findByEvaluatorId(Long evaluatorId);

    Optional<Evaluation> findByApplicationIdAndEvaluatorId(Long applicationId, Long evaluatorId);

    @Query("SELECT AVG(e.score) FROM Evaluation e WHERE e.application.id = :applicationId")
    Double findAverageScoreByApplicationId(@Param("applicationId") Long applicationId);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.application.id = :applicationId")
    Long countByApplicationId(@Param("applicationId") Long applicationId);

    void deleteByApplicationId(Long applicationId);

    boolean existsByApplicationIdAndEvaluatorId(Long applicationId, Long evaluatorId);
}