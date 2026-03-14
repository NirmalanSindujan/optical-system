package com.optical.modules.product.service;

import com.optical.common.base.PageResponse;
import com.optical.common.exception.DuplicateResourceException;
import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.Lense.LensSubType;
import com.optical.modules.product.dto.Lense.LensSubtabResponse;
import com.optical.modules.product.dto.Lense.LensDetailResponse;
import com.optical.modules.product.dto.Product.BillingProductListResponse;
import com.optical.modules.product.dto.ProductCreateRequest;
import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.ProductListResponse;
import com.optical.modules.product.dto.ProductPageResponse;
import com.optical.modules.product.dto.ProductVariantType;
import com.optical.modules.product.entity.AccessoryVariantDetails;
import com.optical.modules.product.entity.LensVariantDetails;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductType;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.Uom;
import com.optical.modules.product.repository.AccessoryVariantDetailsRepository;
import com.optical.modules.product.repository.FrameVariantDetailsRepository;
import com.optical.modules.product.repository.LensVariantDetailsRepository;
import com.optical.modules.product.repository.ProductRepository;
import com.optical.modules.product.repository.ProductTypeRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.SunglassesVariantDetailsRepository;
import com.optical.modules.product.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final LensVariantDetailsRepository lensVariantDetailsRepository;
    private final AccessoryVariantDetailsRepository accessoryVariantDetailsRepository;
    private final FrameVariantDetailsRepository frameVariantDetailsRepository;
    private final SunglassesVariantDetailsRepository sunglassesVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional
    public ProductCreateResponse create(ProductCreateRequest request) {
        validateSupportedVariant(request.getVariantType());

        String sku = normalizeRequired(request.getSku(), "sku is required");
        String barcode = normalize(request.getBarcode());

        if (productVariantRepository.existsBySkuAndDeletedAtIsNull(sku)) {
            throw new DuplicateResourceException("Product SKU already exists");
        }
        if (barcode != null && productVariantRepository.existsByBarcodeAndDeletedAtIsNull(barcode)) {
            throw new DuplicateResourceException("Product barcode already exists");
        }

        ProductType productType = productTypeRepository.findById(normalizeRequired(request.getProductTypeCode(), "productTypeCode is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Product type not found"));
        Uom uom = uomRepository.findById(normalizeRequired(request.getUomCode(), "uomCode is required"))
                .orElseThrow(() -> new ResourceNotFoundException("UOM not found"));

        validateDetailPayload(request);
        validateCommercialFields(request);
        List<Long> supplierIds = productSupportService.resolveAndValidateSupplierIds(
                request.getSupplierId() == null ? null : List.of(request.getSupplierId()),
                "supplierId is required"
        );

        Product product = new Product();
        product.setProductType(productType);
        product.setBrandName(normalize(request.getBrandName()));
        product.setName(normalizeRequired(request.getName(), "name is required"));
        product.setDescription(normalize(request.getDescription()));
        product.setIsActive(request.getIsActive() == null ? true : request.getIsActive());
        Product savedProduct = productRepository.save(product);
        productSupportService.linkSuppliersToProduct(savedProduct, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(savedProduct);
        variant.setSku(sku);
        variant.setBarcode(barcode);
        variant.setUom(uom);
        variant.setNotes(normalize(request.getNotes()));
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setQuantity(request.getQuantity());
        variant.setIsActive(request.getVariantActive() == null ? true : request.getVariantActive());
        ProductVariant savedVariant = productVariantRepository.save(variant);

        saveDetails(savedVariant, request);

        return ProductCreateResponse.builder()
                .productId(savedProduct.getId())
                .variantId(savedVariant.getId())
                .productTypeCode(productType.getCode())
                .productName(savedProduct.getName())
                .sku(savedVariant.getSku())
                .barcode(savedVariant.getBarcode())
                .variantType(request.getVariantType())
                .lensSubType(request.getLensDetails() == null ? null : request.getLensDetails().getLensSubType())
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

    @Transactional
    public void deleteProduct(Long productId) {
        productSupportService.deleteProduct(productId);
    }

    @Transactional(readOnly = true)
    public ProductPageResponse searchLenses(String q, int page, int size) {
        Page<LensVariantDetails> result = lensVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapLensItem)
                .toList();
        return buildPageResponse(result, items);
    }


    @Transactional(readOnly = true)
    public ProductPageResponse searchLensesBySupplierID(String q, int page, int size, Long supplierId) {
        Page<LensVariantDetails> result = lensVariantDetailsRepository.searchBySupplier(
                supplierId,
                normalize(q),
                PageRequest.of(page, size)
        );
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapLensItem)
                .toList();
        return buildPageResponse(result, items);
    }


    @Transactional(readOnly = true)
    public PageResponse<BillingProductListResponse> getBillingProductList(String q, int page, int size, Long supplierId, ProductVariantType type) {
        String keyword = normalize(q);
        List<ProductVariant> filtered = productVariantRepository.findAll().stream()
                .filter(variant -> variant.getProduct() != null)
                .filter(variant -> matchesSupplier(variant, supplierId))
                .filter(variant -> matchesKeyword(variant, keyword))
                .filter(variant -> matchesVariantType(variant, type))
                .sorted(Comparator
                        .comparing((ProductVariant variant) -> variant.getProduct().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProductVariant::getId))
                .toList();

        Page<ProductVariant> result = toPage(filtered, PageRequest.of(page, size));

        List<BillingProductListResponse> items = result.getContent().stream()
                .map(this::mapBillingProductItem)
                .toList();

        return PageResponse.<BillingProductListResponse>builder()
                .content(items)
                .totalElements(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductPageResponse searchLensSubtab(String lensSubType, String q, int page, int size) {
        LensSubType normalizedLensSubType = parseLensSubType(lensSubType);

        Page<LensVariantDetails> result = lensVariantDetailsRepository.searchByLensSubType(
                normalizedLensSubType.name(),
                normalize(q),
                PageRequest.of(page, size)
        );
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapLensItem)
                .toList();
        return buildPageResponse(result, items);
    }

    @Transactional(readOnly = true)
    public LensDetailResponse getLensByVariantId(Long variantId) {
        LensVariantDetails details = lensVariantDetailsRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lens not found"));
        return mapLensDetail(details);
    }

    @Transactional(readOnly = true)
    public List<LensSubtabResponse> getLensSubtabs() {
        return lensVariantDetailsRepository.findLensSubtabs().stream()
                .map(item -> LensSubtabResponse.builder()
                        .lensSubType(parseLensSubType(item.getLensSubType()))
                        .totalCounts(item.getTotalCounts())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductPageResponse searchAccessories(String q, int page, int size) {
        Page<AccessoryVariantDetails> result = accessoryVariantDetailsRepository.search(normalize(q), PageRequest.of(page, size));
        List<ProductListResponse> items = result.getContent().stream()
                .map(this::mapAccessoryItem)
                .toList();
        return buildPageResponse(result, items);
    }

    private void validateSupportedVariant(ProductVariantType variantType) {
        if (variantType == ProductVariantType.FRAME || variantType == ProductVariantType.SUNGLASSES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use the dedicated frame or sunglasses endpoints for this product type"
            );
        }
    }

    private void saveDetails(ProductVariant savedVariant, ProductCreateRequest request) {
        if (request.getVariantType() == ProductVariantType.LENS) {
            LensVariantDetails details = new LensVariantDetails();
            details.setVariant(savedVariant);
            details.setLensSubType(request.getLensDetails().getLensSubType().name());
            details.setMaterial(normalize(request.getLensDetails().getMaterial()));
            details.setLensIndex(request.getLensDetails().getLensIndex());
            details.setLensType(normalize(request.getLensDetails().getLensType()));
            details.setCoatingCode(normalize(request.getLensDetails().getCoatingCode()));
            details.setSph(request.getLensDetails().getSph());
            details.setCyl(request.getLensDetails().getCyl());
            details.setAddPower(request.getLensDetails().getAddPower());
            details.setColor(normalize(request.getLensDetails().getColor()));
            details.setBaseCurve(normalize(request.getLensDetails().getBaseCurve()));
            lensVariantDetailsRepository.save(details);
            return;
        }

        AccessoryVariantDetails details = new AccessoryVariantDetails();
        details.setVariant(savedVariant);
        details.setItemType(normalize(request.getAccessoryDetails().getItemType()));
        accessoryVariantDetailsRepository.save(details);
    }

    private void validateDetailPayload(ProductCreateRequest request) {
        ProductVariantType variantType = request.getVariantType();
        int detailCount = 0;
        detailCount += request.getLensDetails() == null ? 0 : 1;
        detailCount += request.getAccessoryDetails() == null ? 0 : 1;

        if (detailCount != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one supported detail payload is required");
        }
        if (variantType == ProductVariantType.LENS && request.getLensDetails() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensDetails is required for LENS variant");
        }
        if (variantType == ProductVariantType.LENS) {
            validateLensDetails(request);
        }
        if (variantType == ProductVariantType.ACCESSORY && request.getAccessoryDetails() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accessoryDetails is required for ACCESSORY variant");
        }
        if (variantType == ProductVariantType.ACCESSORY
                && normalize(request.getAccessoryDetails().getItemType()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemType is required for ACCESSORY variant");
        }
    }

    private void validateLensDetails(ProductCreateRequest request) {
        var lens = request.getLensDetails();
        if (lens.getLensSubType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensSubType is required");
        }

        if (lens.getSph() != null) {
            validateQuarterStep(lens.getSph(), "sph");
            validateRange(lens.getSph(), BigDecimal.valueOf(-24), BigDecimal.valueOf(24), "sph");
        }
        if (lens.getCyl() != null) {
            validateQuarterStep(lens.getCyl(), "cyl");
            validateRange(lens.getCyl(), BigDecimal.valueOf(-12), BigDecimal.valueOf(12), "cyl");
        }
        if (lens.getAddPower() != null) {
            validateQuarterStep(lens.getAddPower(), "addPower");
            validateRange(lens.getAddPower(), BigDecimal.valueOf(-4), BigDecimal.valueOf(4), "addPower");
        }

        if (lens.getLensSubType() == LensSubType.CONTACT_LENS) {
            if (normalize(lens.getColor()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "color is required for CONTACT_LENS");
            }
            if (normalize(lens.getBaseCurve()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseCurve is required for CONTACT_LENS");
            }
            return;
        }

        if (normalize(lens.getMaterial()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "material is required for lens subtype");
        }
        if (lens.getLensIndex() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensIndex is required for lens subtype");
        }
        if (lens.getSph() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sph is required for lens subtype");
        }

        if (lens.getLensSubType() == LensSubType.SINGLE_VISION) {
            String lensType = normalize(lens.getLensType());
            if (lensType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensType is required for SINGLE_VISION");
            }
            Set<String> allowed = Set.of("UC", "HMC", "PGHMC", "PBHMC", "BB", "PGBB");
            if (!allowed.contains(lensType.toUpperCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lensType for SINGLE_VISION");
            }
            return;
        }

        if ((lens.getLensSubType() == LensSubType.BIFOCAL || lens.getLensSubType() == LensSubType.PROGRESSIVE)
                && lens.getAddPower() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addPower is required for BIFOCAL/PROGRESSIVE");
        }
    }

    private ProductPageResponse buildPageResponse(Page<?> result, List<ProductListResponse> items) {
        return ProductPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    private ProductListResponse mapLensItem(LensVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

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
                .variantType(ProductVariantType.LENS)
                .supplierId(supplierIds.isEmpty() ? null : supplierIds.get(0))
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .quantity(variant.getQuantity())
                .lensSubType(parseLensSubType(details.getLensSubType()))
                .material(details.getMaterial())
                .lensIndex(details.getLensIndex())
                .lensType(details.getLensType())
                .coatingCode(details.getCoatingCode())
                .sph(details.getSph())
                .cyl(details.getCyl())
                .addPower(details.getAddPower())
                .lensColor(details.getColor())
                .color(details.getColor())
                .baseCurve(details.getBaseCurve())
                .build();
    }

    private LensDetailResponse mapLensDetail(LensVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return LensDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .name(product.getName())
                .lensSubType(parseLensSubType(details.getLensSubType()))
                .material(details.getMaterial())
                .index(details.getLensIndex())
                .type(details.getLensType())
                .coatingCode(details.getCoatingCode())
                .sph(details.getSph())
                .cyl(details.getCyl())
                .addPower(details.getAddPower())
                .color(details.getColor())
                .baseCurve(details.getBaseCurve())
                .quantity(variant.getQuantity())
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .extra(variant.getNotes())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .build();
    }

    private ProductListResponse mapAccessoryItem(AccessoryVariantDetails details) {
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
                .variantType(ProductVariantType.ACCESSORY)
                .supplierId(firstSupplierId(product.getId()))
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .quantity(variant.getQuantity())
                .itemType(details.getItemType())
                .build();
    }

    private BillingProductListResponse enrichBillingProductItem(BillingProductListResponse item) {
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(item.getProductId());

        return item.toBuilder()
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .build();
    }

    private BillingProductListResponse mapBillingProductItem(ProductVariant variant) {
        Product product = variant.getProduct();

        return BillingProductListResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .productTypeCode(product.getProductType().getCode())
                .name(product.getName())
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .uomCode(variant.getUom().getCode())
                .variantType(resolveVariantType(variant))
                .sellingPrice(variant.getSellingPrice())
                .quantity(variant.getQuantity())
                .build();
    }

    private ProductVariantType resolveVariantType(ProductVariant variant) {
        if (lensVariantDetailsRepository.findByVariantId(variant.getId()).isPresent()) {
            return ProductVariantType.LENS;
        }
        if (accessoryVariantDetailsRepository.findByProductId(variant.getProduct().getId()).isPresent()) {
            return ProductVariantType.ACCESSORY;
        }
        if (frameVariantDetailsRepository.findByProductId(variant.getProduct().getId()).isPresent()) {
            return ProductVariantType.FRAME;
        }
        if (sunglassesVariantDetailsRepository.findByProductId(variant.getProduct().getId()).isPresent()) {
            return ProductVariantType.SUNGLASSES;
        }
        return null;
    }

    private boolean matchesSupplier(ProductVariant variant, Long supplierId) {
        if (supplierId == null) {
            return true;
        }
        return productSupportService.resolveSupplierIdsForProduct(variant.getProduct().getId()).contains(supplierId);
    }

    private boolean matchesKeyword(ProductVariant variant, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        Product product = variant.getProduct();
        String normalizedKeyword = keyword.toLowerCase();

        return containsIgnoreCase(product.getName(), normalizedKeyword)
                || containsIgnoreCase(product.getBrandName(), normalizedKeyword)
                || containsIgnoreCase(variant.getSku(), normalizedKeyword)
                || containsIgnoreCase(variant.getBarcode(), normalizedKeyword);
    }

    private boolean matchesVariantType(ProductVariant variant, ProductVariantType type) {
        if (type == null) {
            return true;
        }
        return type == resolveVariantType(variant);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Page<ProductVariant> toPage(List<ProductVariant> items, PageRequest pageable) {
        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    private void validateCommercialFields(ProductCreateRequest request) {
        if (request.getSupplierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId is required");
        }
        if (request.getPurchasePrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice is required");
        }
        if (request.getSellingPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice is required");
        }
        if (request.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0 || request.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prices cannot be negative");
        }
        if (request.getVariantType() != ProductVariantType.LENS) {
            if (request.getQuantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity is required for non-lens products");
            }
            if (request.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity cannot be negative");
            }
        }
    }

    private void validateQuarterStep(BigDecimal value, String fieldName) {
        if (value.remainder(BigDecimal.valueOf(0.25)).compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be in 0.25 steps");
        }
    }

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " out of range");
        }
    }

    private LensSubType parseLensSubType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lensSubType is required");
        }
        try {
            return LensSubType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lensSubType");
        }
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
}
