package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.FrameCreateRequest;
import com.optical.modules.product.dto.FrameDetailResponse;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductListResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.entity.FrameVariantDetails;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductType;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.Uom;
import com.optical.modules.product.repository.FrameVariantDetailsRepository;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class FrameService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = ProductVariantType.FRAME.name();
    private static final String DEFAULT_UOM_CODE = "EA";
    private static final String FRAME_SKU_PREFIX = "FRM-";
    private static final Map<String, String> ALLOWED_FRAME_TYPES = createAllowedFrameTypes();

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final FrameVariantDetailsRepository frameVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional
    public ProductCreateResponse create(FrameCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String name = normalizeRequired(request.getName(), "name is required");
        String code = normalizeRequired(request.getCode(), "code is required");
        String type = normalizeAndValidateFrameType(request.getType());
        String color = normalizeRequired(request.getColor(), "color is required");
        String size = normalizeRequired(request.getSize(), "size is required");
        List<Long> supplierIds = resolveSupplierIds(request);

        Product product = new Product();
        product.setProductType(productType);
        product.setName(name);
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);
        productSupportService.linkSuppliersToProduct(savedProduct, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSku(generateFrameSku(savedProduct.getId()));
        variant.setBarcode(null);
        variant.setUom(uom);
        variant.setNotes(normalize(request.getExtra()));
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setQuantity(request.getQuantity());
        variant.setIsActive(true);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        productSupportService.initializeMainBranchInventory(savedVariant);

        FrameVariantDetails details = new FrameVariantDetails();
        details.setVariant(savedVariant);
        details.setFrameCode(code);
        details.setFrameType(type);
        details.setColor(color);
        details.setSize(size);
        frameVariantDetailsRepository.save(details);

        return ProductCreateResponse.builder()
                .productId(savedProduct.getId())
                .variantId(savedVariant.getId())
                .productTypeCode(productType.getCode())
                .productName(savedProduct.getName())
                .sku(savedVariant.getSku())
                .barcode(savedVariant.getBarcode())
                .variantType(ProductVariantType.FRAME)
                .productActive(savedProduct.getIsActive())
                .variantActive(savedVariant.getIsActive())
                .supplierId(supplierIds.get(0))
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantity(request.getQuantity())
                .build();
    }

    @Transactional(readOnly = true)
    public FrameDetailResponse getById(Long productId) {
        return mapFrameDetail(findFrameByProductId(productId));
    }

    @Transactional
    public FrameDetailResponse update(Long productId, FrameCreateRequest request) {
        FrameVariantDetails details = findFrameByProductId(productId);
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        String name = normalizeRequired(request.getName(), "name is required");
        String code = normalizeRequired(request.getCode(), "code is required");
        String type = normalizeAndValidateFrameType(request.getType());
        String color = normalizeRequired(request.getColor(), "color is required");
        String size = normalizeRequired(request.getSize(), "size is required");
        List<Long> supplierIds = resolveSupplierIds(request);

        product.setName(name);
        variant.setNotes(normalize(request.getExtra()));
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setQuantity(request.getQuantity());

        details.setFrameCode(code);
        details.setFrameType(type);
        details.setColor(color);
        details.setSize(size);

        productSupportService.replaceSupplierLinks(product, supplierIds);

        return mapFrameDetail(details);
    }

    @Transactional
    public void delete(Long productId) {
        findFrameByProductId(productId);
        productSupportService.deleteProduct(productId);
    }

    @Transactional(readOnly = true)
    public ProductPageResponse search(String q, int page, int size) {
        Page<FrameVariantDetails> result = frameVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapFrameListItem)
                .toList();

        return ProductPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private FrameVariantDetails findFrameByProductId(Long productId) {
        return frameVariantDetailsRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Frame not found"));
    }

    private List<Long> resolveSupplierIds(FrameCreateRequest request) {
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierIds or supplierId is required");
    }

    private ProductListResponse mapFrameListItem(FrameVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

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
                .variantType(ProductVariantType.FRAME)
                .supplierId(firstSupplierId(product.getId()))
                .suppliers(productSupportService.resolveSupplierInfosForProduct(product.getId()))
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .quantity(variant.getQuantity())
                .frameCode(details.getFrameCode())
                .frameType(details.getFrameType())
                .color(details.getColor())
                .size(details.getSize())
                .build();
    }

    private FrameDetailResponse mapFrameDetail(FrameVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return FrameDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .name(product.getName())
                .code(details.getFrameCode())
                .type(details.getFrameType())
                .color(details.getColor())
                .size(details.getSize())
                .quantity(variant.getQuantity())
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .extra(variant.getNotes())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .build();
    }

    private String generateFrameSku(Long productId) {
        String baseSku = FRAME_SKU_PREFIX + productId;
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

    private String normalizeAndValidateFrameType(String value) {
        String normalized = normalizeRequired(value, "type is required");
        String canonical = ALLOWED_FRAME_TYPES.get(normalized.toLowerCase());
        if (canonical == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid frame type. Allowed: " + String.join(", ", ALLOWED_FRAME_TYPES.values())
            );
        }
        return canonical;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private Long firstSupplierId(Long productId) {
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(productId);
        return supplierIds.isEmpty() ? null : supplierIds.get(0);
    }

    private static Map<String, String> createAllowedFrameTypes() {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("3pieces/rimless", "3Pieces/Rimless");
        allowed.put("half rimless/supra", "Half Rimless/SUPRA");
        allowed.put("full metal", "Full Metal");
        allowed.put("full shell/plastic", "Full Shell/Plastic");
        allowed.put("goggles", "Goggles");
        return Map.copyOf(allowed);
    }
}
