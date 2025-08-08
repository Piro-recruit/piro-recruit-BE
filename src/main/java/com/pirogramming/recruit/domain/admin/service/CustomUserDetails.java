package com.pirogramming.recruit.domain.admin.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.pirogramming.recruit.domain.admin.entity.Admin;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Admin admin;

    public CustomUserDetails(Admin admin) {
        this.admin = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()));
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