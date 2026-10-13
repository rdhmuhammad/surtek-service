package com.sabarceria.surtek.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigResponse {

    @JsonProperty("is_validate")
    private Boolean isValidate = false;

}
