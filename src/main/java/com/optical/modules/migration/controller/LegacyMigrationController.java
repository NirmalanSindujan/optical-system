package com.optical.modules.migration.controller;

import com.optical.modules.migration.dto.LegacyCustomerPrescriptionImportResponse;
import com.optical.modules.migration.dto.LegacyMigrationJobResponse;
import com.optical.modules.migration.dto.LegacyMigrationJobStatusResponse;
import com.optical.modules.migration.service.LegacyMigrationJobService;
import com.optical.modules.migration.service.LegacyCustomerPrescriptionImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Legacy Migration", description = "Legacy SQL import APIs")
@SecurityRequirement(name = "bearerAuth")
public class LegacyMigrationController {

    private final LegacyCustomerPrescriptionImportService legacyCustomerPrescriptionImportService;
    private final LegacyMigrationJobService legacyMigrationJobService;

    @PostMapping(
            path = "/legacy/customers-prescriptions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public LegacyCustomerPrescriptionImportResponse importCustomersAndPrescriptions(
            @RequestPart("file") MultipartFile file
    ) {
        return legacyCustomerPrescriptionImportService.importFromSqlDump(file);
    }

    @PostMapping(
            path = "/legacy/customers-prescriptions/jobs",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public LegacyMigrationJobResponse startCustomersAndPrescriptionsImport(
            @RequestPart("file") MultipartFile file
    ) {
        return legacyMigrationJobService.startCustomerPrescriptionImport(file);
    }

    @GetMapping("/legacy/customers-prescriptions/jobs/{jobId}")
    public LegacyMigrationJobStatusResponse getCustomersAndPrescriptionsImportStatus(
            @PathVariable String jobId
    ) {
        return legacyMigrationJobService.getJobStatus(jobId);
    }
}
