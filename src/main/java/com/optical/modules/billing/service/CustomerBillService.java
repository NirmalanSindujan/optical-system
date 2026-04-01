package com.optical.modules.billing.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.billing.dto.BranchCollectionSummaryResponse;
import com.optical.modules.billing.dto.CustomerBillCreateRequest;
import com.optical.modules.billing.dto.CustomerBillItemRequest;
import com.optical.modules.billing.dto.CustomerBillItemResponse;
import com.optical.modules.billing.dto.CustomerBillPageResponse;
import com.optical.modules.billing.dto.CustomerBillPaymentRequest;
import com.optical.modules.billing.dto.CustomerBillPaymentResponse;
import com.optical.modules.billing.dto.CustomerBillResponse;
import com.optical.modules.billing.dto.CustomerBillSummaryResponse;
import com.optical.modules.billing.entity.CustomerBill;
import com.optical.modules.billing.entity.CustomerBillItem;
import com.optical.modules.billing.entity.CustomerBillPayment;
import com.optical.modules.billing.repository.CustomerBillItemRepository;
import com.optical.modules.billing.repository.CustomerBillPaymentRepository;
import com.optical.modules.billing.repository.CustomerBillRepository;
import com.optical.modules.branch.entity.Branch;
import com.optical.modules.branch.repository.BranchRepository;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.customer.entity.CustomerCreditLedger;
import com.optical.modules.customer.repository.CustomerCreditLedgerRepository;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.product.entity.BranchInventory;
import com.optical.modules.product.entity.InventoryLot;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.repository.BranchInventoryRepository;
import com.optical.modules.product.repository.InventoryLotRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.purchase.entity.PaymentMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class CustomerBillService {

    private final CustomerBillRepository customerBillRepository;
    private final CustomerBillItemRepository customerBillItemRepository;
    private final CustomerBillPaymentRepository customerBillPaymentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerCreditLedgerRepository customerCreditLedgerRepository;
    private final BranchRepository branchRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryLotRepository inventoryLotRepository;

    @Transactional
    public CustomerBillResponse create(CustomerBillCreateRequest request) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        Customer customer = resolveCustomer(request.getCustomerId(), request.getPayments());
        List<CustomerBillItem> items = buildItems(request.getItems());

        BigDecimal subtotalAmount = items.stream()
                .map(CustomerBillItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = resolveDiscountAmount(request.getDiscountAmount(), subtotalAmount);
        BigDecimal totalAmount = scale(subtotalAmount.subtract(discountAmount));
        List<CustomerBillPayment> payments = buildPayments(request.getPayments(), totalAmount);
        BigDecimal paidAmount = resolvePaidAmount(payments);
        BigDecimal balanceAmount = resolveBalanceAmount(payments);

        validateBranchStock(branch.getId(), items);

        CustomerBill bill = new CustomerBill();
        bill.setCustomer(customer);
        bill.setBranch(branch);
        bill.setBillNumber(normalize(request.getBillNumber()));
        bill.setBillDate(request.getBillDate());
        bill.setSubtotalAmount(scale(subtotalAmount));
        bill.setDiscountAmount(discountAmount);
        bill.setTotalAmount(scale(totalAmount));
        bill.setPaidAmount(paidAmount);
        bill.setBalanceAmount(balanceAmount);
        bill.setCurrencyCode(resolveCurrencyCode(request.getCurrencyCode()));
        bill.setNotes(normalize(request.getNotes()));
        for (CustomerBillItem item : items) {
            item.setCustomerBill(bill);
            bill.getItems().add(item);
        }
        for (CustomerBillPayment payment : payments) {
            payment.setCustomerBill(bill);
            bill.getPayments().add(payment);
        }

        CustomerBill saved = customerBillRepository.save(bill);
        applyInventoryEffects(branch, saved.getItems());
        if (customer != null && balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            recordCustomerCredit(saved, customer);
        }

        return mapResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerBillResponse getById(Long id) {
        CustomerBill bill = customerBillRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer bill not found"));
        hydrateBillDetails(bill);
        return mapResponse(bill);
    }

    @Transactional(readOnly = true)
    public CustomerBillPageResponse search(String q, Long branchId, int page, int size) {
        String keyword = normalize(q);
        Page<Long> result = keyword == null
                ? customerBillRepository.findActiveIdsByBranchId(branchId, PageRequest.of(page, size))
                : customerBillRepository.findActiveIds(keyword, branchId, PageRequest.of(page, size));
        List<CustomerBillSummaryResponse> items = loadDetailedBills(result.getContent()).stream()
                .map(this::mapSummaryResponse)
                .toList();

        return CustomerBillPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public BranchCollectionSummaryResponse getBranchCollectionSummary(Long branchId) {
        Branch branch = branchRepository.findByIdAndDeletedAtIsNull(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        return BranchCollectionSummaryResponse.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .totalSales(scale(customerBillRepository.sumTotalAmountByBranchId(branchId)))
                .cashInHand(scale(customerBillRepository.sumTotalAmountByBranchIdAndPaymentMode(branchId, PaymentMode.CASH)))
                .universalBankBalance(scale(customerBillRepository.sumTotalAmountByPaymentMode(PaymentMode.BANK)))
                .chequeCollections(scale(customerBillRepository.sumTotalAmountByBranchIdAndPaymentMode(branchId, PaymentMode.CHEQUE)))
                .creditOutstanding(scale(customerBillRepository.sumBalanceAmountByBranchId(branchId)))
                .build();
    }

    private Customer resolveCustomer(Long customerId, List<CustomerBillPaymentRequest> payments) {
        boolean requiresCustomer = payments.stream()
                .map(CustomerBillPaymentRequest::getPaymentMode)
                .anyMatch(mode -> mode != PaymentMode.CASH);

        if (customerId == null) {
            if (requiresCustomer) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required for bank, cheque, or credit bills");
            }
            return null;
        }

        return customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private List<CustomerBillItem> buildItems(List<CustomerBillItemRequest> requests) {
        Map<Long, ProductVariant> variantsById = loadVariants(requests);
        List<CustomerBillItem> items = new ArrayList<>();

        for (CustomerBillItemRequest request : requests) {
            ProductVariant variant = variantsById.get(request.getVariantId());
            BigDecimal unitPrice = request.getUnitPrice() == null ? variant.getSellingPrice() : request.getUnitPrice();
            if (unitPrice == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "unitPrice is required when variant selling price is not set: " + request.getVariantId()
                );
            }

            CustomerBillItem item = new CustomerBillItem();
            item.setVariant(variant);
            item.setQuantity(scale(request.getQuantity()));
            item.setUnitPrice(scale(unitPrice));
            item.setLineTotal(scale(request.getQuantity().multiply(unitPrice)));
            items.add(item);
        }

        return items;
    }

    private List<CustomerBillPayment> buildPayments(List<CustomerBillPaymentRequest> requests, BigDecimal totalAmount) {
        List<CustomerBillPayment> payments = new ArrayList<>();
        BigDecimal totalPaymentAmount = BigDecimal.ZERO;

        for (CustomerBillPaymentRequest request : requests) {
            BigDecimal amount = scale(request.getAmount());
            CustomerBillPayment payment = new CustomerBillPayment();
            payment.setPaymentMode(request.getPaymentMode());
            payment.setAmount(amount);
            payment.setReference(normalize(request.getReference()));

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment amount cannot be negative");
            }

            if (request.getPaymentMode() == PaymentMode.CHEQUE) {
                validateChequeDetails(request);
                payment.setChequeNumber(normalize(request.getChequeNumber()));
                payment.setChequeDate(request.getChequeDate());
                payment.setChequeBankName(normalize(request.getChequeBankName()));
                payment.setChequeBranchName(normalize(request.getChequeBranchName()));
                payment.setChequeAccountHolder(normalize(request.getChequeAccountHolder()));
            }

            payments.add(payment);
            totalPaymentAmount = totalPaymentAmount.add(amount);
        }

        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one payment line is required");
        }
        if (scale(totalPaymentAmount).compareTo(scale(totalAmount)) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sum of payment amounts must equal the final bill total");
        }

        return payments;
    }

    private Map<Long, ProductVariant> loadVariants(List<CustomerBillItemRequest> requests) {
        List<Long> variantIds = requests.stream()
                .map(CustomerBillItemRequest::getVariantId)
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

    private void validateBranchStock(Long branchId, List<CustomerBillItem> items) {
        for (CustomerBillItem item : items) {
            BranchInventory inventory = branchInventoryRepository.findByBranch_IdAndVariant_Id(branchId, item.getVariant().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No stock found in branch for variant: " + item.getVariant().getId()
                    ));

            BigDecimal available = scale(zeroIfNull(inventory.getOnHand()).subtract(zeroIfNull(inventory.getReserved())));
            if (available.compareTo(item.getQuantity()) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock in branch for variant: " + item.getVariant().getId()
                );
            }
        }
    }

    private void applyInventoryEffects(Branch branch, List<CustomerBillItem> items) {
        Map<Long, BranchInventory> inventoryUpdates = new LinkedHashMap<>();
        Map<Long, ProductVariant> variantUpdates = new LinkedHashMap<>();
        Map<Long, List<InventoryLot>> lotsByVariant = new LinkedHashMap<>();

        for (CustomerBillItem item : items) {
            ProductVariant variant = item.getVariant();
            variant.setQuantity(scale(zeroIfNull(variant.getQuantity()).subtract(item.getQuantity())));
            variantUpdates.put(variant.getId(), variant);

            BranchInventory inventory = inventoryUpdates.computeIfAbsent(variant.getId(), ignored ->
                    branchInventoryRepository.findByBranch_IdAndVariant_Id(branch.getId(), variant.getId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "No stock found in branch for variant: " + variant.getId()
                            )));
            inventory.setOnHand(scale(zeroIfNull(inventory.getOnHand()).subtract(item.getQuantity())));

            consumeLots(variant.getId(), item.getQuantity(), lotsByVariant);
        }

        productVariantRepository.saveAll(variantUpdates.values());
        branchInventoryRepository.saveAll(inventoryUpdates.values());
        lotsByVariant.values().forEach(inventoryLotRepository::saveAll);
    }

    private void consumeLots(Long variantId, BigDecimal requestedQty, Map<Long, List<InventoryLot>> lotsByVariant) {
        BigDecimal remaining = scale(requestedQty);
        List<InventoryLot> lots = lotsByVariant.computeIfAbsent(
                variantId,
                ignored -> new ArrayList<>(inventoryLotRepository.findAvailableLotsByVariantIdOrderByPurchasedAtAsc(variantId))
        );

        for (InventoryLot lot : lots) {
            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }

            BigDecimal available = scale(zeroIfNull(lot.getQtyRemaining()));
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumed = available.min(remaining);
            lot.setQtyRemaining(scale(available.subtract(consumed)));
            remaining = scale(remaining.subtract(consumed));
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient lot balance for variant: " + variantId);
        }
    }

    private void recordCustomerCredit(CustomerBill bill, Customer customer) {
        customer.setPendingAmount(scale(zeroIfNull(customer.getPendingAmount()).add(bill.getBalanceAmount())));

        CustomerCreditLedger ledger = new CustomerCreditLedger();
        ledger.setCustomer(customer);
        ledger.setAmount(scale(bill.getBalanceAmount()));
        ledger.setEntryType("BILL");
        ledger.setReference(resolveBillReference(bill));
        customerCreditLedgerRepository.save(ledger);
    }

    private void validateChequeDetails(CustomerBillPaymentRequest request) {
        if (normalize(request.getChequeNumber()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeNumber is required for cheque payments");
        }
        if (request.getChequeDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeDate is required for cheque payments");
        }
        if (normalize(request.getChequeBankName()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chequeBankName is required for cheque payments");
        }
    }

    private BigDecimal resolvePaidAmount(List<CustomerBillPayment> payments) {
        return scale(payments.stream()
                .filter(payment -> payment.getPaymentMode() != PaymentMode.CREDIT)
                .map(CustomerBillPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal resolveBalanceAmount(List<CustomerBillPayment> payments) {
        return scale(payments.stream()
                .filter(payment -> payment.getPaymentMode() == PaymentMode.CREDIT)
                .map(CustomerBillPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal resolveDiscountAmount(BigDecimal rawDiscountAmount, BigDecimal subtotalAmount) {
        BigDecimal discountAmount = rawDiscountAmount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : scale(rawDiscountAmount);

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountAmount cannot be negative");
        }
        if (discountAmount.compareTo(scale(subtotalAmount)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountAmount cannot exceed subtotalAmount");
        }
        return discountAmount;
    }

    private List<CustomerBill> loadDetailedBills(List<Long> billIds) {
        if (billIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> indexById = new HashMap<>();
        for (int i = 0; i < billIds.size(); i++) {
            indexById.put(billIds.get(i), i);
        }

        return customerBillRepository.findByIdIn(billIds).stream()
                .sorted(Comparator.comparingInt(bill -> indexById.getOrDefault(bill.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private void hydrateBillDetails(CustomerBill bill) {
        bill.setItems(customerBillItemRepository.findByCustomerBillIdOrderByIdAsc(bill.getId()));
        bill.setPayments(customerBillPaymentRepository.findByCustomerBillIdOrderByIdAsc(bill.getId()));
    }

    private CustomerBillResponse mapResponse(CustomerBill bill) {
        return CustomerBillResponse.builder()
                .id(bill.getId())
                .customerId(bill.getCustomer() == null ? null : bill.getCustomer().getId())
                .customerName(bill.getCustomer() == null ? null : bill.getCustomer().getName())
                .customerPendingAmount(bill.getCustomer() == null ? null : bill.getCustomer().getPendingAmount())
                .branchId(bill.getBranch().getId())
                .branchName(bill.getBranch().getName())
                .billNumber(resolveBillReference(bill))
                .billDate(bill.getBillDate())
                .subtotalAmount(bill.getSubtotalAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .balanceAmount(bill.getBalanceAmount())
                .currencyCode(bill.getCurrencyCode())
                .notes(bill.getNotes())
                .items(bill.getItems().stream().map(this::mapItem).toList())
                .payments(bill.getPayments().stream().map(this::mapPayment).toList())
                .build();
    }

    private CustomerBillSummaryResponse mapSummaryResponse(CustomerBill bill) {
        return CustomerBillSummaryResponse.builder()
                .id(bill.getId())
                .customerId(bill.getCustomer() == null ? null : bill.getCustomer().getId())
                .customerName(bill.getCustomer() == null ? null : bill.getCustomer().getName())
                .branchId(bill.getBranch().getId())
                .branchName(bill.getBranch().getName())
                .billNumber(resolveBillReference(bill))
                .billDate(bill.getBillDate())
                .subtotalAmount(bill.getSubtotalAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .balanceAmount(bill.getBalanceAmount())
                .currencyCode(bill.getCurrencyCode())
                .build();
    }

    private CustomerBillItemResponse mapItem(CustomerBillItem item) {
        ProductVariant variant = item.getVariant();
        return CustomerBillItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private CustomerBillPaymentResponse mapPayment(CustomerBillPayment payment) {
        return CustomerBillPaymentResponse.builder()
                .id(payment.getId())
                .paymentMode(payment.getPaymentMode())
                .amount(payment.getAmount())
                .chequeNumber(payment.getChequeNumber())
                .chequeDate(payment.getChequeDate())
                .chequeBankName(payment.getChequeBankName())
                .chequeBranchName(payment.getChequeBranchName())
                .chequeAccountHolder(payment.getChequeAccountHolder())
                .reference(payment.getReference())
                .build();
    }

    private String resolveBillReference(CustomerBill bill) {
        if (bill.getBillNumber() != null) {
            return bill.getBillNumber();
        }
        return "BILL-" + bill.getId();
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
