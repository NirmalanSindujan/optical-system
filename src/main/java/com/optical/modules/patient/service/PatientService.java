package com.optical.modules.patient.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.patient.dto.PatientPageResponse;
import com.optical.modules.patient.dto.PatientRequest;
import com.optical.modules.patient.dto.PatientResponse;
import com.optical.modules.patient.entity.Patient;
import com.optical.modules.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public PatientResponse create(Long customerId, PatientRequest request) {
        Customer customer = findCustomer(customerId);

        Patient patient = new Patient();
        patient.setCustomer(customer);
        applyRequest(patient, request);

        return mapToResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public PatientPageResponse searchByCustomer(Long customerId, String q, int page, int size) {
        findCustomer(customerId);
        Page<Patient> result = patientRepository.searchByCustomerId(customerId, normalize(q), PageRequest.of(page, size));
        List<PatientResponse> items = result.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PatientPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        return mapToResponse(findPatient(id));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = findPatient(id);
        applyRequest(patient, request);
        return mapToResponse(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = findPatient(id);
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private Patient findPatient(Long id) {
        return patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    private void applyRequest(Patient patient, PatientRequest request) {
        patient.setName(request.getName());
        patient.setGender(normalize(request.getGender()));
        patient.setDob(request.getDob());
        patient.setNotes(normalize(request.getNotes()));
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .customerId(patient.getCustomer().getId())
                .customerName(patient.getCustomer().getName())
                .name(patient.getName())
                .gender(patient.getGender())
                .dob(patient.getDob())
                .notes(patient.getNotes())
                .build();
    }
}
