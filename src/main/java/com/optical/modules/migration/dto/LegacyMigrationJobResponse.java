package com.optical.modules.migration.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LegacyMigrationJobResponse {

    private String jobId;
    private String status;
    private String sourceFileName;
    private LocalDateTime createdAt;
}
