package com.optical.modules.billing.repository;

import com.optical.modules.billing.entity.CustomerBillItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerBillItemRepository extends JpaRepository<CustomerBillItem, Long> {

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    List<CustomerBillItem> findByCustomerBillIdOrderByIdAsc(Long customerBillId);
}
