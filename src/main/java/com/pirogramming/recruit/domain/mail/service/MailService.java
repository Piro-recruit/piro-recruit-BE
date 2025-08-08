package com.pirogramming.recruit.domain.mail.service;

import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

	private final JavaMailSender javaMailSender;
	private final MailSubscriberRepository mailSubscriberRepository;

	public void sendSingleMail(SingleMailRequestDto mailRequest) {
		try {
			String htmlContent = convertMarkdownToHtml(mailRequest.getContent());
			sendHtmlMail(List.of(mailRequest.getRecipientEmail()), mailRequest.getSubject(), htmlContent);
			log.info("개별 메일 발송 성공 - 수신자: {}", mailRequest.getRecipientEmail());
		} catch (Exception e) {
			log.error("개별 메일 발송 실패 - 수신자: {}, 오류: {}", mailRequest.getRecipientEmail(), e.getMessage(), e);
			throw new RuntimeException("메일 발송에 실패했습니다: " + e.getMessage(), e);
		}
	}

	public void sendBulkMail(BulkMailRequestDto mailRequest) {
		List<String> recipients = getAllSubscribedEmails();
		
		if (recipients.isEmpty()) {
			throw new IllegalArgumentException("알림을 신청한 사용자가 없습니다");
		}

		try {
			String htmlContent = convertMarkdownToHtml(mailRequest.getContent());
			sendHtmlMail(recipients, mailRequest.getSubject(), htmlContent);
			log.info("일괄 메일 발송 성공 - 수신자 수: {}", recipients.size());
		} catch (Exception e) {
			log.error("일괄 메일 발송 실패 - 수신자 수: {}, 오류: {}", recipients.size(), e.getMessage(), e);
			throw new RuntimeException("메일 발송에 실패했습니다: " + e.getMessage(), e);
		}
	}

	private void sendHtmlMail(List<String> recipients, String subject, String content) throws MessagingException {
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
		
		helper.setTo(recipients.toArray(new String[0]));
		helper.setSubject(subject);
		helper.setText(createHtmlTemplate(subject, content), true); // HTML로 전송
		
		javaMailSender.send(mimeMessage);
	}

	private String createHtmlTemplate(String subject, String content) {
		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
				<meta charset="UTF-8">
				<meta name="viewport" content="width=device-width, initial-scale=1.0">
				<title>%s</title>
				<style>
					body {
						font-family: 'Arial', sans-serif;
						margin: 0;
						padding: 0;
						background-color: #f4f4f4;
						color: #333;
					}
					.container {
						max-width: 600px;
						margin: 20px auto;
						background-color: #ffffff;
						border-radius: 10px;
						box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
						overflow: hidden;
					}
					.header {
						background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
						color: white;
						padding: 30px;
						text-align: center;
					}
					.header h1 {
						margin: 0;
						font-size: 28px;
						font-weight: 300;
					}
					.content {
						padding: 40px 30px;
						line-height: 1.6;
					}
					.content h2 {
						color: #667eea;
						border-bottom: 2px solid #f0f0f0;
						padding-bottom: 10px;
						margin-bottom: 20px;
					}
					.content p {
						margin-bottom: 15px;
						color: #555;
					}
					.content table {
						width: 100%%;
						border-collapse: collapse;
						margin: 20px 0;
						background-color: #fff;
						box-shadow: 0 2px 4px rgba(0,0,0,0.1);
					}
					.content th, .content td {
						padding: 12px 15px;
						text-align: left;
						border-bottom: 1px solid #e9ecef;
					}
					.content th {
						background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
						color: white;
						font-weight: 600;
						border-bottom: 2px solid #5a67d8;
					}
					.content tr:hover {
						background-color: #f8f9fa;
					}
					.content code {
						background-color: #f1f3f4;
						color: #d73a49;
						padding: 2px 4px;
						border-radius: 3px;
						font-family: 'Courier New', monospace;
						font-size: 0.9em;
					}
					.content pre {
						background-color: #f6f8fa;
						border: 1px solid #e1e4e8;
						border-radius: 6px;
						padding: 16px;
						overflow-x: auto;
						margin: 20px 0;
					}
					.content pre code {
						background-color: transparent;
						color: #24292e;
						padding: 0;
					}
					.footer {
						background-color: #f8f9fa;
						padding: 20px 30px;
						text-align: center;
						border-top: 1px solid #e9ecef;
					}
					.footer p {
						margin: 0;
						color: #6c757d;
						font-size: 14px;
					}
					.highlight {
						background-color: #fff3cd;
						border-left: 4px solid #ffc107;
						padding: 15px;
						margin: 20px 0;
					}
					.button {
						display: inline-block;
						padding: 12px 30px;
						background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
						color: white;
						text-decoration: none;
						border-radius: 25px;
						margin: 20px 0;
						transition: transform 0.2s;
					}
					.button:hover {
						transform: translateY(-2px);
					}
				</style>
			</head>
			<body>
				<div class="container">
					<div class="header">
						<h1>📧 PIRO RECRUIT</h1>
						<p>피로그래밍 리크루팅 시스템</p>
					</div>
					<div class="content">
						<h2>%s</h2>
						<div>%s</div>
					</div>
					<div class="footer">
						<p>© 2025 PIRO Programming. All rights reserved.</p>
						<p>이 메일은 PIRO 리크루팅 시스템에서 자동으로 발송되었습니다.</p>
					</div>
				</div>예
			</body>
			</html>
			""".formatted(subject, subject, content);
	}

	private List<String> getAllSubscribedEmails() {
		return mailSubscriberRepository.findAll()
				.stream()
				.map(subscriber -> subscriber.getEmail())
				.toList();
	}

	private String convertMarkdownToHtml(String markdown) {
		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		Parser parser = Parser.builder()
			.extensions(extensions)
			.build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder()
			.extensions(extensions)
			.build();
		return renderer.render(document);
	}
}
