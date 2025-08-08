package com.pirogramming.recruit.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // BaseTimeEntity의 @CreatedDate, @LastModifiedDate 자동 처리를 위한 설정
}
