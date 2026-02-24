package com.optical.modules.branch.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchResponse {

    private Long id;
    private String code;
    private String name;
    private Boolean isMain;
}