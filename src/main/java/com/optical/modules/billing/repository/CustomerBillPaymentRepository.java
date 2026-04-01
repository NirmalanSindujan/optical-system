package com.optical.modules.billing.repository;

import com.optical.modules.billing.entity.CustomerBillPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerBillPaymentRepository extends JpaRepository<CustomerBillPayment, Long> {
    List<CustomerBillPayment> findByCustomerBillIdOrderByIdAsc(Long customerBillId);
}
