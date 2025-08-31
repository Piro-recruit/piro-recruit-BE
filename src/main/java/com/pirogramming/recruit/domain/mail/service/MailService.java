package com.pirogramming.recruit.domain.mail.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.Value;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.mail.dto.BulkMailRequestDto;
import com.pirogramming.recruit.domain.mail.dto.SingleMailRequestDto;
import com.pirogramming.recruit.domain.mail.repository.MailSubscriberRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

	private final SesClient sesClient;
	private final MailSubscriberRepository mailSubscriberRepository;

	@Qualifier("mailExecutor")
	private final Executor mailExecutor;

	@Value("${aws.ses.from-email}")
	private String fromEmail;

	@Value("${aws.ses.max-send-rate}")
	private int maxSendRate;

	@Value("${aws.ses.batch-size}")
	private int batchSize;

	@Value("${aws.ses.retry-count}")
	private int retryCount;

	// 단일 메일 전송
	public void sendSingleMail(SingleMailRequestDto mailRequest) {
		try {
			String htmlContent = convertMarkdownToHtml(mailRequest.getContent());
			sendEmailWithRetry(
					List.of(mailRequest.getRecipientEmail()),
					mailRequest.getSubject(),
					htmlContent,
					1
			);
			log.info("개별 메일 발송 성공 - 수신자: {}", mailRequest.getRecipientEmail());
		} catch (Exception e) {
			log.error("개별 메일 발송 실패 - 수신자: {}, 오류: {}",
					mailRequest.getRecipientEmail(), e.getMessage(), e);
			throw new RuntimeException("메일 발송에 실패했습니다: " + e.getMessage(), e);
		}
	}

	// 일괄 메일 전송 (성능 최적화)
	public void sendBulkMail(BulkMailRequestDto mailRequest) {
		List<String> recipients = getAllSubscribedEmails();

		if (recipients.isEmpty()) {
			throw new IllegalArgumentException("알림을 신청한 사용자가 없습니다");
		}

		try {
			String htmlContent = convertMarkdownToHtml(mailRequest.getContent());

			log.info("일괄 메일 발송 시작 - 총 수신자: {} 명", recipients.size());

			// 200개가 넘으면 배치로 나누어서 처리
			if (recipients.size() > batchSize) {
				sendBulkEmailInBatches(recipients, mailRequest.getSubject(), htmlContent);
			} else {
				sendEmailWithRetry(recipients, mailRequest.getSubject(), htmlContent, 1);
			}

			log.info("일괄 메일 발송 완료 - 수신자 수: {}", recipients.size());
		} catch (Exception e) {
			log.error("일괄 메일 발송 실패 - 수신자 수: {}, 오류: {}",
					recipients.size(), e.getMessage(), e);
			throw new RuntimeException("메일 발송에 실패했습니다: " + e.getMessage(), e);
		}
	}

	// 배치로 나누어서 메일 전송 (비동기 처리)
	private void sendBulkEmailInBatches(List<String> recipients, String subject, String htmlContent) {
		int totalRecipients = recipients.size();
		CompletableFuture<Void>[] futures = new CompletableFuture[0];

		// 배치 단위로 나누어 처리
		for (int i = 0; i < totalRecipients; i += batchSize) {
			int endIndex = Math.min(i + batchSize, totalRecipients);
			List<String> batch = recipients.subList(i, endIndex);
			int batchNumber = (i / batchSize) + 1;

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					// AWS SES 전송률 제한을 고려한 지연
					long delay = calculateDelay(batch.size());
					if (delay > 0) {
						Thread.sleep(delay);
					}

					sendEmailWithRetry(batch, subject, htmlContent, 1);
					log.info("배치 {} 전송 완료 - {} 건", batchNumber, batch.size());
				} catch (Exception e) {
					log.error("배치 {} 전송 실패 - {} 건, 오류: {}",
							batchNumber, batch.size(), e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}, mailExecutor);

			futures = Arrays.copyOf(futures, futures.length + 1);
			futures[futures.length - 1] = future;
		}

		// 모든 배치 작업 완료 대기
		CompletableFuture.allOf(futures).join();
	}

	// 재시도 로직을 포함한 메일 전송
	private void sendEmailWithRetry(List<String> recipients, String subject, String htmlContent, int attempt) {
		try {
			SendEmailRequest emailRequest = SendEmailRequest.builder()
					.source(fromEmail)
					.destination(Destination.builder().toAddresses(recipients).build())
					.message(Message.builder()
							.subject(Content.builder().data(subject).charset(StandardCharsets.UTF_8.name()).build())
							.body(Body.builder()
									.html(Content.builder()
											.data(createHtmlTemplate(subject, htmlContent))
											.charset(StandardCharsets.UTF_8.name())
											.build())
									.build())
							.build())
					.build();

			SendEmailResponse response = sesClient.sendEmail(emailRequest);
			log.debug("SES 메일 전송 성공 - MessageId: {}, 수신자 수: {}",
					response.messageId(), recipients.size());

		} catch (SdkException e) {
			if (attempt < retryCount) {
				log.warn("메일 전송 실패 (재시도 {}/{}): {}", attempt, retryCount, e.getMessage());
				try {
					Thread.sleep(1000 * attempt); // 지수백오프
					sendEmailWithRetry(recipients, subject, htmlContent, attempt + 1);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("메일 전송 중 중단됨", ie);
				}
			} else {
				log.error("메일 전송 최종 실패 - 재시도 횟수 초과: {}", e.getMessage(), e);
				throw new RuntimeException("메일 전송에 실패했습니다: " + e.getMessage(), e);
			}
		}
	}

	// AWS SES 전송률 제한을 고려한 지연 시간 계산
	private long calculateDelay(int emailCount) {
		// maxSendRate는 초당 전송 가능한 메일 수
		if (emailCount <= maxSendRate) {
			return 0;
		}

		// 전송할 메일 수가 전송률을 초과하면 지연 시간 계산
		return (long) ((emailCount / (double) maxSendRate) * 1000);
	}

	// 구독자 이메일 목록 조회
	private List<String> getAllSubscribedEmails() {
		return mailSubscriberRepository.findAllEmails();
	}

	// Markdown을 HTML로 변환
	private String convertMarkdownToHtml(String markdown) {
		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		Parser parser = Parser.builder().extensions(extensions).build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
		return renderer.render(document);
	}

	// HTML 템플릿 생성
	private String createHtmlTemplate(String subject, String content) {
		return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        padding-bottom: 20px;
                        border-bottom: 2px solid #e9ecef;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #e9ecef;
                        font-size: 12px;
                        color: #666;
                        text-align: center;
                    }
                    .unsubscribe {
                        margin-top: 10px;
                    }
                    .unsubscribe a {
                        color: #666;
                        text-decoration: none;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>피로그래밍</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>이 메일은 피로그래밍 알림 서비스입니다.</p>
                        <div class="unsubscribe">
                            <a href="https://recruit.pirogramming.com/unsubscribe">구독 해지</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, subject, content);
	}
}
