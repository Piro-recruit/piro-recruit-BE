# --- 1단계: 빌드 스테이지 (Gradle 빌드)
FROM gradle:8.5-jdk21 AS builder

WORKDIR /home/app

# 1. 의존성 캐시 활용: Gradle 설정 파일만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle dependencies --no-daemon || return 0

# 2. 나머지 소스 복사
COPY . .

# 3. bootJar 실행 (테스트 제외)
RUN gradle bootJar --no-daemon -x test

# --- 2단계: 실행 스테이지 (최종 배포)
FROM openjdk:21-jdk-slim AS runtime

RUN apt-get update && \
    apt-get install -y locales && \
    sed -i '/ko_KR.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen ko_KR.UTF-8 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

WORKDIR /app

# JAR 이름 하드코딩 대신 *.jar 로 복사
COPY --from=builder /home/app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
