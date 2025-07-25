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

        // 애플리케이션 포트
        BLUE_PORT = '8080'
        GREEN_PORT = '8081'

        // 데이터베이스 정보
        DB_HOST = '34.64.113.7'
        DB_NAME = 'piro-recruit'
        DB_USERNAME = credentials('DB_USERNAME')
        DB_PASSWORD = credentials('DB_PASSWORD')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                // Git 저장소에서 코드 체크아웃 (자동)
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
                    // 일시적으로 테스트 건너뛰기 (DB 연결 설정 후 활성화)
                    sh 'echo "Tests will be enabled after DB configuration"'
                }
            }
        }

        stage('Build JAR') {
            steps {
                echo 'Building Spring Boot application with memory optimization...'
                script {
                    // 메모리 최적화된 빌드
                    sh './gradlew clean bootJar --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx400m" -x test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    // Docker 이미지 빌드
                    def image = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")

                    // latest 태그도 추가
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Skipping Docker Hub push for now - will deploy locally built image'
                script {
                    sh 'echo "Image built successfully: ${DOCKER_IMAGE}:${DOCKER_TAG}"'
                }
            }
        }

        stage('Deploy Blue-Green') {
            steps {
                echo 'Starting Blue-Green deployment...'
                script {
                    // 현재 활성 포트 확인
                    def activePort = "8080"  // 첫 배포이므로 기본값 사용

                    // 새로운 배포 포트 결정
                    def newPort = (activePort == '8080') ? GREEN_PORT : BLUE_PORT
                    def newColor = (newPort == '8080') ? 'blue' : 'green'

                    echo "Current active port: ${activePort}"
                    echo "Deploying to port: ${newPort} (${newColor})"

                    // 새 컨테이너 배포
                    sshagent(['app-server-ssh']) {
                        sh """
                            # Docker 이미지를 App 서버로 전송
                            docker save kimgyuill/piro-recruiting:${BUILD_NUMBER} | gzip > /tmp/app-image.tar.gz
                            scp -o StrictHostKeyChecking=no /tmp/app-image.tar.gz ubuntu@34.64.41.136:/tmp/

                            ssh -o StrictHostKeyChecking=no ubuntu@34.64.41.136 '
                                # 기존 컨테이너 중지 및 제거
                                docker stop piro-recruiting-${newColor} || true
                                docker rm piro-recruiting-${newColor} || true

                                # 새 이미지 로드
                                gunzip -c /tmp/app-image.tar.gz | docker load
                                rm /tmp/app-image.tar.gz

                                # 새 컨테이너 실행
                                docker run -d \\
                                    --name piro-recruiting-${newColor} \\
                                    --restart unless-stopped \\
                                    -p ${newPort}:8080 \\
                                    -e DB_HOST=34.64.113.7 \\
                                    -e DB_NAME=piro-recruit \\
                                    -e DB_USERNAME=postgres \\
                                    -e DB_PASSWORD=${DB_PASSWORD} \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    kimgyuill/piro-recruiting:${BUILD_NUMBER}

                                # 헬스체크 대기
                                echo "Waiting for application to start..."
                                for i in {1..30}; do
                                    if curl -f http://localhost:${newPort}/actuator/health > /dev/null 2>&1; then
                                        echo "Application is healthy!"
                                        break
                                    fi
                                    echo "Attempt \$i/30: Waiting for health check..."
                                    sleep 10
                                done
                            '
                        """
                    }

                    // Nginx 설정 업데이트 (트래픽 스위칭)
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                # Nginx 설정 업데이트
                                sudo tee /etc/nginx/sites-available/default > /dev/null <<EOF
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://localhost:${newPort};
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;

        # 헬스체크 설정
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }

    location /health-check {
        return 200 "${newPort}";
        add_header Content-Type text/plain;
    }
}
EOF

                                # Nginx 설정 테스트 및 재시작
                                sudo nginx -t && sudo systemctl reload nginx

                                echo "Traffic switched to port ${newPort} (${newColor})"
                            '
                        """
                    }

                    // 이전 버전 정리 (30초 대기 후)
                    sleep(30)
                    def oldPort = (newPort == '8080') ? GREEN_PORT : BLUE_PORT
                    def oldColor = (oldPort == '8080') ? 'blue' : 'green'

                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                echo "Stopping old container on port ${oldPort} (${oldColor})"
                                docker stop piro-recruiting-${oldColor} || true
                                docker rm piro-recruiting-${oldColor} || true

                                # 오래된 이미지 정리
                                docker image prune -f
                            '
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                echo 'Performing final health check...'
                script {
                    // 최종 헬스체크
                    def healthCheck = sh(
                        script: "curl -f http://${APP_SERVER}/actuator/health",
                        returnStatus: true
                    )

                    if (healthCheck == 0) {
                        echo "✅ Deployment successful! Application is healthy."
                    } else {
                        error "❌ Health check failed! Rolling back..."
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up workspace...'
            // 로컬 Docker 이미지 정리
            sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
            sh "docker rmi ${DOCKER_IMAGE}:latest || true"

            // 워크스페이스 정리
            cleanWs()
        }

        success {
            echo '🎉 Pipeline completed successfully!'
            // 성공 알림 (슬랙, 이메일 등)
        }

        failure {
            echo '❌ Pipeline failed!'
            // 실패 알림 및 롤백 스크립트 실행
            script {
                // 자동 롤백 로직 (옵션)
                echo "Consider running rollback script..."
            }
        }
    }
}