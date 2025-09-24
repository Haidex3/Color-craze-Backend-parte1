package com.Color_craze.configs;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.Color_craze.auth.models.AuthUser;

import lombok.RequiredArgsConstructor;

/**
 * Implementación personalizada de UserDetails para integrar AuthUser (MongoDB)
 * con Spring Security.
 */
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final AuthUser authUser;

    /**
     * Devuelve el ID del usuario (String, compatible con MongoDB ObjectId).
     */
    public String getId() {
        return authUser.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Por ahora no hay roles/authorities configurados
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return authUser.getPassword();
    }

    @Override
    public String getUsername() {
        return authUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Puedes implementar lógica personalizada si lo necesitas
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Cambia si manejas bloqueo de cuentas
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Cambia si manejas expiración de credenciales
    }

    @Override
    public boolean isEnabled() {
        return true; // Cambia si manejas usuarios inactivos
    }
}
