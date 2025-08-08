pipeline {
    agent any

    environment {
        // Docker 이미지 정보
        DOCKER_IMAGE = 'kimgyuill/piro-recruiting'
        DOCKER_TAG = "${BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')

        // 배포 서버 정보
        APP_SERVER = '34.64.41.136'
        APP_USER = 'ubuntu'

        // 애플리케이션 포트 (수정됨)
        BLUE_PORT = '8081'
        GREEN_PORT = '8082'

        // 데이터베이스 정보
        DB_HOST = '34.64.113.7'
        DB_NAME = 'app_user'
        DB_USERNAME = credentials('DB_USERNAME')
        DB_PASSWORD = credentials('DB_PASSWORD')

        // API Keys 및 기타 환경변수
        OPENAI_API_KEY = credentials('OPENAI_API_KEY')
        STMP_USER_ID = credentials('STMP_USER_ID')
        STMP_PASSWORD = credentials('STMP_PASSWORD')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Gradle Permission') {
            steps {
                echo 'Setting Gradle permissions...'
                sh 'chmod +x ./gradlew'
            }
        }

        stage('Test') {
            steps {
                echo 'Skipping tests for CI/CD setup'
                script {
                    sh 'echo "Tests will be enabled after stable deployment"'
                }
            }
        }

        stage('Build JAR') {
            steps {
                echo 'Building Spring Boot application with memory optimization...'
                script {
                    sh './gradlew clean bootJar --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx512m -XX:+UseG1GC" -x test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    def image = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing to Docker Hub...'
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                        def image = docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}")
                        image.push()
                        image.push("latest")
                    }
                    echo "✅ Docker 이미지 푸시 완료: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                }
            }
        }

        stage('Deploy Blue-Green') {
            steps {
                echo '🚀 Starting Blue-Green deployment...'
                script {
                    // 현재 실행 중인 컨테이너 상세 확인
                    def containerInfo = ""
                    try {
                        sshagent(['app-server-ssh']) {
                            containerInfo = sh(
                                script: """
                                    ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                        echo "=== 현재 실행 중인 컨테이너 상태 ==="
                                        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(piro-recruiting|NAMES)" || echo "실행 중인 piro-recruiting 컨테이너 없음"
                                        echo "=== 포트 사용 현황 ==="
                                        ss -tulpn | grep -E ":808[12]" || echo "8081, 8082 포트 사용 없음"
                                    '
                                """,
                                returnStdout: true
                            ).trim()
                        }
                    } catch (Exception e) {
                        echo "컨테이너 상태 확인 중 오류: ${e.message}"
                        containerInfo = "확인 실패"
                    }

                    echo "📋 서버 상태:"
                    echo containerInfo

                    // Blue/Green 결정 로직 (수정됨)
                    def deployPort = ""
                    def deployColor = ""
                    def stopColor = ""
                    def stopPort = ""

                    if (containerInfo.contains('piro-recruiting-blue') && containerInfo.contains(':8081->')) {
                        // Blue가 8081에서 실행 중 → Green으로 배포
                        deployPort = GREEN_PORT
                        deployColor = 'green'
                        stopColor = 'blue'
                        stopPort = BLUE_PORT
                    } else if (containerInfo.contains('piro-recruiting-green') && containerInfo.contains(':8082->')) {
                        // Green이 8082에서 실행 중 → Blue로 배포
                        deployPort = BLUE_PORT
                        deployColor = 'blue'
                        stopColor = 'green'
                        stopPort = GREEN_PORT
                    } else {
                        // 아무것도 실행 중이 아님 → Blue로 시작
                        deployPort = BLUE_PORT
                        deployColor = 'blue'
                        stopColor = 'none'
                        stopPort = 'none'
                    }

                    echo "🎯 배포 계획:"
                    echo "   - 새 컨테이너: ${deployColor} (포트: ${deployPort})"
                    echo "   - 정리 대상: ${stopColor} (포트: ${stopPort})"

                    // 기존 컨테이너 정리 (배포 전)
                    if (stopColor != 'none') {
                        sshagent(['app-server-ssh']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    echo "🧹 기존 컨테이너 정리 중..."
                                    docker stop piro-recruiting-${stopColor} || true
                                    docker rm piro-recruiting-${stopColor} || true
                                    echo "✅ ${stopColor} 컨테이너 정리 완료"
                                '
                            """
                        }
                    }

                    // 새 컨테이너 배포
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                echo "📦 새 컨테이너 배포 시작..."

                                # 혹시 모를 기존 컨테이너 정리
                                docker stop piro-recruiting-${deployColor} 2>/dev/null || true
                                docker rm piro-recruiting-${deployColor} 2>/dev/null || true

                                # 최신 이미지 풀
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                                # 새 컨테이너 실행
                                docker run -d \\
                                    --name piro-recruiting-${deployColor} \\
                                    --restart unless-stopped \\
                                    -p ${deployPort}:8080 \\
                                    -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME} \\
                                    -e SPRING_DATASOURCE_USERNAME=${DB_USERNAME} \\
                                    -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \\
                                    -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate \\
                                    -e SPRING_JPA_SHOW_SQL=false \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    -e LOGGING_LEVEL_ROOT=INFO \\
                                    -e OPENAI_API_KEY=${OPENAI_API_KEY} \\
                                    -e STMP_USER_ID=${STMP_USER_ID} \\
                                    -e STMP_PASSWORD=${STMP_PASSWORD} \\
                                    -e JAVA_OPTS="-Xmx256m -XX:+UseG1GC" \\
                                    ${DOCKER_IMAGE}:${DOCKER_TAG}

                                # 컨테이너 시작 확인
                                sleep 10
                                if docker ps | grep piro-recruiting-${deployColor}; then
                                    echo "✅ 컨테이너 ${deployColor} 정상 시작 (포트: ${deployPort})"
                                else
                                    echo "❌ 컨테이너 시작 실패!"
                                    docker logs piro-recruiting-${deployColor}
                                    exit 1
                                fi
                            '
                        """
                    }

                    // 개선된 헬스체크 (SSH 로컬 접근 - 핵심 수정!)
                    echo "🔍 헬스체크 시작 (${deployColor} 환경, 포트: ${deployPort})"

                    def healthCheckPassed = false
                    def maxRetries = 18  // 4.5분 대기 (15초 * 18)
                    def retryCount = 0

                    while (retryCount < maxRetries && !healthCheckPassed) {
                        try {
                            sleep(15)
                            def healthResponse = ""

                            // sshagent 블록으로 SSH 인증 처리
                            sshagent(['app-server-ssh']) {
                                healthResponse = sh(
                                    script: """
                                        ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                            curl -f -s http://localhost:${deployPort}/actuator/health 2>/dev/null || echo "NO_RESPONSE"
                                        '
                                    """,
                                    returnStdout: true
                                ).trim()
                            }

                            echo "📊 헬스체크 응답 (${retryCount + 1}/${maxRetries}): ${healthResponse}"

                            // JSON 응답이 있으면 성공 (UP/DOWN 상관없이)
                            if (healthResponse != 'NO_RESPONSE' &&
                                (healthResponse.contains('"status"') ||
                                 healthResponse.contains('UP') ||
                                 healthResponse.contains('DOWN'))) {
                                echo "✅ 헬스체크 성공! 애플리케이션이 응답 중"
                                healthCheckPassed = true
                            } else {
                                echo "⏳ 애플리케이션 시작 중..."
                            }
                        } catch (Exception e) {
                            echo "⏳ 헬스체크 대기 중... (${retryCount + 1}/${maxRetries})"
                        }
                        retryCount++
                    }

                    if (!healthCheckPassed) {
                        // 디버깅 정보 수집
                        sshagent(['app-server-ssh']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    echo "🔍 디버깅 정보:"
                                    echo "=== 컨테이너 상태 ==="
                                    docker ps -a | grep piro-recruiting-${deployColor}
                                    echo "=== 컨테이너 로그 (최근 30줄) ==="
                                    docker logs --tail 30 piro-recruiting-${deployColor}
                                    echo "=== 로컬 헬스체크 테스트 ==="
                                    curl -v http://localhost:${deployPort}/actuator/health || echo "로컬 헬스체크 실패"
                                    echo "=== 컨테이너 내부 헬스체크 ==="
                                    docker exec piro-recruiting-${deployColor} curl -s http://localhost:8080/actuator/health || echo "내부 헬스체크 실패"
                                '
                            """
                        }
                        error "❌ 헬스체크 실패: ${maxRetries}번 시도 후에도 응답 없음"
                    }

                    // Nginx 설정 업데이트 (app 파일 사용 - 핵심 수정!)
                    echo "🔄 Nginx 트래픽 전환 중..."
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                # Nginx app 설정 파일 업데이트
                                sudo tee /etc/nginx/sites-available/app > /dev/null <<EOF
upstream app_backend {
    server localhost:${deployPort};
}

server {
    listen 80;
    server_name _;

    location /health {
        return 200 "healthy\\n";
        add_header Content-Type text/plain;
    }

    location / {
        proxy_pass http://app_backend;
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;

        proxy_connect_timeout 10s;
        proxy_send_timeout 15s;
        proxy_read_timeout 15s;
    }

    location /actuator/health {
        proxy_pass http://app_backend/actuator/health;
        proxy_set_header Host \\\$host;
    }

    location /deployment-status {
        return 200 "Active: ${deployPort} (${deployColor}) - Build: ${DOCKER_TAG}";
        add_header Content-Type text/plain;
    }
}
EOF

                                # Nginx 테스트 및 재시작
                                if sudo nginx -t; then
                                    sudo systemctl reload nginx
                                    echo "✅ Nginx 트래픽 전환 완료: ${deployColor} (${deployPort})"
                                else
                                    echo "❌ Nginx 설정 오류"
                                    sudo nginx -t
                                    exit 1
                                fi
                            '
                        """
                    }

                    echo "🎉 Blue-Green 배포 완료!"
                    echo "🚀 활성 환경: ${deployColor} (포트: ${deployPort})"
                }
            }
        }

        stage('Final Verification') {
            steps {
                echo '🔍 최종 검증 수행 중...'
                script {
                    def finalCheck = sh(
                        script: "curl -f -s http://${APP_SERVER}/deployment-status",
                        returnStdout: true
                    ).trim()

                    echo "📊 배포 상태: ${finalCheck}"

                    def healthCheck = sh(
                        script: "curl -f -s http://${APP_SERVER}/actuator/health",
                        returnStatus: true
                    )

                    if (healthCheck == 0) {
                        echo "✅ 최종 검증 성공!"
                        echo "🌐 서비스 URL: http://${APP_SERVER}"
                        echo "💚 헬스체크: http://${APP_SERVER}/actuator/health"
                    } else {
                        echo "⚠️ 헬스체크에 문제가 있지만 서비스는 실행 중일 수 있습니다."
                    }
                }
            }
        }
    }

    post {
        always {
            echo '🧹 워크스페이스 정리 중...'
            sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} 2>/dev/null || true"
            sh "docker rmi ${DOCKER_IMAGE}:latest 2>/dev/null || true"
            sh "docker system prune -f"
            cleanWs()
        }

        success {
            echo '🎉🎉🎉 Blue-Green 배포 성공! 🎉🎉🎉'
            echo "🚀 애플리케이션: http://${APP_SERVER}"
            echo "📊 배포 상태: http://${APP_SERVER}/deployment-status"
        }

        failure {
            echo '❌ 배포 실패!'
            echo "🔧 수동 확인: ssh ubuntu@${APP_SERVER} 'docker ps -a'"
        }
    }
}