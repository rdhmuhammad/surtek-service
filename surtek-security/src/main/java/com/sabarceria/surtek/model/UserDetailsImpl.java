package com.sabarceria.surtek.model;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {

    private String username;

    private String password;

    private ClientManifest userEntity;

    private Boolean isValid;

    private Boolean isExpired;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(ClientManifest user, String password, Collection<? extends GrantedAuthority> authorities, Boolean isValid, Boolean isExpired) {
        this.username = user.getUsername();
        this.userEntity = user;
        this.password = password;
        this.authorities = authorities;
        this.isValid = isValid;
        this.isExpired = isExpired;
    }

    public static UserDetailsImpl build(UserSecurityModel securityModel) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + securityModel.getAuthorization()));
        return new UserDetailsImpl(securityModel.getUserEntity(), securityModel.getPassword(), authorities, securityModel.getIsValid(), securityModel.getIsExpired());
    }

    public ClientManifest getUserEntity() {
        return userEntity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !this.isExpired;
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
        if (Objects.isNull(this.isValid)) return false;
        return this.isValid;
    }

}
