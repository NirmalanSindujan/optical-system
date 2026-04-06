package com.optical.modules.inventoryrequest.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.inventoryrequest.dto.InventoryRequestCreateItem;
import com.optical.modules.inventoryrequest.dto.InventoryRequestCreateRequest;
import com.optical.modules.inventoryrequest.dto.InventoryRequestDecisionRequest;
import com.optical.modules.inventoryrequest.dto.InventoryRequestItemResponse;
import com.optical.modules.inventoryrequest.dto.InventoryRequestPageResponse;
import com.optical.modules.inventoryrequest.dto.InventoryRequestResponse;
import com.optical.modules.inventoryrequest.entity.InventoryRequest;
import com.optical.modules.inventoryrequest.entity.InventoryRequestItem;
import com.optical.modules.inventoryrequest.entity.InventoryRequestStatus;
import com.optical.modules.inventoryrequest.repository.InventoryRequestRepository;
import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.BranchInventoryId;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.repository.BranchInventoryRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.users.entity.Role;
import com.optical.modules.users.entity.User;
import com.optical.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class InventoryRequestService {

    private final InventoryRequestRepository inventoryRequestRepository;
    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    @Transactional
    public InventoryRequestResponse create(InventoryRequestCreateRequest request) {
        CustomUserDetails currentUser = getCurrentUser();
        User user = currentUser.getUser();
        Branch requestingBranch = resolveRequestingBranch(user, request.getRequestingBranchId());
        Branch supplyingBranch = resolveBranch(request.getSupplyingBranchId(), "Supplying branch not found");

        validateDistinctBranches(requestingBranch.getId(), supplyingBranch.getId());

        InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setRequestingBranch(requestingBranch);
        inventoryRequest.setSupplyingBranch(supplyingBranch);
        inventoryRequest.setRequestedBy(user);
        inventoryRequest.setStatus(InventoryRequestStatus.PENDING);
        inventoryRequest.setRequestNote(normalize(request.getRequestNote()));

        Map<Long, ProductVariant> variantsById = loadVariants(request.getItems());
        for (InventoryRequestCreateItem itemRequest : request.getItems()) {
            InventoryRequestItem item = new InventoryRequestItem();
            item.setInventoryRequest(inventoryRequest);
            item.setVariant(variantsById.get(itemRequest.getVariantId()));
            item.setRequestedQuantity(scale(itemRequest.getQuantity()));
            inventoryRequest.getItems().add(item);
        }

        return mapResponse(inventoryRequestRepository.save(inventoryRequest));
    }

    @Transactional(readOnly = true)
    public InventoryRequestResponse getById(Long id) {
        InventoryRequest request = loadAccessibleRequest(id, getCurrentUser().getUser());
        return mapResponse(request);
    }

    @Transactional(readOnly = true)
    public InventoryRequestPageResponse search(Long branchId,
                                               InventoryRequestStatus status,
                                               String direction,
                                               int page,
                                               int size) {
        validatePageRequest(page, size);
        CustomUserDetails currentUser = getCurrentUser();
        User user = currentUser.getUser();
        String normalizedDirection = normalizeDirection(direction);
        Long resolvedBranchId = resolveSearchBranchId(user, branchId);

        Page<InventoryRequest> result = inventoryRequestRepository.findAll(
                buildSpecification(user, resolvedBranchId, status, normalizedDirection),
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );

        return InventoryRequestPageResponse.builder()
                .items(result.getContent().stream().map(this::mapResponse).toList())
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public InventoryRequestResponse accept(Long id, InventoryRequestDecisionRequest decisionRequest) {
        CustomUserDetails currentUser = getCurrentUser();
        User user = currentUser.getUser();
        InventoryRequest request = loadAccessibleRequest(id, user);

        ensurePending(request);
        ensureCanProcess(user, request);
        applyAcceptance(request);
        request.setStatus(InventoryRequestStatus.ACCEPTED);
        request.setProcessedBy(user);
        request.setDecisionNote(normalize(decisionRequest == null ? null : decisionRequest.getDecisionNote()));
        request.setProcessedAt(LocalDateTime.now());

        return mapResponse(inventoryRequestRepository.save(request));
    }

    @Transactional
    public InventoryRequestResponse reject(Long id, InventoryRequestDecisionRequest decisionRequest) {
        CustomUserDetails currentUser = getCurrentUser();
        User user = currentUser.getUser();
        InventoryRequest request = loadAccessibleRequest(id, user);

        ensurePending(request);
        ensureCanProcess(user, request);
        request.setStatus(InventoryRequestStatus.REJECTED);
        request.setProcessedBy(user);
        request.setDecisionNote(normalize(decisionRequest == null ? null : decisionRequest.getDecisionNote()));
        request.setProcessedAt(LocalDateTime.now());

        return mapResponse(inventoryRequestRepository.save(request));
    }

    private void applyAcceptance(InventoryRequest request) {
        Branch supplyingBranch = request.getSupplyingBranch();
        Branch requestingBranch = request.getRequestingBranch();

        Map<BranchInventoryId, BranchInventory> inventoryUpdates = new LinkedHashMap<>();
        for (InventoryRequestItem item : request.getItems()) {
            ProductVariant variant = item.getVariant();
            BranchInventory sourceInventory = getOrCreateInventory(supplyingBranch, variant, inventoryUpdates);
            BigDecimal available = zeroIfNull(sourceInventory.getOnHand()).subtract(zeroIfNull(sourceInventory.getReserved()));
            if (available.compareTo(item.getRequestedQuantity()) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock in supplying branch for variant: " + variant.getId()
                );
            }

            BranchInventory destinationInventory = getOrCreateInventory(requestingBranch, variant, inventoryUpdates);
            sourceInventory.setOnHand(scale(zeroIfNull(sourceInventory.getOnHand()).subtract(item.getRequestedQuantity())));
            destinationInventory.setOnHand(scale(zeroIfNull(destinationInventory.getOnHand()).add(item.getRequestedQuantity())));
        }

        branchInventoryRepository.saveAll(inventoryUpdates.values());
    }

    private BranchInventory getOrCreateInventory(Branch branch,
                                                 ProductVariant variant,
                                                 Map<BranchInventoryId, BranchInventory> inventoryUpdates) {
        BranchInventoryId id = new BranchInventoryId();
        id.setBranchId(branch.getId());
        id.setVariantId(variant.getId());

        return inventoryUpdates.computeIfAbsent(id, ignored ->
                branchInventoryRepository.findById(id).orElseGet(() -> {
                    BranchInventory inventory = new BranchInventory();
                    inventory.setId(id);
                    inventory.setBranch(branch);
                    inventory.setVariant(variant);
                    inventory.setOnHand(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    inventory.setReserved(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    inventory.setReorderLevel(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    return inventory;
                })
        );
    }

    private Specification<InventoryRequest> buildSpecification(User user,
                                                               Long branchId,
                                                               InventoryRequestStatus status,
                                                               String direction) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (user.getRole() == Role.BRANCH_USER) {
                Long userBranchId = requireUserBranchId(user);
                if (branchId != null && !userBranchId.equals(branchId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only search requests for your branch");
                }
                if ("OUTGOING".equals(direction)) {
                    predicates.add(criteriaBuilder.equal(root.get("requestingBranch").get("id"), userBranchId));
                } else if ("INCOMING".equals(direction)) {
                    predicates.add(criteriaBuilder.equal(root.get("supplyingBranch").get("id"), userBranchId));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("requestingBranch").get("id"), userBranchId),
                            criteriaBuilder.equal(root.get("supplyingBranch").get("id"), userBranchId)
                    ));
                }
            } else if (branchId != null) {
                if ("OUTGOING".equals(direction)) {
                    predicates.add(criteriaBuilder.equal(root.get("requestingBranch").get("id"), branchId));
                } else if ("INCOMING".equals(direction)) {
                    predicates.add(criteriaBuilder.equal(root.get("supplyingBranch").get("id"), branchId));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("requestingBranch").get("id"), branchId),
                            criteriaBuilder.equal(root.get("supplyingBranch").get("id"), branchId)
                    ));
                }
            } else if ("OUTGOING".equals(direction) || "INCOMING".equals(direction)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "branchId is required when direction filter is used"
                );
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private InventoryRequest loadAccessibleRequest(Long id, User user) {
        InventoryRequest request = inventoryRequestRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory request not found"));

        if (user.getRole() == Role.BRANCH_USER) {
            Long branchId = requireUserBranchId(user);
            boolean allowed = branchId.equals(request.getRequestingBranch().getId())
                    || branchId.equals(request.getSupplyingBranch().getId());
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access requests for your branch");
            }
        }
        return request;
    }

    private void ensureCanProcess(User user, InventoryRequest request) {
        if (user.getRole() == Role.BRANCH_USER) {
            Long branchId = requireUserBranchId(user);
            if (!branchId.equals(request.getSupplyingBranch().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only process requests assigned to your branch"
                );
            }
        }
    }

    private void ensurePending(InventoryRequest request) {
        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be processed");
        }
    }

    private Branch resolveRequestingBranch(User user, Long requestingBranchId) {
        if (user.getRole() == Role.BRANCH_USER) {
            Long userBranchId = requireUserBranchId(user);
            if (!userBranchId.equals(requestingBranchId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only create requests for your branch"
                );
            }
        }
        return resolveBranch(requestingBranchId, "Requesting branch not found");
    }

    private Long resolveSearchBranchId(User user, Long branchId) {
        if (user.getRole() != Role.BRANCH_USER) {
            return branchId;
        }

        Long userBranchId = requireUserBranchId(user);
        if (branchId != null && !userBranchId.equals(branchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only search requests for your branch");
        }
        return userBranchId;
    }

    private Long requireUserBranchId(User user) {
        if (user.getBranch() == null || user.getBranch().getId() == null) {
            throw new ResourceNotFoundException("Branch not assigned for branch user");
        }
        return user.getBranch().getId();
    }

    private Branch resolveBranch(Long branchId, String notFoundMessage) {
        return branchRepository.findByIdAndDeletedAtIsNull(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private Map<Long, ProductVariant> loadVariants(List<InventoryRequestCreateItem> requests) {
        List<Long> variantIds = requests.stream()
                .map(InventoryRequestCreateItem::getVariantId)
                .toList();
        if (variantIds.stream().distinct().count() != variantIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate variant lines are not allowed");
        }

        Map<Long, ProductVariant> variantsById = new LinkedHashMap<>();
        for (ProductVariant variant : productVariantRepository.findAllById(variantIds)) {
            variantsById.put(variant.getId(), variant);
        }

        for (Long variantId : variantIds) {
            if (!variantsById.containsKey(variantId)) {
                throw new ResourceNotFoundException("Product variant not found: " + variantId);
            }
        }
        return variantsById;
    }

    private InventoryRequestResponse mapResponse(InventoryRequest request) {
        return InventoryRequestResponse.builder()
                .id(request.getId())
                .requestingBranchId(request.getRequestingBranch().getId())
                .requestingBranchName(request.getRequestingBranch().getName())
                .supplyingBranchId(request.getSupplyingBranch().getId())
                .supplyingBranchName(request.getSupplyingBranch().getName())
                .requestedByUserId(request.getRequestedBy().getId())
                .requestedByUsername(request.getRequestedBy().getUsername())
                .processedByUserId(request.getProcessedBy() == null ? null : request.getProcessedBy().getId())
                .processedByUsername(request.getProcessedBy() == null ? null : request.getProcessedBy().getUsername())
                .status(request.getStatus())
                .requestNote(request.getRequestNote())
                .decisionNote(request.getDecisionNote())
                .processedAt(request.getProcessedAt())
                .createdAt(request.getCreatedAt())
                .items(request.getItems().stream().map(this::mapItem).toList())
                .build();
    }

    private InventoryRequestItemResponse mapItem(InventoryRequestItem item) {
        ProductVariant variant = item.getVariant();
        return InventoryRequestItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .requestedQuantity(item.getRequestedQuantity())
                .uomCode(variant.getUom().getCode())
                .uomName(variant.getUom().getName())
                .build();
    }

    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return currentUser;
    }

    private void validateDistinctBranches(Long requestingBranchId, Long supplyingBranchId) {
        if (requestingBranchId.equals(supplyingBranchId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requesting branch and supplying branch must be different"
            );
        }
    }

    private String normalizeDirection(String direction) {
        String normalized = normalize(direction);
        if (normalized == null) {
            return null;
        }
        String value = normalized.toUpperCase(Locale.ROOT);
        if (!"INCOMING".equals(value) && !"OUTGOING".equals(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be INCOMING or OUTGOING");
        }
        return value;
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

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
