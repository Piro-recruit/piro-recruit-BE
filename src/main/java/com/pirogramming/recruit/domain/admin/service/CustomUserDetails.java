package com.pirogramming.recruit.domain.admin.service;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Admin admin;

    public CustomUserDetails(Admin admin) {
        this.admin = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 권한이 필요할 경우 추가
    }

    @Override
    public String getPassword() {
        return null; // 로그인 코드 기반이므로 사용 안함
    }

    @Override
    public String getUsername() {
        return String.valueOf(admin.getId()); // ID 기반
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getId() {
        return admin.getId();
    }

    public String getRole() {
        return admin.getRole().name();
    }
}
