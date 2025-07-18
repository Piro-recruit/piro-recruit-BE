# --- 1단계: 빌드 스테이지 (Gradle 빌드 수행용)
FROM gradle:8.5-jdk21 AS builder

WORKDIR /home/app

COPY . .

RUN gradle build --no-daemon -x test

# --- 2단계: 실행 스테이지 (최종 배포 이미지)
FROM openjdk:21-jdk-slim AS runtime

# 로케일 설정을 위한 패키지 설치
RUN apt-get update && \
    apt-get install -y locales && \
    sed -i '/ko_KR.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen ko_KR.UTF-8 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

WORKDIR /app

COPY --from=builder /home/app/build/libs/recruit-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
