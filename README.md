## 실행 명령어  
 
```
# 실행
docker compose up --build

# 중지
docker compose down

# + 볼륨 포함 정리 시 (DB 데이터까지 삭제)
docker compose down -v


```
## **1. 커밋 컨벤션**

커밋 메시지는 다음 규칙을 따릅니다:

```
[타입] 모듈명: 메시지 내용
```

- **타입** (소문자):
    - feat: 새로운 기능 추가
    - fix: 버그 수정
    - docs: 문서 수정
    - style: 코드 포맷팅, 세미콜론 누락 등 (코드 변경 없음)
    - refactor: 리팩토링 (기능 추가/변경 없음)
    - test: 테스트 코드 추가/수정
    - chore: 빌드 작업, 의존성 관리 등 기타 작업

예시:

```
feat member: 회원 가입 기능 구현
fix auth: 로그인 시 인증 오류 수정
```

## **2. 패키지 구조 컨벤션 (DDD 기반)**

프로젝트는 DDD(Domain-Driven Design) 기반의 패키지 구조를 따릅니다:

```
2. 패키지 구조 컨벤션

com.example.project
│
├── domain
│   ├── [도메인명]
│   │   ├── service
│   │   ├── entity
│   │   ├── repository
│   │   ├── controller
│   │   └── dto
├── global
│   ├── config
│   └── ...

domain: 도메인별 패키지 (ex: member, board)

service: 비즈니스 로직

entity: JPA Entity 등 도메인 모델

repository: 데이터 액세스

controller: API 컨트롤러

dto: 요청/응답 DTO

global: 공통 설정, 유틸리티 등
```

## **3. 코드 포맷터**

- [NAVER Intellij Java Formatter](https://github.com/naver/hackday-conventions-java/blob/master/rule-config/naver-intellij-formatter.xml)
- [적용법](https://eroul-ri.tistory.com/26)

---

## **4. 브랜치 컨벤션**

브랜치는 이슈 번호와 연결하여 생성

형식: [이슈번호]-feature/설명, [이슈번호]-fix/설명 등

- 이슈 생성 -> development -> create a branch

예시:

```
23-feature/회원가입-API

45-fix/로그인-버그수정
```

## **5. PR 잘 올리기**

## **6. Swagger 문서화 꾸준히**

## **7. 응답 규격화도 하기**
