package com.optical.modules.customer.repository;

import com.optical.modules.customer.entity.CustomerCreditLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCreditLedgerRepository extends JpaRepository<CustomerCreditLedger, Long> {
}
