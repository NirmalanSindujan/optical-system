package com.optical.modules.patient.repository;

import com.optical.modules.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select p
            from Patient p
            join p.customer c
            where p.deletedAt is null
              and c.deletedAt is null
              and c.id = :customerId
              and (
                    :q is null or :q = ''
                    or lower(coalesce(p.name, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(p.gender, '')) like lower(concat('%', :q, '%'))
                  )
            """)
    Page<Patient> searchByCustomerId(@Param("customerId") Long customerId, @Param("q") String q, Pageable pageable);
}
