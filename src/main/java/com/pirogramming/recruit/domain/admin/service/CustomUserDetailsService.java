package com.pirogramming.recruit.domain.admin.service;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final AdminRepository adminRepository;

    public CustomUserDetails loadUserById(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("해당 관리자를 찾을 수 없습니다."));
        return new CustomUserDetails(admin);
    }
}
