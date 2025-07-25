pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'kimgyuill/piro-recruiting'
        DOCKER_TAG = "${BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        APP_SERVER = '34.64.41.136'  // 실제 App 서버 IP
        APP_USER = 'ubuntu'
        BLUE_PORT = '8081'
        GREEN_PORT = '8082'
        DB_HOST = '34.64.113.7'      // 실제 DB IP
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

        stage('Build JAR') {
            steps {
                echo 'Building Spring Boot application...'
                sh './gradlew clean bootJar --no-daemon --max-workers=1 -x test'
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
                }
            }
        }

        stage('Deploy Blue-Green') {
            steps {
                echo '🚀 Starting Blue-Green deployment...'
                script {
                    // 현재 실행 중인 컨테이너 확인
                    def containerInfo = ""
                    sshagent(['app-server-ssh']) {
                        containerInfo = sh(
                            script: """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(piro-recruiting|NAMES)" || echo "실행 중인 컨테이너 없음"
                                '
                            """,
                            returnStdout: true
                        ).trim()
                    }

                    echo "📋 현재 컨테이너 상태:"
                    echo containerInfo

                    // Blue/Green 결정 로직
                    def deployPort = ""
                    def deployColor = ""
                    def stopColor = ""

                    if (containerInfo.contains('piro-recruiting-green')) {
                        deployPort = BLUE_PORT
                        deployColor = 'blue'
                        stopColor = 'green'
                    } else {
                        deployPort = GREEN_PORT
                        deployColor = 'green'
                        stopColor = 'blue'
                    }

                    echo "🎯 배포 대상: ${deployColor} (포트: ${deployPort})"

                    // 기존 컨테이너 정리
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                docker stop piro-recruiting-${stopColor} 2>/dev/null || true
                                docker rm piro-recruiting-${stopColor} 2>/dev/null || true
                                docker stop piro-recruiting-${deployColor} 2>/dev/null || true
                                docker rm piro-recruiting-${deployColor} 2>/dev/null || true
                            '
                        """
                    }

                    // 새 컨테이너 배포
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                                docker run -d \\
                                    --name piro-recruiting-${deployColor} \\
                                    --restart unless-stopped \\
                                    -p ${deployPort}:8080 \\
                                    -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME} \\
                                    -e SPRING_DATASOURCE_USERNAME=${DB_USERNAME} \\
                                    -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \\
                                    -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    -e JAVA_OPTS="-Xmx256m -XX:+UseG1GC" \\
                                    ${DOCKER_IMAGE}:${DOCKER_TAG}

                                sleep 15
                                if docker ps | grep piro-recruiting-${deployColor}; then
                                    echo "✅ 컨테이너 ${deployColor} 시작 성공"
                                else
                                    echo "❌ 컨테이너 시작 실패"
                                    exit 1
                                fi
                            '
                        """
                    }

                    // 헬스체크 (SSH 로컬 접근)
                    echo "🔍 헬스체크 시작..."
                    def healthCheckPassed = false
                    def maxRetries = 15
                    def retryCount = 0

                    while (retryCount < maxRetries && !healthCheckPassed) {
                        sleep(15)
                        def healthResponse = sh(
                            script: """
                                ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
                                    curl -f -s http://localhost:${deployPort}/actuator/health 2>/dev/null || echo "NO_RESPONSE"
                                '
                            """,
                            returnStdout: true
                        ).trim()

                        echo "📊 헬스체크 (${retryCount + 1}/${maxRetries}): ${healthResponse}"

                        if (healthResponse != 'NO_RESPONSE' &&
                            (healthResponse.contains('"status"') || healthResponse.contains('UP'))) {
                            echo "✅ 헬스체크 성공!"
                            healthCheckPassed = true
                        }
                        retryCount++
                    }

                    if (!healthCheckPassed) {
                        error "❌ 헬스체크 실패"
                    }

                    // Nginx 설정 업데이트
                    echo "🔄 Nginx 트래픽 전환..."
                    sshagent(['app-server-ssh']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${APP_USER}@${APP_SERVER} '
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
        proxy_set_header Host \\$host;
        proxy_set_header X-Real-IP \\$remote_addr;
        proxy_set_header X-Forwarded-For \\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\$scheme;
    }

    location /actuator/health {
        proxy_pass http://app_backend/actuator/health;
        proxy_set_header Host \\$host;
    }
}
EOF

                                sudo nginx -t && sudo systemctl reload nginx
                                echo "✅ Nginx 업데이트 완료"
                            '
                        """
                    }

                    echo "🎉 Blue-Green 배포 완료!"
                }
            }
        }
    }

    post {
        success {
            echo '🎉 배포 성공!'
        }
        failure {
            echo '❌ 배포 실패!'
        }
    }
}