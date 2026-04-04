package com.optical.modules.prescription.controller;

import com.optical.modules.prescription.dto.PrescriptionPageResponse;
import com.optical.modules.prescription.dto.PrescriptionResponse;
import com.optical.modules.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/prescriptions/{id}")
    public PrescriptionResponse getById(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }

    @GetMapping("/customers/{customerId}/prescriptions")
    public PrescriptionPageResponse searchByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return prescriptionService.searchByCustomer(customerId, page, size);
    }
}
