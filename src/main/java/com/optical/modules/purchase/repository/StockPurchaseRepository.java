package com.optical.modules.purchase.repository;

import com.optical.modules.purchase.entity.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {
}
