package com.pirogramming.recruit.domain.mail.entity;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "mail_subscriber")
public class MailSubscriber extends BaseTimeEntity {
	@Id
	private String email;
}
