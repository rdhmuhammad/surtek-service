package com.sabarceria.surtek.model;

import lombok.Data;

@Data
public class JwtResponse {

    private Long id;

    private String token;

    private String type = "Bearer";

    private String username;

    private String email;

}
