package com.optical.modules.prescription.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.prescription.dto.PrescriptionPageResponse;
import com.optical.modules.prescription.dto.PrescriptionResponse;
import com.optical.modules.prescription.entity.Prescription;
import com.optical.modules.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Long id) {
        Prescription prescription = prescriptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        return mapResponse(prescription);
    }

    @Transactional(readOnly = true)
    public PrescriptionPageResponse searchByCustomer(Long customerId, int page, int size) {
        customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Page<Prescription> result = prescriptionRepository.findActiveByCustomerId(customerId, PageRequest.of(page, size));
        List<PrescriptionResponse> items = result.getContent().stream()
                .map(this::mapResponse)
                .toList();

        return PrescriptionPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    public PrescriptionResponse mapResponse(Prescription prescription) {
        CustomerBill bill = prescription.getCustomerBill();
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .customerId(prescription.getPatient().getCustomer().getId())
                .customerName(prescription.getPatient().getCustomer().getName())
                .patientId(prescription.getPatient().getId())
                .patientName(prescription.getPatient().getName())
                .customerBillId(bill == null ? null : bill.getId())
                .billNumber(bill == null ? null : (bill.getBillNumber() == null ? "BILL-" + bill.getId() : bill.getBillNumber()))
                .prescriptionDate(prescription.getPrescriptionDate())
                .values(prescription.getValues())
                .notes(prescription.getNotes())
                .build();
    }
}
