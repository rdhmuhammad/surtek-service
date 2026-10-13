package com.sabarceria.surtek.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ValidateJwtResponse {
    private String phone;

    private String userId;

    private String userCode;

    private String enable;
}