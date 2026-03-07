package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.AccessoryCreateRequest;
import com.optical.modules.product.dto.AccessoryDetailResponse;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductListResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.entity.AccessoryVariantDetails;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductType;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.Uom;
import com.optical.modules.product.repository.AccessoryVariantDetailsRepository;
import com.optical.modules.product.repository.ProductRepository;
import com.optical.modules.product.repository.ProductTypeRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class AccessoryService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = ProductVariantType.ACCESSORY.name();
    private static final String DEFAULT_UOM_CODE = "EA";
    private static final String ACCESSORY_SKU_PREFIX = "ACC-";
    private static final Map<String, String> ALLOWED_TYPES = createAllowedTypes();

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final AccessoryVariantDetailsRepository accessoryVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional(readOnly = true)
    public ProductPageResponse search(String q, int page, int size) {
        Page<AccessoryVariantDetails> result = accessoryVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapAccessoryListItem)
                .toList();

        return ProductPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public ProductCreateResponse create(AccessoryCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String modelName = normalizeRequired(request.getModelName(), "modelName is required");
        String itemType = normalizeAndValidateType(request.getType());
        validateCommercialFields(request, itemType);
        List<Long> supplierIds = resolveSupplierIds(request, itemType);

        Product product = new Product();
        product.setProductType(productType);
        product.setBrandName(companyName);
        product.setName(modelName);
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);
        productSupportService.linkSuppliersToProduct(savedProduct, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSku(generateAccessorySku(savedProduct.getId()));
        variant.setBarcode(null);
        variant.setUom(uom);
        variant.setNotes(normalize(request.getExtra()));
        variant.setAttributes(buildAttributes(request, supplierIds));
        variant.setIsActive(true);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        AccessoryVariantDetails details = new AccessoryVariantDetails();
        details.setVariant(savedVariant);
        details.setItemType(itemType);
        accessoryVariantDetailsRepository.save(details);

        return ProductCreateResponse.builder()
                .productId(savedProduct.getId())
                .variantId(savedVariant.getId())
                .productTypeCode(productType.getCode())
                .productName(savedProduct.getName())
                .sku(savedVariant.getSku())
                .barcode(savedVariant.getBarcode())
                .variantType(ProductVariantType.ACCESSORY)
                .productActive(savedProduct.getIsActive())
                .variantActive(savedVariant.getIsActive())
                .supplierId(firstSupplierId(supplierIds))
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantity(request.getQuantity())
                .build();
    }

    @Transactional(readOnly = true)
    public AccessoryDetailResponse getById(Long productId) {
        return mapAccessoryDetail(findAccessoryByProductId(productId));
    }

    @Transactional
    public AccessoryDetailResponse update(Long productId, AccessoryCreateRequest request) {
        AccessoryVariantDetails details = findAccessoryByProductId(productId);
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String modelName = normalizeRequired(request.getModelName(), "modelName is required");
        String itemType = normalizeAndValidateType(request.getType());
        validateCommercialFields(request, itemType);
        List<Long> supplierIds = resolveSupplierIds(request, itemType);

        product.setBrandName(companyName);
        product.setName(modelName);
        variant.setNotes(normalize(request.getExtra()));
        variant.setAttributes(buildAttributes(request, supplierIds));

        details.setItemType(itemType);
        productSupportService.replaceSupplierLinks(product, supplierIds);

        return mapAccessoryDetail(details);
    }

    @Transactional
    public void delete(Long productId) {
        findAccessoryByProductId(productId);
        productSupportService.deleteProduct(productId);
    }

    private AccessoryVariantDetails findAccessoryByProductId(Long productId) {
        return accessoryVariantDetailsRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessory not found"));
    }

    private List<Long> resolveSupplierIds(AccessoryCreateRequest request, String itemType) {
        if (request.getSupplierIds() != null && !request.getSupplierIds().isEmpty()) {
            return productSupportService.resolveAndValidateSupplierIds(
                    request.getSupplierIds(),
                    "At least one valid supplier id is required"
            );
        }
        if (request.getSupplierId() != null) {
            return productSupportService.resolveAndValidateSupplierIds(
                    List.of(request.getSupplierId()),
                    "supplierIds or supplierId is required"
            );
        }
        if (isServiceType(itemType)) {
            return List.of();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierIds or supplierId is required");
    }

    private Map<String, Object> buildAttributes(AccessoryCreateRequest request, List<Long> supplierIds) {
        Map<String, Object> attributes = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            attributes.put("supplierIds", supplierIds);
            attributes.put("supplierId", supplierIds.get(0));
        }
        if (request.getPurchasePrice() != null) {
            attributes.put("purchasePrice", request.getPurchasePrice());
        }
        attributes.put("sellingPrice", request.getSellingPrice());
        if (request.getQuantity() != null) {
            attributes.put("quantity", request.getQuantity());
        }
        return attributes;
    }

    private ProductListResponse mapAccessoryListItem(AccessoryVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        Map<String, Object> attributes = variant.getAttributes();

        return ProductListResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .productTypeCode(product.getProductType().getCode())
                .brandName(product.getBrandName())
                .name(product.getName())
                .description(product.getDescription())
                .productActive(product.getIsActive())
                .variantActive(variant.getIsActive())
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .uomCode(variant.getUom().getCode())
                .notes(variant.getNotes())
                .attributes(attributes)
                .variantType(ProductVariantType.ACCESSORY)
                .supplierId(productSupportService.parseLong(attributes.get("supplierId")))
                .suppliers(productSupportService.resolveSupplierInfosForProduct(product.getId(), attributes))
                .purchasePrice(productSupportService.parseBigDecimal(attributes.get("purchasePrice")))
                .sellingPrice(productSupportService.parseBigDecimal(attributes.get("sellingPrice")))
                .quantity(productSupportService.parseBigDecimal(attributes.get("quantity")))
                .itemType(details.getItemType())
                .build();
    }

    private AccessoryDetailResponse mapAccessoryDetail(AccessoryVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        Map<String, Object> attributes = variant.getAttributes();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId(), attributes);

        return AccessoryDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .modelName(product.getName())
                .type(details.getItemType())
                .quantity(productSupportService.parseBigDecimal(attributes.get("quantity")))
                .purchasePrice(productSupportService.parseBigDecimal(attributes.get("purchasePrice")))
                .sellingPrice(productSupportService.parseBigDecimal(attributes.get("sellingPrice")))
                .extra(variant.getNotes())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .build();
    }

    private String generateAccessorySku(Long productId) {
        String baseSku = ACCESSORY_SKU_PREFIX + productId;
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

    private String normalizeAndValidateType(String value) {
        String normalized = normalizeRequired(value, "type is required");
        String canonical = ALLOWED_TYPES.get(normalized.toLowerCase());
        if (canonical == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid accessory type. Allowed: " + String.join(", ", ALLOWED_TYPES.values())
            );
        }
        return canonical;
    }

    private void validateCommercialFields(AccessoryCreateRequest request, String itemType) {
        if (request.getSellingPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice is required");
        }
        if (request.getSellingPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice cannot be negative");
        }

        if (isServiceType(itemType)) {
            return;
        }

        if (request.getPurchasePrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice is required for Product type");
        }
        if (request.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity is required for Product type");
        }
        if (request.getPurchasePrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice cannot be negative");
        }
        if (request.getQuantity().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity cannot be negative");
        }
    }

    private boolean isServiceType(String itemType) {
        return "Service".equalsIgnoreCase(itemType);
    }

    private Long firstSupplierId(List<Long> supplierIds) {
        return supplierIds == null || supplierIds.isEmpty() ? null : supplierIds.get(0);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private static Map<String, String> createAllowedTypes() {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("product", "Product");
        allowed.put("service", "Service");
        allowed.put("services", "Service");
        return Map.copyOf(allowed);
    }
}
