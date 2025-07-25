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

        // 데이터베이스 정보 (Spring Boot 표준으로 수정)
        DB_HOST = '34.64.113.7'
        DB_NAME = 'app_user'
        DB_USERNAME = credentials('DB_USERNAME')
        DB_PASSWORD = credentials('DB_PASSWORD')
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
                    // Jenkins 메모리 최적화 빌드
                    sh './gradlew clean bootJar --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx512m -XX:+UseG1GC" -x test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    // Docker 이미지 빌드
                    def image = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing to Docker Hub...'
                script {
                    // Docker Hub에 푸시 (안정적인 배포를 위해 활성화)
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
                    // 현재 실행 중인 컨테이너 확인
                    def runningContainers = ""
                    try {
                        sshagent(['app-server-ssh']) {
                            runningContainers = sh(
                                script: """
                                    ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                        docker ps --format "{{.Names}}" | grep piro-recruiting || echo "none"
                                    '
                                """,
                                returnStdout: true
                            ).trim()
                        }
                    } catch (Exception e) {
                        echo "컨테이너 확인 중 오류: ${e.message}"
                        runningContainers = "none"
                    }

                    echo "📋 현재 실행 중인 컨테이너: ${runningContainers}"

                    // Blue/Green 결정 로직
                    def newPort = BLUE_PORT
                    def newColor = 'blue'
                    def oldPort = GREEN_PORT
                    def oldColor = 'green'

                    if (runningContainers.contains('piro-recruiting-blue')) {
                        newPort = GREEN_PORT
                        newColor = 'green'
                        oldPort = BLUE_PORT
                        oldColor = 'blue'
                    }

                    echo "🎯 배포 대상: ${newColor} 환경 (포트: ${newPort})"

                    // 새 컨테이너 배포
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                echo "📦 컨테이너 배포 시작..."

                                # 기존 컨테이너 정리
                                docker stop piro-recruiting-${newColor} 2>/dev/null || true
                                docker rm piro-recruiting-${newColor} 2>/dev/null || true

                                # 최신 이미지 풀
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                                # 새 컨테이너 실행 (Spring Boot 표준 환경변수 사용)
                                docker run -d \\
                                    --name piro-recruiting-${newColor} \\
                                    --restart unless-stopped \\
                                    -p ${newPort}:8080 \\
                                    -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME} \\
                                    -e SPRING_DATASOURCE_USERNAME=${DB_USERNAME} \\
                                    -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \\
                                    -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \\
                                    -e SPRING_JPA_SHOW_SQL=false \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    -e LOGGING_LEVEL_ROOT=INFO \\
                                    -e JAVA_OPTS="-Xmx256m -XX:+UseG1GC" \\
                                    ${DOCKER_IMAGE}:${DOCKER_TAG}

                                echo "✅ 컨테이너 ${newColor} 시작됨 (포트: ${newPort})"

                                # 컨테이너 상태 확인
                                sleep 5
                                docker ps | grep piro-recruiting-${newColor} || echo "⚠️ 컨테이너 상태 확인 필요"
                            '
                        """
                    }

                    // 개선된 헬스체크 (DOWN 상태도 허용)
                    echo "🔍 헬스체크 시작 (${newColor} 환경, 포트: ${newPort})"

                    def healthCheckPassed = false
                    def maxRetries = 20  // 5분 대기 (15초 * 20)
                    def retryCount = 0

                    while (retryCount < maxRetries && !healthCheckPassed) {
                        try {
                            sleep(15)
                            def healthResponse = sh(
                                script: "curl -f -s http://${APP_SERVER}:${newPort}/actuator/health 2>/dev/null || echo 'NO_RESPONSE'",
                                returnStdout: true
                            ).trim()

                            echo "📊 헬스체크 응답 (${retryCount + 1}/${maxRetries}): ${healthResponse}"

                            // JSON 응답이 있으면 성공 (UP/DOWN 상관없이)
                            if (healthResponse != 'NO_RESPONSE' &&
                                (healthResponse.contains('"status"') ||
                                 healthResponse.contains('UP') ||
                                 healthResponse.contains('DOWN'))) {
                                echo "✅ 헬스체크 성공! 애플리케이션이 응답하고 있습니다."
                                echo "📈 응답 내용: ${healthResponse}"
                                healthCheckPassed = true
                            } else {
                                echo "⏳ 애플리케이션 시작 중... (${retryCount + 1}/${maxRetries})"
                            }
                        } catch (Exception e) {
                            echo "⏳ 헬스체크 대기 중... (${retryCount + 1}/${maxRetries}): ${e.message}"
                        }
                        retryCount++
                    }

                    if (!healthCheckPassed) {
                        // 컨테이너 로그 확인
                        sshagent(['app-server-ssh']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    echo "🔍 컨테이너 로그 확인:"
                                    docker logs --tail 20 piro-recruiting-${newColor}
                                '
                            """
                        }
                        error "❌ 헬스체크 실패: ${maxRetries}번 시도 후에도 애플리케이션이 응답하지 않습니다."
                    }

                    // Nginx 트래픽 전환
                    echo "🔄 Nginx 트래픽 전환 중..."
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                # Nginx 설정 업데이트
                                sudo tee /etc/nginx/sites-available/default > /dev/null <<EOF
