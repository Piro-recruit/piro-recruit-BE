package com.pirogramming.recruit.global.jwt;


import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;

    public UserDetails loadUserById(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("관리자 없음"));
        return new CustomUserDetails(admin);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        throw new UnsupportedOperationException("loginCode로는 조회 안함");
    }
}
