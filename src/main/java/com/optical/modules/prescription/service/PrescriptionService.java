package com.optical.modules.prescription.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.prescription.dto.PrescriptionResponse;
import com.optical.modules.prescription.entity.Prescription;
import com.optical.modules.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Long id) {
        Prescription prescription = prescriptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        return mapResponse(prescription);
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
