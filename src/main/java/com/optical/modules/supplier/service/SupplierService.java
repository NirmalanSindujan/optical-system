package com.optical.modules.supplier.service;

import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.supplier.dto.SupplierPageResponse;
import com.optical.modules.supplier.dto.SupplierProductStockResponse;
import com.optical.modules.supplier.dto.SupplierRequest;
import com.optical.modules.supplier.dto.SupplierResponse;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.entity.SupplierCreditEntryType;
import com.optical.modules.supplier.entity.SupplierCreditLedger;
import com.optical.modules.supplier.repository.SupplierCreditLedgerRepository;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SupplierCreditLedgerRepository supplierCreditLedgerRepository;

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String normalizedPhone = normalize(request.getPhone());
        String normalizedEmail = normalize(request.getEmail());

        if (normalizedPhone != null
                && supplierRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone)) {
            throw new DuplicateResourceException("Supplier phone already exists");
        }
        if (normalizedEmail != null
                && supplierRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new DuplicateResourceException("Supplier email already exists");
        }

        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        BigDecimal openingPendingAmount = scaleOrZero(request.getPendingAmount());
        supplier.setPendingAmount(openingPendingAmount);
        Supplier saved = supplierRepository.save(supplier);
        recordOpeningBalanceIfRequired(saved, request, openingPendingAmount);
        return mapToResponse(saved);
    }

    public SupplierPageResponse search(String q, int page, int size) {
        Page<Supplier> result = supplierRepository.search(q, PageRequest.of(page, size));
        List<SupplierResponse> items = result.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return SupplierPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    public SupplierResponse getById(Long id) {
        Supplier supplier = supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        return mapToResponse(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierProductStockResponse> getProducts(Long id) {
        supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        return productVariantRepository.findStockBySupplierId(id);
    }

    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        String normalizedPhone = normalize(request.getPhone());
        String normalizedEmail = normalize(request.getEmail());

        if (normalizedPhone != null
                && supplierRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(normalizedPhone, id)) {
            throw new DuplicateResourceException("Supplier phone already exists");
        }
        if (normalizedEmail != null
                && supplierRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("Supplier email already exists");
        }

        applyRequest(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    public void delete(Long id) {
        Supplier supplier = supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        supplier.setDeletedAt(LocalDateTime.now());
        supplierRepository.save(supplier);
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName());
        supplier.setPhone(normalize(request.getPhone()));
        supplier.setEmail(normalize(request.getEmail()));
        supplier.setAddress(normalize(request.getAddress()));
        supplier.setNotes(normalize(request.getNotes()));
    }

    private void recordOpeningBalanceIfRequired(Supplier supplier, SupplierRequest request, BigDecimal openingPendingAmount) {
        if (openingPendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        LocalDate entryDate = request.getOpeningBalanceDate() == null ? LocalDate.now() : request.getOpeningBalanceDate();
        if (entryDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "openingBalanceDate cannot be in the future");
        }

        SupplierCreditLedger ledger = new SupplierCreditLedger();
        ledger.setSupplier(supplier);
        ledger.setEntryDate(entryDate);
        ledger.setAmount(openingPendingAmount);
        ledger.setEntryType(SupplierCreditEntryType.ADJUSTMENT);
        ledger.setReference(normalize(request.getOpeningBalanceReference()) == null
                ? "OPENING_BALANCE"
                : normalize(request.getOpeningBalanceReference()));
        ledger.setNotes(normalize(request.getOpeningBalanceNotes()));
        supplierCreditLedgerRepository.save(ledger);
    }

    private BigDecimal scaleOrZero(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .notes(supplier.getNotes())
                .pendingAmount(supplier.getPendingAmount())
                .build();
    }
}
