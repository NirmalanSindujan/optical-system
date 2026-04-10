package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.product.dto.SupplierInfoResponse;
import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.BranchInventoryId;
import com.optical.modules.product.entity.InventoryLot;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.SupplierProduct;
import com.optical.modules.product.repository.BranchInventoryRepository;
import com.optical.modules.product.repository.InventoryLotRepository;
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
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;

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

    public void initializeMainBranchInventory(ProductVariant variant) {
        BigDecimal quantity = variant.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Branch mainBranch = branchRepository.findFirstByIsMainTrueAndDeletedAtIsNull()
                .orElseThrow(() -> new ResourceNotFoundException("Main branch not found"));

        BranchInventory inventory = branchInventoryRepository
                .findByBranch_IdAndVariant_Id(mainBranch.getId(), variant.getId())
                .orElseGet(() -> {
                    BranchInventory created = new BranchInventory();
                    BranchInventoryId id = new BranchInventoryId();
                    id.setBranchId(mainBranch.getId());
                    id.setVariantId(variant.getId());
                    created.setId(id);
                    created.setBranch(mainBranch);
                    created.setVariant(variant);
                    created.setOnHand(BigDecimal.ZERO);
                    created.setReserved(BigDecimal.ZERO);
                    created.setReorderLevel(BigDecimal.ZERO);
                    return created;
                });

        inventory.setOnHand(zeroIfNull(inventory.getOnHand()).add(quantity));
        branchInventoryRepository.save(inventory);

        InventoryLot openingLot = new InventoryLot();
        openingLot.setVariant(variant);
        openingLot.setPurchasedAt(LocalDateTime.now());
        openingLot.setPurchaseCost(variant.getPurchasePrice());
        openingLot.setCurrencyCode("LKR");
        openingLot.setQtyReceived(quantity);
        openingLot.setQtyRemaining(quantity);
        openingLot.setNotes("OPENING_STOCK main-branch=" + mainBranch.getId());
        inventoryLotRepository.save(openingLot);
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

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
