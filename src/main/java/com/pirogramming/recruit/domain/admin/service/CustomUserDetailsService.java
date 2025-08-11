package com.pirogramming.recruit.domain.admin.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;

import lombok.RequiredArgsConstructor;

@Service("adminUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final AdminRepository adminRepository;

    public CustomUserDetails loadUserById(Long id) {
        // 웹훅 전용 토큰 처리 (외부 서비스용)
        if (id.equals(-1L)) {
            return createWebhookUserDetails();
        }
        
        Admin admin = adminRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("해당 관리자를 찾을 수 없습니다."));
        return new CustomUserDetails(admin);
    }
    
    /**
     * 웹훅 전용 UserDetails 생성 (외부 서비스용)
     */
    private CustomUserDetails createWebhookUserDetails() {
        Admin webhookAdmin = Admin.builder()
            .id(-1L)
            .loginCode("WEBHOOK")
            .identifierName("Webhook Service")
            .role(com.pirogramming.recruit.domain.admin.entity.AdminRole.WEBHOOK)
            .build();
        return new CustomUserDetails(webhookAdmin);
    }
}