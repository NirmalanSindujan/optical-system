package com.optical.modules.prescription.controller;

import com.optical.modules.prescription.dto.PrescriptionResponse;
import com.optical.modules.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/{id}")
    public PrescriptionResponse getById(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }
}
