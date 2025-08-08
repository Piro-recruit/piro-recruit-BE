package com.pirogramming.recruit.global.config;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.entity.AdminRole;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    
    @Value("${admin.root.login-code}")
    private String rootAdminLoginCode;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeRootAdmin();
    }

    private void initializeRootAdmin() {
        // ROOT admin이 이미 존재하는지 확인
        if (adminRepository.findByRole(AdminRole.ROOT).isEmpty()) {
            Admin rootAdmin = Admin.builder()
                    .loginCode(rootAdminLoginCode)
                    .identifierName("시스템 관리자")
                    .role(AdminRole.ROOT)
                    .expiredAt(null) // ROOT는 만료되지 않음
                    .build();
            
            adminRepository.save(rootAdmin);
            log.info("ROOT Admin이 자동으로 생성되었습니다. Login Code: {}", rootAdminLoginCode);
        } else {
            log.info("ROOT Admin이 이미 존재합니다.");
        }
    }
}