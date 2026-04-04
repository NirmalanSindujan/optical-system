package com.optical.modules.prescription.repository;

import com.optical.modules.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByIdAndDeletedAtIsNull(Long id);

    Optional<Prescription> findByCustomerBillIdAndDeletedAtIsNull(Long customerBillId);
}
