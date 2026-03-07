package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.*;
import com.optical.modules.product.entity.*;
import com.optical.modules.product.repository.*;
import com.optical.modules.supplier.entity.Supplier;
import com.optical.modules.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SunglassService
{

    private static final String DEFAULT_PRODUCT_TYPE_CODE = ProductVariantType.SUNGLASSES.toString();

    private static final String DEFAULT_UOM_CODE = "EA";
    private static final String SUNGLASSES_SKU_PREFIX = "SUN-";

    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final ProductRepository productRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;

    private final SunglassesVariantDetailsRepository sunglassesVariantDetailsRepository;

    @Transactional(readOnly = true)
    public SunglassesPageResponse searchSunglasses(String q, int page, int size) {
        Page<SunglassesVariantDetails> result = sunglassesVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<SunglassesListResponse> items = result.getContent().stream().map(this::mapSunglassesItem).toList();
        return SunglassesPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private SunglassesListResponse mapSunglassesItem(SunglassesVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        Map<String, Object> attributes = variant.getAttributes();

        return SunglassesListResponse.builder()
                .id(product.getId())
                .modelName(product.getName())
                .company(product.getBrandName())
                .purchasePrice(parseBigDecimal(attributes.get("purchasePrice")))
                .quantity(parseBigDecimal(attributes.get("quantity")))
                .salesPrice(parseBigDecimal(attributes.get("sellingPrice")))
                .build();
    }


    @Transactional
    public ProductCreateResponse createSunglasses(SunglassesCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        String normalizedDescription = normalizeRequired(request.getDescription(), "description is required");
        List<Long> supplierIds = resolveSunglassesSupplierIds(request);

        Product product = new Product();
        product.setProductType(productType);
        product.setBrandName(companyName);
        product.setName(productName);
        product.setDescription(normalizedDescription);
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);
        linkSuppliersToProduct(savedProduct, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSku(generateSunglassesSku(savedProduct.getId()));
        variant.setBarcode(null);
        variant.setUom(uom);
        variant.setNotes(normalize(request.getNotes()));
        variant.setAttributes(buildSunglassesAttributes(request, supplierIds));
        variant.setIsActive(true);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        SunglassesVariantDetails details = new SunglassesVariantDetails();
        details.setVariant(savedVariant);
        details.setDescription(normalizedDescription);
        sunglassesVariantDetailsRepository.save(details);

        return ProductCreateResponse.builder()
                .productId(savedProduct.getId())
                .variantId(savedVariant.getId())
                .productTypeCode(productType.getCode())
                .productName(savedProduct.getName())
                .sku(savedVariant.getSku())
                .barcode(savedVariant.getBarcode())
                .variantType(ProductVariantType.SUNGLASSES)
                .lensSubType(null)
                .productActive(savedProduct.getIsActive())
                .variantActive(savedVariant.getIsActive())
                .supplierId(supplierIds.get(0))
                .supplierIds(supplierIds)
                .suppliers(resolveSupplierInfos(supplierIds))
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantity(request.getQuantity())
                .build();
    }



    private BigDecimal parseBigDecimal(Object value) {
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
    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private List<Long> resolveSunglassesSupplierIds(SunglassesCreateRequest request) {
        List<Long> requestSupplierIds = request.getSupplierIds();
        if (requestSupplierIds != null && !requestSupplierIds.isEmpty()) {
            return resolveAndValidateSupplierIds(requestSupplierIds, "At least one valid supplier id is required");
        }

        if (request.getSupplierId() != null) {
            return resolveAndValidateSupplierIds(List.of(request.getSupplierId()), "supplierIds or supplierId is required");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierIds or supplierId is required");
    }

    private List<Long> resolveFrameSupplierIds(FrameCreateRequest request) {
        List<Long> requestSupplierIds = request.getSupplierIds();
        if (requestSupplierIds != null && !requestSupplierIds.isEmpty()) {
            return resolveAndValidateSupplierIds(requestSupplierIds, "At least one valid supplier id is required");
        }

        if (request.getSupplierId() != null) {
            return resolveAndValidateSupplierIds(List.of(request.getSupplierId()), "supplierIds or supplierId is required");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierIds or supplierId is required");
    }

    private List<Long> resolveAndValidateSupplierIds(List<Long> rawSupplierIds, String requiredMessage) {
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


    private void linkSuppliersToProduct(Product product, List<Long> supplierIds) {
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

    private List<SupplierInfoResponse> resolveSupplierInfos(List<Long> supplierIds) {
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

    private SupplierInfoResponse mapSupplierInfo(Supplier supplier) {
        return SupplierInfoResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .build();
    }

    private String generateSunglassesSku(Long productId) {
        String baseSku = SUNGLASSES_SKU_PREFIX + productId;
        if (!productVariantRepository.existsBySkuAndDeletedAtIsNull(baseSku)) {
            return baseSku;
        }

        for (int i = 0; i < 5; i++) {
            String candidate = baseSku + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!productVariantRepository.existsBySkuAndDeletedAtIsNull(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to generate unique SKU");
    }


    private Map<String, Object> buildSunglassesAttributes(SunglassesCreateRequest request, List<Long> supplierIds) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("supplierIds", supplierIds);
        attributes.put("supplierId", supplierIds.get(0));
        attributes.put("purchasePrice", request.getPurchasePrice());
        attributes.put("sellingPrice", request.getSellingPrice());
        attributes.put("quantity", request.getQuantity());
        return attributes;
    }


}
