# AI Summary API 명세서

대학생 IT 개발 동아리 지원서 AI 자동 분석 및 평가 API

## 목차

- [개요](#개요)
- [인증 및 권한](#인증-및-권한)
- [API 엔드포인트](#api-엔드포인트)
  - [AI 지원서 분석](#ai-지원서-분석)
  - [AI 요약 결과 조회](#ai-요약-결과-조회)
  - [모니터링 및 관리](#모니터링-및-관리)
- [데이터 모델](#데이터-모델)
- [AI 분석 기준](#ai-분석-기준)
- [성능 및 제한사항](#성능-및-제한사항)
- [오류 처리](#오류-처리)

## 개요

AI Summary API는 OpenAI를 활용하여 대학생 IT 개발 동아리 지원서를 자동으로 분석하고 평가하는 시스템입니다. 협업, 성장, 열정을 기준으로 지원서를 분석하며, 전공/비전공 구분 없이 학습 의지와 협업 능력을 중점적으로 평가합니다.

**Base URL:** `/api/ai-summary`

## 인증 및 권한

- **모든 엔드포인트**: Admin 권한 필요 (`@RequireAdmin`)
- **인증 방식**: JWT 토큰 기반 인증
- **API 키**: OpenAI API 키 필요 (환경변수로 관리)

## API 엔드포인트

### AI 지원서 분석

#### GET /test
더미 지원서 분석 테스트 (개발/테스트 용도)

더미 데이터를 사용하여 AI 분석 기능을 테스트합니다.

**요청 헤더**
```
Authorization: Bearer {JWT_TOKEN}
```

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "questionSummaries": [
      {
        "question": "본인의 가치관, 성격 등을 포함한 자기소개",
        "aiSummary": "컴퓨터공학과 3학년으로 Spring Boot 기반 프로젝트 경험이 있으며, 팀 프로젝트를 통해 협업 능력을 보여주었습니다."
      }
    ],
    "scoreOutOf100": 78,
    "scoreReason": "열정 및 학습 태도: 30/40점 - 프로젝트 경험과 학습 계획이 구체적임. 협업 잠재력: 22/30점 - 팀 프로젝트 경험 보유."
  },
  "message": "지원서 요약이 성공적으로 생성되었습니다.",
  "status": 200
}
```

**Fallback 응답 (API 오류 시)**
```json
{
  "success": true,
  "data": {
    "questionSummaries": [
      {
        "question": "API 오류",
        "aiSummary": "OpenAI API 호출 중 오류가 발생하여 자동 분석을 완료할 수 없습니다."
      }
    ],
    "scoreOutOf100": 0,
    "scoreReason": "OpenAI 서비스 오류로 인해 점수 산출을 완료할 수 없습니다."
  },
  "message": "지원서 요약이 성공적으로 생성되었습니다."
}
```

#### POST /analyze
동적 지원서 분석

실제 지원서 문항과 답변을 받아 AI로 분석합니다.

**요청 본문**
```json
[
  {
    "question": "1. 본인의 가치관, 성격 등을 포함한 자기소개와 피로그래밍에 지원한 동기 및 목표를 적어주세요",
    "answer": "안녕하세요. 컴퓨터공학을 전공하고 있는 학생입니다. 웹 개발에 관심이 많아 피로그래밍에 지원하게 되었습니다."
  },
  {
    "question": "2. 협업을 진행하며 겪었던 어려움과 이를 극복한 경험을 구체적으로 작성해 주세요",
    "answer": "팀 프로젝트에서 의견 충돌이 있었지만, 서로의 의견을 듣고 타협점을 찾아 해결했습니다."
  },
  {
    "question": "3. 평소 개발을 공부하며 만들어 보고 싶었던 웹사이트는 어떻게 되나요?",
    "answer": "사용자들이 쉽게 정보를 공유할 수 있는 커뮤니티 사이트를 만들어보고 싶습니다."
  }
]
```

**제약사항**
- 최대 20개 질문까지 가능
- 질문당 최대 500자, 답변당 최대 5000자
- 45초 타임아웃 적용

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "questionSummaries": [
      {
        "question": "1. 본인의 가치관, 성격 등을 포함한 자기소개와 피로그래밍에 지원한 동기 및 목표를 적어주세요",
        "aiSummary": "컴퓨터공학 전공 학생으로 웹 개발에 대한 관심이 높고, 피로그래밍을 통한 성장 의지를 보여줍니다."
      },
      {
        "question": "2. 협업을 진행하며 겪었던 어려움과 이를 극복한 경험을 구체적으로 작성해 주세요",
        "aiSummary": "팀 프로젝트에서 발생한 의견 충돌을 원만하게 해결한 경험으로 협업 능력을 입증했습니다."
      }
    ],
    "scoreOutOf100": 82,
    "scoreReason": "열정 및 학습 태도: 32/40점 - 구체적인 목표와 학습 의지 확인. 협업 잠재력: 25/30점 - 갈등 해결 경험 보유. 기술적 기반: 18/20점 - 웹 개발 관심사 명확. 성장 마인드셋: 7/10점 - 지속적 개선 의지."
  },
  "message": "동적 지원서 요약이 성공적으로 생성되었습니다.",
  "status": 200
}
```

**오류 응답**
- **400 Bad Request**: 잘못된 요청 형식, 유효성 검증 실패
- **408 Request Timeout**: AI 분석 시간 초과 (45초)
- **500 Internal Server Error**: AI 분석 중 오류 발생

#### POST /batch-analyze
배치 지원서 분석 (비동기)

다중 지원서를 병렬로 처리하여 성능을 향상시킵니다.

**요청 본문**
```json
[
  [
    {
      "question": "질문1",
      "answer": "답변1"
    }
  ],
  [
    {
      "question": "질문1",
      "answer": "답변1"
    }
  ]
]
```

**제약사항**
- 최대 10개 지원서까지 동시 처리
- 각 지원서당 최대 20개 질문
- 2분 타임아웃 적용

**성공 응답 (200)**
```json
{
  "success": true,
  "data": [
    {
      "questionSummaries": [...],
      "scoreOutOf100": 82,
      "scoreReason": "..."
    },
    {
      "questionSummaries": [...],
      "scoreOutOf100": 75,
      "scoreReason": "..."
    }
  ],
  "message": "2개 지원서 배치 분석이 완료되었습니다.",
  "status": 200
}
```

### AI 요약 결과 조회

#### GET /webhook-application/{webhookApplicationId}
WebhookApplication ID로 요약 조회

**경로 매개변수**
- `webhookApplicationId`: WebhookApplication ID (Long)

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "webhookApplicationId": 1,
    "questionSummaries": [...],
    "scoreOutOf100": 82,
    "scoreReason": "...",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  "message": "요약 결과 조회 성공"
}
```

#### GET /all
모든 AI 요약 조회

모든 AI 요약 결과를 최신순으로 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "webhookApplicationId": 1,
      "questionSummaries": [...],
      "scoreOutOf100": 82,
      "createdAt": "2024-01-01T10:00:00"
    },
    {
      "id": 2,
      "webhookApplicationId": 2,
      "questionSummaries": [...],
      "scoreOutOf100": 75,
      "createdAt": "2024-01-01T09:00:00"
    }
  ],
  "message": "2개의 AI 요약을 조회했습니다."
}
```

#### GET /{summaryId}
AI 요약 ID로 조회

**경로 매개변수**
- `summaryId`: AI 요약 ID (Long)

### 모니터링 및 관리

**Base URL:** `/api/ai-summary/monitoring`

#### GET /batch-status
배치 처리 상태 조회

AI 요약 배치 처리의 현재 상태와 통계를 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "isEnabled": true,
    "processingCount": 5,
    "successCount": 23,
    "failureCount": 2,
    "retryCount": 1,
    "lastProcessedAt": "2024-01-01T10:00:00",
    "averageProcessingTime": 1500
  },
  "message": "배치 처리 상태를 성공적으로 조회했습니다."
}
```

#### GET /openai-stats
OpenAI API 통계 조회

OpenAI API 호출 통계 및 동시성 제어 상태를 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "totalRequests": 150,
    "successRequests": 142,
    "failedRequests": 8,
    "averageResponseTime": 2300,
    "currentConcurrency": 3,
    "maxConcurrency": 10,
    "rateLimitRemaining": 47,
    "lastRequestAt": "2024-01-01T10:00:00"
  },
  "message": "OpenAI API 통계를 성공적으로 조회했습니다."
}
```

#### GET /cache-stats
캐시 통계 조회

AI 요약 캐시 상태를 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "hitCount": 45,
    "missCount": 12,
    "hitRate": 0.789,
    "evictionCount": 3,
    "cacheSize": 42,
    "maxCacheSize": 100,
    "averageLoadTime": 850
  },
  "message": "캐시 통계를 성공적으로 조회했습니다."
}
```

#### GET /dashboard
종합 대시보드

AI 요약 시스템 전체 상태를 한 번에 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "batchProcessing": {
      "isEnabled": true,
      "processingCount": 5,
      "successCount": 23
    },
    "openAiApi": {
      "totalRequests": 150,
      "successRequests": 142,
      "currentConcurrency": 3
    },
    "cache": {
      "hitCount": 45,
      "hitRate": 0.789,
      "cacheSize": 42
    },
    "timestamp": "2024-01-01T10:00:00"
  },
  "message": "대시보드 데이터를 성공적으로 조회했습니다."
}
```

#### POST /trigger-batch
즉시 배치 처리 실행

대기 중인 AI 요약 작업을 즉시 처리합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": "배치 처리가 시작되었습니다.",
  "message": "배치 처리를 수동으로 실행했습니다."
}
```

#### POST /clear-cache
캐시 초기화

AI 요약 캐시를 모두 초기화합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": "캐시가 초기화되었습니다.",
  "message": "캐시를 성공적으로 초기화했습니다."
}
```

## 데이터 모델

### ApplicationQuestionDto
```json
{
  "question": "String (required, max 500자) - 지원서 문항",
  "answer": "String (required, max 5000자) - 문항에 대한 답변"
}
```

### ApplicationSummaryDto
```json
{
  "questionSummaries": [
    {
      "question": "String - 질문 내용",
      "aiSummary": "String - AI 요약 결과"
    }
  ],
  "scoreOutOf100": "Integer (0~100) - 100점 만점 평가 점수",
  "scoreReason": "String - 점수 산출 근거 (객관적)"
}
```

### ApplicationSummary (Entity)
```json
{
  "id": "Long - AI 요약 ID",
  "webhookApplicationId": "Long - 연결된 지원서 ID",
  "questionSummaries": "List<QuestionSummaryDto> - 질문별 AI 요약",
  "scoreOutOf100": "Integer - 평가 점수",
  "scoreReason": "String - 점수 근거",
  "createdAt": "LocalDateTime - 생성 시간",
  "updatedAt": "LocalDateTime - 수정 시간"
}
```

### BatchProcessingStats
```json
{
  "isEnabled": "Boolean - 배치 처리 활성화 여부",
  "processingCount": "Integer - 현재 처리 중인 작업 수",
  "successCount": "Long - 성공한 작업 수",
  "failureCount": "Long - 실패한 작업 수",
  "retryCount": "Long - 재시도한 작업 수",
  "lastProcessedAt": "LocalDateTime - 마지막 처리 시간",
  "averageProcessingTime": "Long - 평균 처리 시간 (ms)"
}
```

### CacheStats
```json
{
  "hitCount": "Long - 캐시 히트 수",
  "missCount": "Long - 캐시 미스 수", 
  "hitRate": "Double - 캐시 히트율",
  "evictionCount": "Long - 캐시 제거 수",
  "cacheSize": "Integer - 현재 캐시 크기",
  "maxCacheSize": "Integer - 최대 캐시 크기",
  "averageLoadTime": "Double - 평균 로드 시간 (ms)"
}
```

## AI 분석 기준

### 평가 영역 및 배점
1. **열정 및 학습 태도** (40점)
   - 지속적인 학습 의지
   - 구체적인 목표 설정
   - 동아리 활동에 대한 열정

2. **협업 잠재력** (30점)
   - 팀워크 경험
   - 의사소통 능력
   - 갈등 해결 경험

3. **기술적 기반** (20점)
   - 프로그래밍 기초 소양
   - 기술 학습 경험
   - 프로젝트 경험

4. **성장 마인드셋** (10점)
   - 피드백 수용 자세
   - 지속적 개선 의지
   - 도전 정신

### 분석 특징
- **전공/비전공 구분 없음**: 학습 의지와 협업 능력 중심 평가
- **객관적 근거 제시**: 점수 산출 근거를 상세히 설명
- **개별 문항 분석**: 각 질문별 핵심 내용 요약
- **대학생 맞춤**: 대학생 IT 개발 동아리 환경에 특화된 분석

## 성능 및 제한사항

### API 제한사항
- **동시 처리**: OpenAI API 동시성 제어 (Semaphore 10개)
- **요청 크기**: 질문 최대 20개, 답변 최대 5000자
- **배치 크기**: 최대 10개 지원서 동시 처리
- **타임아웃**: 단일 분석 45초, 배치 분석 2분

### 성능 최적화
- **캐시 시스템**: 동일한 요청에 대한 결과 캐싱
- **비동기 처리**: CompletableFuture 활용한 병렬 처리
- **배치 처리**: 대량 지원서 처리를 위한 스케줄링
- **Fallback 시스템**: OpenAI API 오류 시 안전한 응답 제공

### 모니터링
- **실시간 통계**: API 호출, 성공/실패, 응답 시간 추적
- **캐시 성능**: 히트율, 로드 시간 모니터링
- **배치 상태**: 처리 현황 및 오류 추적
- **대시보드**: 전체 시스템 상태 종합 모니터링

## 오류 처리

### 오류 코드 및 메시지

| 상태 코드 | 오류 유형 | 설명 | 예시 메시지 |
|---------|---------|------|------------|
| 400 | Bad Request | 잘못된 요청 형식 | "질문은 최대 20개까지 가능합니다" |
| 401 | Unauthorized | 인증되지 않은 요청 | "Admin 권한이 필요합니다" |
| 404 | Not Found | 리소스를 찾을 수 없음 | "AI 요약 ID 999를 찾을 수 없습니다" |
| 408 | Request Timeout | 처리 시간 초과 | "AI 분석 시간이 초과되었습니다" |
| 500 | Internal Server Error | 서버 내부 오류 | "AI 분석 중 오류가 발생했습니다" |

### Fallback 처리
OpenAI API 오류 시 안전한 기본 응답을 제공합니다.

```json
{
  "questionSummaries": [
    {
      "question": "API 오류",
      "aiSummary": "OpenAI API 호출 중 오류가 발생하여 자동 분석을 완료할 수 없습니다."
    }
  ],
  "scoreOutOf100": 0,
  "scoreReason": "OpenAI 서비스 오류로 인해 점수 산출을 완료할 수 없습니다."
}
```

### 유효성 검증
- **필수 필드 검증**: question, answer 필수
- **길이 제한**: 질문 500자, 답변 5000자
- **개수 제한**: 질문 최대 20개, 배치 최대 10개
- **형식 검증**: JSON 구조 및 데이터 타입 확인

## 사용 예시

### 1. 단일 지원서 분석
```bash
# 지원서 분석 요청
POST /api/ai-summary/analyze
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

[
  {
    "question": "자기소개와 지원 동기를 작성해주세요",
    "answer": "컴퓨터공학과 학생으로..."
  }
]
```

### 2. 배치 분석
```bash
# 여러 지원서 동시 분석
POST /api/ai-summary/batch-analyze
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

[
  [{"question": "...", "answer": "..."}],
  [{"question": "...", "answer": "..."}]
]
```

### 3. 결과 조회 및 모니터링
```bash
# 모든 AI 요약 조회
GET /api/ai-summary/all

# 시스템 상태 확인
GET /api/ai-summary/monitoring/dashboard

# 배치 처리 수동 실행
POST /api/ai-summary/monitoring/trigger-batch
```