package com.optical.modules.prescription.repository;

import com.optical.modules.prescription.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @EntityGraph(attributePaths = {"patient", "patient.customer", "customerBill"})
    Optional<Prescription> findByIdAndDeletedAtIsNull(Long id);

    Optional<Prescription> findByCustomerBillIdAndDeletedAtIsNull(Long customerBillId);

    Optional<Prescription> findFirstByNotesAndDeletedAtIsNull(String notes);

    @Query("""
            select count(pr)
            from Prescription pr
            join pr.patient p
            join p.customer c
            where pr.deletedAt is null
              and p.deletedAt is null
              and c.deletedAt is null
              and c.id = :customerId
            """)
    long countActiveByCustomerId(@Param("customerId") Long customerId);

    @EntityGraph(attributePaths = {"patient", "patient.customer", "customerBill"})
    @Query("""
            select pr
            from Prescription pr
            join pr.patient p
            join p.customer c
            where pr.deletedAt is null
              and p.deletedAt is null
              and c.deletedAt is null
              and c.id = :customerId
            order by pr.prescriptionDate desc, pr.id desc
            """)
    Page<Prescription> findActiveByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
}
