package com.optical.modules.product.repository;

import com.optical.modules.product.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, String> {
}


