package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.dto.SunglassesCreateRequest;
import com.optical.modules.product.dto.SunglassesDetailResponse;
import com.optical.modules.product.dto.SunglassesListResponse;
import com.optical.modules.product.dto.SunglassesPageResponse;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductType;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.SunglassesVariantDetails;
import com.optical.modules.product.entity.Uom;
import com.optical.modules.product.repository.ProductRepository;
import com.optical.modules.product.repository.ProductTypeRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.SunglassesVariantDetailsRepository;
import com.optical.modules.product.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class SunglassService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = ProductVariantType.SUNGLASSES.name();
    private static final String DEFAULT_UOM_CODE = "EA";
    private static final String SUNGLASSES_SKU_PREFIX = "SUN-";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final SunglassesVariantDetailsRepository sunglassesVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional(readOnly = true)
    public SunglassesPageResponse search(String q, int page, int size) {
        Page<SunglassesVariantDetails> result = sunglassesVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<SunglassesListResponse> items = result.getContent().stream()
                .map(this::mapSunglassesListItem)
                .toList();

        return SunglassesPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public ProductCreateResponse create(SunglassesCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        String description = normalizeRequired(request.getDescription(), "description is required");
        List<Long> supplierIds = resolveSupplierIds(request);

        Product product = new Product();
        product.setProductType(productType);
        product.setBrandName(companyName);
        product.setName(productName);
        product.setDescription(description);
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);
        productSupportService.linkSuppliersToProduct(savedProduct, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSku(generateSunglassesSku(savedProduct.getId()));
        variant.setBarcode(null);
        variant.setUom(uom);
        variant.setNotes(normalize(request.getNotes()));
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setQuantity(request.getQuantity());
        variant.setIsActive(true);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        SunglassesVariantDetails details = new SunglassesVariantDetails();
        details.setVariant(savedVariant);
        details.setDescription(description);
        sunglassesVariantDetailsRepository.save(details);

        return ProductCreateResponse.builder()
                .productId(savedProduct.getId())
                .variantId(savedVariant.getId())
                .productTypeCode(productType.getCode())
                .productName(savedProduct.getName())
                .sku(savedVariant.getSku())
                .barcode(savedVariant.getBarcode())
                .variantType(ProductVariantType.SUNGLASSES)
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
    public SunglassesDetailResponse getById(Long productId) {
        return mapSunglassesDetail(findSunglassesByProductId(productId));
    }

    @Transactional
    public SunglassesDetailResponse update(Long productId, SunglassesCreateRequest request) {
        SunglassesVariantDetails details = findSunglassesByProductId(productId);
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        String description = normalizeRequired(request.getDescription(), "description is required");
        List<Long> supplierIds = resolveSupplierIds(request);

        product.setBrandName(companyName);
        product.setName(productName);
        product.setDescription(description);
        variant.setNotes(normalize(request.getNotes()));
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setQuantity(request.getQuantity());

        details.setDescription(description);
        productSupportService.replaceSupplierLinks(product, supplierIds);

        return mapSunglassesDetail(details);
    }

    private SunglassesVariantDetails findSunglassesByProductId(Long productId) {
        return sunglassesVariantDetailsRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sunglasses not found"));
    }

    private List<Long> resolveSupplierIds(SunglassesCreateRequest request) {
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

    private SunglassesListResponse mapSunglassesListItem(SunglassesVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        return SunglassesListResponse.builder()
                .id(product.getId())
                .modelName(product.getName())
                .company(product.getBrandName())
                .purchasePrice(variant.getPurchasePrice())
                .quantity(variant.getQuantity())
                .salesPrice(variant.getSellingPrice())
                .suppliers(productSupportService.resolveSupplierInfosForProduct(product.getId()))
                .build();
    }

    private SunglassesDetailResponse mapSunglassesDetail(SunglassesVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return SunglassesDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .name(product.getName())
                .description(details.getDescription())
                .quantity(variant.getQuantity())
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .notes(variant.getNotes())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
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

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }
}
