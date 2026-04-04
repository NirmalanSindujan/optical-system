package com.optical.modules.inventory.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.inventory.dto.InventoryItemResponse;
import com.optical.modules.inventory.dto.InventoryPageResponse;
import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.repository.BranchInventoryRepository;
import com.optical.modules.users.entity.Role;
import com.optical.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private static final String LENS_PRODUCT_TYPE = "LENS";

    @Transactional(readOnly = true)
    public InventoryPageResponse search(String q,
                                        Long requestedBranchId,
                                        String productType,
                                        String lensSubType,
                                        int page,
                                        int size) {
        CustomUserDetails currentUser = getCurrentUser();
        Long resolvedBranchId = resolveAllowedBranchId(currentUser, requestedBranchId);
        String keyword = normalize(q);
        String normalizedProductType = normalizeCode(productType);
        String normalizedLensSubType = normalizeCode(lensSubType);
        validatePageRequest(page, size);
        validateFilters(normalizedProductType, normalizedLensSubType);

        List<BranchInventory> inventories = branchInventoryRepository.findInventoryByBranchId(resolvedBranchId).stream()
                .filter(inventory -> matchesKeyword(inventory, keyword))
                .filter(inventory -> matchesProductType(inventory, normalizedProductType))
                .filter(inventory -> matchesLensSubType(inventory, normalizedLensSubType))
                .toList();
        int fromIndex = Math.min(page * size, inventories.size());
        int toIndex = Math.min(fromIndex + size, inventories.size());
        List<InventoryItemResponse> items = inventories.subList(fromIndex, toIndex).stream()
                .map(this::mapResponse)
                .toList();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) inventories.size() / size);

        return InventoryPageResponse.builder()
                .items(items)
                .totalCounts(inventories.size())
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .branchId(resolvedBranchId)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse getByBranch(Long branchId,
                                             String q,
                                             String productType,
                                             String lensSubType,
                                             int page,
                                             int size) {
        return search(q, branchId, productType, lensSubType, page, size);
    }

    private Long resolveAllowedBranchId(CustomUserDetails currentUser, Long requestedBranchId) {
        if (currentUser.getUser().getRole() != Role.BRANCH_USER) {
            return requestedBranchId;
        }

        if (currentUser.getUser().getBranch() == null || currentUser.getUser().getBranch().getId() == null) {
            throw new ResourceNotFoundException("Branch not assigned for branch user");
        }

        Long userBranchId = currentUser.getUser().getBranch().getId();
        if (requestedBranchId != null && !userBranchId.equals(requestedBranchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view inventory for your branch");
        }
        return userBranchId;
    }

    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return currentUser;
    }

    private InventoryItemResponse mapResponse(BranchInventory inventory) {
        ProductVariant variant = inventory.getVariant();
        BigDecimal onHand = zeroIfNull(inventory.getOnHand());
        BigDecimal reserved = zeroIfNull(inventory.getReserved());

        return InventoryItemResponse.builder()
                .branchId(inventory.getBranch().getId())
                .branchName(inventory.getBranch().getName())
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .productTypeCode(variant.getProduct().getProductType().getCode())
                .lensSubType(variant.getLensDetails() == null ? null : variant.getLensDetails().getLensSubType())
                .availableQuantity(onHand.subtract(reserved))
                .sellingPrice(variant.getSellingPrice())
                .build();
    }

    private boolean matchesKeyword(BranchInventory inventory, String keyword) {
        if (keyword == null) {
            return true;
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        ProductVariant variant = inventory.getVariant();

        return containsIgnoreCase(variant.getProduct().getName(), normalizedKeyword)
                || containsIgnoreCase(variant.getSku(), normalizedKeyword)
                || containsIgnoreCase(variant.getBarcode(), normalizedKeyword)
                || containsIgnoreCase(variant.getProduct().getBrandName(), normalizedKeyword);
    }

    private boolean matchesProductType(BranchInventory inventory, String productType) {
        return productType == null
                || productType.equalsIgnoreCase(inventory.getVariant().getProduct().getProductType().getCode());
    }

    private boolean matchesLensSubType(BranchInventory inventory, String lensSubType) {
        if (lensSubType == null) {
            return true;
        }

        return inventory.getVariant().getLensDetails() != null
                && lensSubType.equalsIgnoreCase(inventory.getVariant().getLensDetails().getLensSubType());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private void validateFilters(String productType, String lensSubType) {
        if (lensSubType != null && productType != null && !LENS_PRODUCT_TYPE.equals(productType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensSubType filter can only be used with productType=LENS");
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be greater than 0");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
