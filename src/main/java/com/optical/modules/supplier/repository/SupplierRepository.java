package com.optical.modules.supplier.repository;

import com.optical.modules.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByPhoneAndDeletedAtIsNull(String phone);
    boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String phone, Long id);
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(String email, Long id);

    @Query("""
            select s from Supplier s
            where s.deletedAt is null
              and (
                   :q is null or :q = ''
                   or lower(coalesce(s.name, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(s.phone, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(s.email, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(s.address, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);
}
