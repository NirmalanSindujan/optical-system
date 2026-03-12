package com.optical.modules.purchase.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.BranchInventoryId;
import com.optical.modules.product.entity.InventoryLot;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.repository.BranchInventoryRepository;
import com.optical.modules.product.repository.InventoryLotRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.SupplierProductRepository;
import com.optical.modules.purchase.dto.StockPurchaseCreateRequest;
import com.optical.modules.purchase.dto.StockPurchaseItemRequest;
import com.optical.modules.purchase.dto.StockPurchaseItemResponse;
import com.optical.modules.purchase.dto.StockPurchasePageResponse;
import com.optical.modules.purchase.dto.StockPurchaseResponse;
import com.optical.modules.purchase.entity.PaymentMode;
import com.optical.modules.purchase.entity.StockPurchase;
import com.optical.modules.purchase.entity.StockPurchaseItem;
import com.optical.modules.purchase.repository.StockPurchaseRepository;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class StockPurchaseService {

    private final StockPurchaseRepository stockPurchaseRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;

    @Transactional
    public StockPurchaseResponse create(StockPurchaseCreateRequest request) {
        Supplier supplier = supplierRepository.findByIdAndDeletedAtIsNull(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        Branch branch = resolveBranch(request.getBranchId());

        List<StockPurchaseItem> items = buildItems(request.getItems(), supplier.getId());
        BigDecimal totalAmount = items.stream()
                .map(StockPurchaseItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = resolvePaidAmount(request.getPaymentMode(), request.getPaidAmount(), totalAmount);
        BigDecimal balanceAmount = totalAmount.subtract(paidAmount);

        StockPurchase purchase = new StockPurchase();
        purchase.setSupplier(supplier);
        purchase.setBranch(branch);
        purchase.setBillNumber(normalize(request.getBillNumber()));
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setPaymentMode(request.getPaymentMode());
        purchase.setTotalAmount(scale(totalAmount));
        purchase.setPaidAmount(scale(paidAmount));
        purchase.setBalanceAmount(scale(balanceAmount));
        purchase.setCurrencyCode(resolveCurrencyCode(request.getCurrencyCode()));
        purchase.setNotes(normalize(request.getNotes()));
        for (StockPurchaseItem item : items) {
            item.setStockPurchase(purchase);
            purchase.getItems().add(item);
        }

        StockPurchase savedPurchase = stockPurchaseRepository.save(purchase);
        applyInventoryEffects(savedPurchase, branch, supplier);
        if (balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            recordCreditPurchase(savedPurchase, supplier, balanceAmount);
        }

        return mapResponse(savedPurchase);
    }

    @Transactional(readOnly = true)
    public StockPurchaseResponse getById(Long id) {
        StockPurchase purchase = stockPurchaseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock purchase not found"));
        return mapResponse(purchase);
    }

    @Transactional(readOnly = true)
    public StockPurchasePageResponse search(String q, int page, int size) {
        String keyword = normalize(q);
        Page<Long> result = resolvePurchasePage(keyword, page, size);
        List<StockPurchaseResponse> items = loadDetailedPurchases(result.getContent()).stream()
                .map(this::mapResponse)
                .toList();

        return StockPurchasePageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private Page<Long> resolvePurchasePage(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (keyword == null) {
            return stockPurchaseRepository.findActiveIds(pageable);
        }

        try {
            return stockPurchaseRepository.findActiveIdsById(Long.parseLong(keyword), pageable);
        } catch (NumberFormatException ignored) {
            return Page.empty(pageable);
        }
    }

    private List<StockPurchaseItem> buildItems(List<StockPurchaseItemRequest> requests, Long supplierId) {
        Map<Long, ProductVariant> variantsById = loadVariants(requests);
        List<StockPurchaseItem> items = new ArrayList<>();

        for (StockPurchaseItemRequest request : requests) {
            ProductVariant variant = variantsById.get(request.getVariantId());
            if (!supplierProductRepository.existsBySupplierIdAndProductIdAndDeletedAtIsNull(
                    supplierId,
                    variant.getProduct().getId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Supplier is not linked to product for variant: " + request.getVariantId()
                );
            }

            StockPurchaseItem item = new StockPurchaseItem();
            item.setVariant(variant);
            item.setQuantity(scale(request.getQuantity()));
            item.setPurchasePrice(scale(request.getPurchasePrice()));
            item.setLineTotal(scale(request.getQuantity().multiply(request.getPurchasePrice())));
            item.setNotes(normalize(request.getNotes()));
            items.add(item);
        }

        return items;
    }

    private Map<Long, ProductVariant> loadVariants(List<StockPurchaseItemRequest> requests) {
        List<Long> variantIds = requests.stream()
                .map(StockPurchaseItemRequest::getVariantId)
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

    private Branch resolveBranch(Long branchId) {
        if (branchId != null) {
            return branchRepository.findByIdAndDeletedAtIsNull(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }
        return branchRepository.findFirstByIsMainTrueAndDeletedAtIsNull()
                .orElseThrow(() -> new ResourceNotFoundException("Main branch not found"));
    }

    private BigDecimal resolvePaidAmount(PaymentMode paymentMode, BigDecimal rawPaidAmount, BigDecimal totalAmount) {
        BigDecimal paidAmount = rawPaidAmount == null
                ? (paymentMode == PaymentMode.CREDIT ? BigDecimal.ZERO : totalAmount)
                : scale(rawPaidAmount);

        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paidAmount cannot be negative");
        }
        if (paidAmount.compareTo(totalAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paidAmount cannot exceed total amount");
        }
        if (paymentMode != PaymentMode.CREDIT && paidAmount.compareTo(totalAmount) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use CREDIT payment mode for purchases with an outstanding balance"
            );
        }

        return paidAmount;
    }

    private void applyInventoryEffects(StockPurchase purchase, Branch branch, Supplier supplier) {
        Map<String, BranchInventory> inventoryUpdates = new LinkedHashMap<>();
        List<InventoryLot> lots = new ArrayList<>();
        LocalDateTime purchasedAt = purchase.getPurchaseDate().atStartOfDay();

        for (StockPurchaseItem item : purchase.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.setQuantity(scale(zeroIfNull(variant.getQuantity()).add(item.getQuantity())));
            variant.setPurchasePrice(item.getPurchasePrice());

            String inventoryKey = branch.getId() + ":" + variant.getId();
            BranchInventory inventory = inventoryUpdates.computeIfAbsent(inventoryKey, ignored -> {
                BranchInventoryId inventoryId = new BranchInventoryId();
                inventoryId.setBranchId(branch.getId());
                inventoryId.setVariantId(variant.getId());
                return branchInventoryRepository.findById(inventoryId).orElseGet(() -> {
                    BranchInventory created = new BranchInventory();
                    created.setId(inventoryId);
                    created.setBranch(branch);
                    created.setVariant(variant);
                    return created;
                });
            });
            inventory.setOnHand(scale(zeroIfNull(inventory.getOnHand()).add(item.getQuantity())));

            InventoryLot lot = new InventoryLot();
            lot.setVariant(variant);
            lot.setSupplierId(supplier.getId());
            lot.setPurchasedAt(purchasedAt);
            lot.setPurchaseCost(item.getPurchasePrice());
            lot.setCurrencyCode(purchase.getCurrencyCode());
            lot.setQtyReceived(item.getQuantity());
            lot.setQtyRemaining(item.getQuantity());
            lot.setNotes(buildLotNote(purchase, item));
            lots.add(lot);
        }

        branchInventoryRepository.saveAll(inventoryUpdates.values());
        inventoryLotRepository.saveAll(lots);
    }

    private void recordCreditPurchase(StockPurchase purchase, Supplier supplier, BigDecimal balanceAmount) {
        supplier.setPendingAmount(scale(zeroIfNull(supplier.getPendingAmount()).add(balanceAmount)));

        SupplierCreditLedger ledger = new SupplierCreditLedger();
        ledger.setSupplier(supplier);
        ledger.setStockPurchase(purchase);
        ledger.setEntryDate(purchase.getPurchaseDate());
        ledger.setAmount(scale(balanceAmount));
        ledger.setEntryType(SupplierCreditEntryType.PURCHASE);
        ledger.setPaymentMode(purchase.getPaymentMode());
        ledger.setReference(resolvePurchaseReference(purchase));
        ledger.setNotes(purchase.getNotes());
        supplierCreditLedgerRepository.save(ledger);
    }

    private List<StockPurchase> loadDetailedPurchases(List<Long> purchaseIds) {
        if (purchaseIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> indexById = new HashMap<>();
        for (int i = 0; i < purchaseIds.size(); i++) {
            indexById.put(purchaseIds.get(i), i);
        }

        return stockPurchaseRepository.findDetailedByIdIn(purchaseIds).stream()
                .sorted(Comparator.comparingInt(purchase -> indexById.getOrDefault(purchase.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private StockPurchaseResponse mapResponse(StockPurchase purchase) {
        return StockPurchaseResponse.builder()
                .id(purchase.getId())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getName())
                .branchId(purchase.getBranch().getId())
                .branchName(purchase.getBranch().getName())
                .billNumber(purchase.getBillNumber())
                .purchaseDate(purchase.getPurchaseDate())
                .paymentMode(purchase.getPaymentMode())
                .totalAmount(purchase.getTotalAmount())
                .paidAmount(purchase.getPaidAmount())
                .balanceAmount(purchase.getBalanceAmount())
                .currencyCode(purchase.getCurrencyCode())
                .notes(purchase.getNotes())
                .supplierPendingAmount(purchase.getSupplier().getPendingAmount())
                .items(purchase.getItems().stream().map(this::mapItem).toList())
                .build();
    }

    private StockPurchaseItemResponse mapItem(StockPurchaseItem item) {
        ProductVariant variant = item.getVariant();
        return StockPurchaseItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .quantity(item.getQuantity())
                .purchasePrice(item.getPurchasePrice())
                .lineTotal(item.getLineTotal())
                .notes(item.getNotes())
                .build();
    }

    private String buildLotNote(StockPurchase purchase, StockPurchaseItem item) {
        String reference = resolvePurchaseReference(purchase);
        if (item.getNotes() == null) {
            return reference;
        }
        return reference + " | " + item.getNotes();
    }

    private String resolvePurchaseReference(StockPurchase purchase) {
        if (purchase.getBillNumber() != null) {
            return purchase.getBillNumber();
        }
        return "PURCHASE-" + purchase.getId();
    }

    private String resolveCurrencyCode(String currencyCode) {
        String normalizedCurrency = normalize(currencyCode);
        if (normalizedCurrency == null) {
            return "LKR";
        }
        String value = normalizedCurrency.toUpperCase();
        if (value.length() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currencyCode must be a 3 letter code");
        }
        return value;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
