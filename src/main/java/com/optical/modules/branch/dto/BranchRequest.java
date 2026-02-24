package com.optical.modules.branch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private Boolean isMain;
}