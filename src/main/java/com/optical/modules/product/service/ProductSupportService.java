package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.SupplierInfoResponse;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.SupplierProduct;
import com.optical.modules.product.repository.ProductRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.SupplierProductRepository;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSupportService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;

    public List<Long> resolveAndValidateSupplierIds(List<Long> rawSupplierIds, String requiredMessage) {
        List<Long> normalized = rawSupplierIds == null
                ? List.of()
                : rawSupplierIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, requiredMessage);
        }

        for (Long supplierId : normalized) {
            supplierRepository.findByIdAndDeletedAtIsNull(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        }
        return normalized;
    }

    public void linkSuppliersToProduct(Product product, List<Long> supplierIds) {
        List<SupplierProduct> links = supplierIds.stream()
                .map(supplierId -> {
                    SupplierProduct link = new SupplierProduct();
                    link.setProduct(product);
                    link.setSupplierId(supplierId);
                    return link;
                })
                .toList();
        supplierProductRepository.saveAll(links);
    }

    public void replaceSupplierLinks(Product product, List<Long> supplierIds) {
        supplierProductRepository.softDeleteActiveByProductId(product.getId(), LocalDateTime.now());
        linkSuppliersToProduct(product, supplierIds);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<ProductVariant> variants = productVariantRepository.findByProductIdAndDeletedAtIsNull(productId);
        for (ProductVariant variant : variants) {
            productVariantRepository.delete(variant);
        }

        supplierProductRepository.softDeleteActiveByProductId(productId, LocalDateTime.now());
        productRepository.delete(product);
    }

    public List<Long> resolveSupplierIdsForProduct(Long productId) {
        return supplierProductRepository.findActiveSupplierIdsByProductId(productId);
    }

    public List<SupplierInfoResponse> resolveSupplierInfosForProduct(Long productId) {
        return resolveSupplierInfos(resolveSupplierIdsForProduct(productId));
    }

    public List<SupplierInfoResponse> resolveSupplierInfos(List<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Supplier> suppliersById = supplierRepository.findByIdInAndDeletedAtIsNull(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));

        return supplierIds.stream()
                .map(suppliersById::get)
                .filter(Objects::nonNull)
                .map(this::mapSupplierInfo)
                .toList();
    }

    public BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private SupplierInfoResponse mapSupplierInfo(Supplier supplier) {
        return SupplierInfoResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .build();
    }
}
