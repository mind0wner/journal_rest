package com.journal.data.entities;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {

    ANON,
    APPROVED,
    STUDENT,
    MONITOR,
    ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }
}