package com.optical.modules.customer.repository;

import com.optical.modules.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndDeletedAtIsNull(Long id);
    Optional<Customer> findFirstByPhoneAndDeletedAtIsNull(String phone);
    Optional<Customer> findFirstByNameIgnoreCaseAndDobAndDeletedAtIsNull(String name, LocalDate dob);
    Optional<Customer> findFirstByNameIgnoreCaseAndDeletedAtIsNull(String name);

    List<Customer> findAllByDeletedAtIsNull();

    boolean existsByPhoneAndDeletedAtIsNull(String phone);
    boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String phone, Long id);
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(String email, Long id);

    @Query("""
            select c from Customer c
            where c.deletedAt is null
              and (
                   :q is null or :q = ''
                   or lower(coalesce(c.name, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.phone, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select coalesce(sum(c.pendingAmount), 0)
            from Customer c
            where c.deletedAt is null
            """)
    BigDecimal sumPendingAmount();
}
