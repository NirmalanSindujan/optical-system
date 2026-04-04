package com.optical.modules.patient.controller;

import com.optical.modules.patient.dto.PatientPageResponse;
import com.optical.modules.patient.dto.PatientRequest;
import com.optical.modules.patient.dto.PatientResponse;
import com.optical.modules.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping("/customers/{customerId}/patients")
    public PatientResponse create(@PathVariable Long customerId, @Valid @RequestBody PatientRequest request) {
        return patientService.create(customerId, request);
    }

    @GetMapping("/customers/{customerId}/patients")
    public PatientPageResponse searchByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return patientService.searchByCustomer(customerId, q, page, size);
    }

    @GetMapping("/patients/{id}")
    public PatientResponse getById(@PathVariable Long id) {
        return patientService.getById(id);
    }

    @PutMapping("/patients/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/patients/{id}")
    public void delete(@PathVariable Long id) {
        patientService.delete(id);
    }
}