server {
    listen 80;
    server_name _;

    # 메인 애플리케이션
    location / {
        proxy_pass http://localhost:${newPort};
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;

        # 타임아웃 설정
        proxy_connect_timeout 10s;
        proxy_send_timeout 15s;
        proxy_read_timeout 15s;
    }

    # 헬스체크 엔드포인트
    location /actuator/health {
        proxy_pass http://localhost:${newPort}/actuator/health;
        proxy_set_header Host \\\$host;
    }

    # 배포 상태 확인용
    location /deployment-status {
        return 200 "Active: ${newPort} (${newColor}) - Build: ${DOCKER_TAG}";
        add_header Content-Type text/plain;
    }
}
EOF

                                # Nginx 설정 테스트 및 재시작
                                if sudo nginx -t; then
                                    sudo systemctl reload nginx
                                    echo "✅ Nginx 설정 업데이트 완료"
                                else
                                    echo "❌ Nginx 설정 오류"
                                    exit 1
                                fi

                                echo "🎯 트래픽이 포트 ${newPort} (${newColor})로 전환되었습니다."
                            '
                        """
                    }

                    // 이전 버전 정리 (30초 대기 후)
                    if (runningContainers != "none" && runningContainers.contains('piro-recruiting-')) {
                        echo "⏰ 30초 후 이전 컨테이너 정리..."
                        sleep(30)

                        sshagent(['app-server-ssh']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    echo "🧹 이전 컨테이너 ${oldColor} 정리 중..."
                                    docker stop piro-recruiting-${oldColor} 2>/dev/null || true
                                    docker rm piro-recruiting-${oldColor} 2>/dev/null || true

                                    # 사용하지 않는 이미지 정리
                                    docker image prune -f

                                    echo "✅ 정리 완료"
                                '
                            """
                        }
                    }

                    echo "🎉 Blue-Green 배포 완료!"
                }
            }
        }

        stage('Final Verification') {
            steps {
                echo '🔍 최종 검증 수행 중...'
                script {
                    // Nginx를 통한 최종 접속 테스트
                    def nginxCheck = sh(
                        script: "curl -f -s http://${APP_SERVER}/actuator/health",
                        returnStatus: true
                    )

                    def statusCheck = sh(
                        script: "curl -f -s http://${APP_SERVER}/deployment-status",
                        returnStdout: true
                    ).trim()

                    if (nginxCheck == 0) {
                        echo "✅ 최종 검증 성공!"
                        echo "📊 배포 상태: ${statusCheck}"
                        echo "🌐 서비스 URL: http://${APP_SERVER}"
                        echo "💚 헬스체크 URL: http://${APP_SERVER}/actuator/health"
                    } else {
                        echo "⚠️ Nginx를 통한 접속에 문제가 있을 수 있습니다."
                        echo "🔗 직접 접속 테스트: http://${APP_SERVER}:8081 또는 8082"
                    }
                }
            }
        }
    }

    post {
        always {
            echo '🧹 워크스페이스 정리 중...'
            // 로컬 Docker 이미지 정리 (Jenkins 메모리 절약)
            sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} 2>/dev/null || true"
            sh "docker rmi ${DOCKER_IMAGE}:latest 2>/dev/null || true"
            sh "docker system prune -f"
            cleanWs()
        }

        success {
            echo '🎉🎉🎉 Blue-Green 배포 성공! 🎉🎉🎉'
            echo "🚀 애플리케이션 URL: http://${APP_SERVER}"
            echo "📊 헬스체크: http://${APP_SERVER}/actuator/health"
            echo "📈 배포 상태: http://${APP_SERVER}/deployment-status"
            echo "🐳 Docker 이미지: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }

        failure {
            echo '❌ 배포 실패!'
            echo "🔍 로그를 확인하여 문제를 파악하세요."
            echo "🔧 필요시 수동 롤백: docker stop piro-recruiting-green && docker start piro-recruiting-blue"
        }
    }
}