package com.optical.modules.supplier.service;

import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.supplier.dto.SupplierPageResponse;
import com.optical.modules.supplier.dto.SupplierProductStockResponse;
import com.optical.modules.supplier.dto.SupplierRequest;
import com.optical.modules.supplier.dto.SupplierResponse;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;

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
        Supplier saved = supplierRepository.save(supplier);
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
