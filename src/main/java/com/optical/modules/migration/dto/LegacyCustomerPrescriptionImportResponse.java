package com.optical.modules.migration.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LegacyCustomerPrescriptionImportResponse {

    private String sourceFileName;
    private long customersProcessed;
    private long customersCreated;
    private long customersUpdated;
    private long patientsCreated;
    private long patientsUpdated;
    private long prescriptionsProcessed;
    private long prescriptionsCreated;
    private long prescriptionsSkipped;
}
