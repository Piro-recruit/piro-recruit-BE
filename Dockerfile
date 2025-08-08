# --- 1단계: 빌드 스테이지 (Gradle 빌드)
FROM gradle:8.5-jdk21 AS builder

WORKDIR /home/app

# 1. 의존성 캐시 활용: Gradle 설정 파일만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (실패해도 진행)
RUN gradle dependencies --no-daemon || return 0

# 2. 소스 코드 복사
COPY src ./src

# 3. bootJar 실행 (테스트 제외)
RUN gradle bootJar --no-daemon -x test

# --- 2단계: 실행 스테이지 (최종 배포)
FROM eclipse-temurin:21-jre AS runtime

# 한국어 로케일 설정 및 curl 설치 (헬스체크용)
RUN apt-get update && \
    apt-get install -y locales curl && \
    sed -i '/ko_KR.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen ko_KR.UTF-8 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

# 애플리케이션 사용자 생성 (보안)
RUN groupadd -r app && useradd -r -g app app

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /home/app/build/libs/*.jar app.jar

# 파일 소유권 변경
RUN chown app:app app.jar

# 애플리케이션 사용자로 전환
USER app

# 포트 노출
EXPOSE 8080

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]