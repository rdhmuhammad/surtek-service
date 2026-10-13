package com.sabarceria.surtek.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSecurityModel {
    private String username;

    private ClientManifest userEntity;

    private String authorization;

    private String password;

    private Boolean isValid;

    private Boolean isExpired;
}
