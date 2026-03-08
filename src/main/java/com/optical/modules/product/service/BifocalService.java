package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.BifocalCreateRequest;
import com.optical.modules.product.dto.BifocalCreateResponse;
import com.optical.modules.product.dto.BifocalDetailResponse;
import com.optical.modules.product.dto.BifocalUpdateRequest;
import com.optical.modules.product.dto.BifocalVariantResponse;
import com.optical.modules.product.dto.LensAdditionMethod;
import com.optical.modules.product.dto.LensSubType;
import com.optical.modules.product.entity.LensVariantDetails;
import com.optical.modules.product.entity.Product;
import com.optical.modules.product.entity.ProductType;
import com.optical.modules.product.entity.ProductVariant;
import com.optical.modules.product.entity.Uom;
import com.optical.modules.product.repository.LensVariantDetailsRepository;
import com.optical.modules.product.repository.ProductRepository;
import com.optical.modules.product.repository.ProductTypeRepository;
import com.optical.modules.product.repository.ProductVariantRepository;
import com.optical.modules.product.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class BifocalService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = "LENS";
    private static final String DEFAULT_UOM_CODE = "PA";
    private static final BigDecimal SPH_MIN = BigDecimal.valueOf(-24);
    private static final BigDecimal SPH_MAX = BigDecimal.valueOf(24);
    private static final BigDecimal CYL_MIN = BigDecimal.valueOf(-12);
    private static final BigDecimal CYL_MAX = BigDecimal.valueOf(12);
    private static final BigDecimal ADD_MIN = BigDecimal.valueOf(-4);
    private static final BigDecimal ADD_MAX = BigDecimal.valueOf(4);
    private static final BigDecimal STEP = BigDecimal.valueOf(0.25);
    private static final int MAX_VARIANTS_PER_REQUEST = 5000;
    private static final Map<String, String> ALLOWED_MATERIALS = createAllowedMaterials();

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final LensVariantDetailsRepository lensVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional
    public BifocalCreateResponse create(BifocalCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String material = normalizeAndValidateMaterial(request.getMaterial());
        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");

        if (request.getIndex() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "index is required");
        }
        if (request.getSphAdditionMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sphAdditionMethod is required");
        }
        if (Boolean.TRUE.equals(request.getCylEnabled()) && request.getCylAdditionMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cylAdditionMethod is required when cylEnabled=true");
        }
        if (request.getAddAdditionMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addAdditionMethod is required");
        }

        validateCommercialFields(request);
        List<Long> supplierIds = resolveSupplierIds(request.getSupplierIds(), request.getSupplierId());
        List<PowerTriple> powerTriples = buildPowerTriples(request);

        List<BifocalVariantResponse> variants = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (PowerTriple powerTriple : powerTriples) {
            Product savedProduct = createProduct(productType, companyName, buildBifocalName(productName, powerTriple), supplierIds);
            productIds.add(savedProduct.getId());

            ProductVariant variant = new ProductVariant();
            variant.setProduct(savedProduct);
            variant.setSku(generateSku(savedProduct.getId()));
            variant.setBarcode(null);
            variant.setUom(uom);
            variant.setNotes(normalize(request.getExtra()));
            variant.setPurchasePrice(request.getPurchasePrice());
            variant.setSellingPrice(request.getSellingPrice());
            variant.setQuantity(request.getQuantity());
            variant.setIsActive(true);
            ProductVariant savedVariant = productVariantRepository.save(variant);

            LensVariantDetails details = new LensVariantDetails();
            details.setVariant(savedVariant);
            details.setLensSubType(LensSubType.BIFOCAL.name());
            details.setMaterial(material);
            details.setLensIndex(request.getIndex());
            details.setSph(powerTriple.sph());
            details.setCyl(powerTriple.cyl());
            details.setAddPower(powerTriple.addPower());
            lensVariantDetailsRepository.save(details);

            variants.add(BifocalVariantResponse.builder()
                    .productId(savedProduct.getId())
                    .variantId(savedVariant.getId())
                    .sku(savedVariant.getSku())
                    .sph(powerTriple.sph())
                    .cyl(powerTriple.cyl())
                    .addPower(powerTriple.addPower())
                    .build());
        }

        return BifocalCreateResponse.builder()
                .productId(productIds.isEmpty() ? null : productIds.get(0))
                .productIds(List.copyOf(productIds))
                .productTypeCode(productType.getCode())
                .companyName(companyName)
                .productName(productName)
                .lensSubType(LensSubType.BIFOCAL)
                .material(material)
                .index(request.getIndex())
                .uomCode(uom.getCode())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantity(request.getQuantity())
                .extra(normalize(request.getExtra()))
                .totalProductsCreated(productIds.size())
                .totalVariantsCreated(variants.size())
                .variants(variants)
                .build();
    }

    @Transactional(readOnly = true)
    public BifocalDetailResponse getById(Long productId) {
        return mapBifocalDetail(findBifocalByProductId(productId));
    }

    @Transactional
    public BifocalDetailResponse update(Long productId, BifocalUpdateRequest request) {
        LensVariantDetails details = findBifocalByProductId(productId);
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        String material = normalizeAndValidateMaterial(request.getMaterial());
        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        List<Long> supplierIds = resolveSupplierIds(request.getSupplierIds(), request.getSupplierId());

        if (request.getIndex() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "index is required");
        }

        BigDecimal sph = requiredDecimal(request.getSph(), "sph is required");
        validateQuarterStep(sph, "sph");
        validateRange(sph, SPH_MIN, SPH_MAX, "sph");

        BigDecimal cyl = request.getCyl();
        if (cyl != null) {
            validateQuarterStep(cyl, "cyl");
            validateRange(cyl, CYL_MIN, CYL_MAX, "cyl");
        }

        BigDecimal addPower = requiredDecimal(request.getAddPower(), "addPower is required");
        validateQuarterStep(addPower, "addPower");
        validateRange(addPower, ADD_MIN, ADD_MAX, "addPower");

        if (request.getSellingPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice is required");
        }
        if (request.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice cannot be negative");
        }

        product.setBrandName(companyName);
        product.setName(productName);

        variant.setNotes(normalize(request.getExtra()));
        variant.setSellingPrice(request.getSellingPrice());

        details.setMaterial(material);
        details.setLensIndex(request.getIndex());
        details.setSph(sph);
        details.setCyl(cyl);
        details.setAddPower(addPower);

        productSupportService.replaceSupplierLinks(product, supplierIds);

        return mapBifocalDetail(details);
    }

    private List<PowerTriple> buildPowerTriples(BifocalCreateRequest request) {
        List<BigDecimal> sphValues = resolveSphValues(request);
        List<BigDecimal> cylValues = resolveCylValues(request);
        List<BigDecimal> addValues = resolveAddValues(request);

        Set<PowerTriple> triples = new LinkedHashSet<>();
        for (BigDecimal sph : sphValues) {
            for (BigDecimal cyl : cylValues) {
                for (BigDecimal addPower : addValues) {
                    triples.add(new PowerTriple(sph, cyl, addPower));
                    if (triples.size() > MAX_VARIANTS_PER_REQUEST) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Too many variants generated. Reduce the selected range."
                        );
                    }
                }
            }
        }
        return List.copyOf(triples);
    }

    private List<BigDecimal> resolveSphValues(BifocalCreateRequest request) {
        if (request.getSphAdditionMethod() == LensAdditionMethod.SINGLE) {
            BigDecimal sph = requiredDecimal(request.getSph(), "sph is required for SINGLE mode");
            validateQuarterStep(sph, "sph");
            validateRange(sph, SPH_MIN, SPH_MAX, "sph");
            return List.of(sph);
        }

        BigDecimal sphStart = requiredDecimal(request.getSphStart(), "sphStart is required for RANGE mode");
        BigDecimal sphEnd = requiredDecimal(request.getSphEnd(), "sphEnd is required for RANGE mode");
        validateQuarterStep(sphStart, "sphStart");
        validateQuarterStep(sphEnd, "sphEnd");
        validateRange(sphStart, SPH_MIN, SPH_MAX, "sphStart");
        validateRange(sphEnd, SPH_MIN, SPH_MAX, "sphEnd");
        if (sphStart.compareTo(sphEnd) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sphStart must be less than or equal to sphEnd");
        }
        return expandRange(sphStart, sphEnd);
    }

    private List<BigDecimal> resolveCylValues(BifocalCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getCylEnabled())) {
            return List.of((BigDecimal) null);
        }

        if (request.getCylAdditionMethod() == LensAdditionMethod.SINGLE) {
            BigDecimal cyl = requiredDecimal(request.getCyl(), "cyl is required when cylEnabled=true");
            validateQuarterStep(cyl, "cyl");
            validateRange(cyl, CYL_MIN, CYL_MAX, "cyl");
            return List.of(cyl);
        }

        BigDecimal cylStart = requiredDecimal(request.getCylStart(), "cylStart is required when cylEnabled=true");
        BigDecimal cylEnd = requiredDecimal(request.getCylEnd(), "cylEnd is required when cylEnabled=true");
        validateQuarterStep(cylStart, "cylStart");
        validateQuarterStep(cylEnd, "cylEnd");
        validateRange(cylStart, CYL_MIN, CYL_MAX, "cylStart");
        validateRange(cylEnd, CYL_MIN, CYL_MAX, "cylEnd");
        if (cylStart.compareTo(cylEnd) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cylStart must be less than or equal to cylEnd");
        }
        return expandRange(cylStart, cylEnd);
    }

    private List<BigDecimal> resolveAddValues(BifocalCreateRequest request) {
        if (request.getAddAdditionMethod() == LensAdditionMethod.SINGLE) {
            BigDecimal addPower = requiredDecimal(request.getAddPower(), "addPower is required for SINGLE mode");
            validateQuarterStep(addPower, "addPower");
            validateRange(addPower, ADD_MIN, ADD_MAX, "addPower");
            return List.of(addPower);
        }

        BigDecimal addPowerStart = requiredDecimal(request.getAddPowerStart(), "addPowerStart is required for RANGE mode");
        BigDecimal addPowerEnd = requiredDecimal(request.getAddPowerEnd(), "addPowerEnd is required for RANGE mode");
        validateQuarterStep(addPowerStart, "addPowerStart");
        validateQuarterStep(addPowerEnd, "addPowerEnd");
        validateRange(addPowerStart, ADD_MIN, ADD_MAX, "addPowerStart");
        validateRange(addPowerEnd, ADD_MIN, ADD_MAX, "addPowerEnd");
        if (addPowerStart.compareTo(addPowerEnd) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addPowerStart must be less than or equal to addPowerEnd");
        }
        return expandRange(addPowerStart, addPowerEnd);
    }

    private List<BigDecimal> expandRange(BigDecimal start, BigDecimal end) {
        List<BigDecimal> values = new ArrayList<>();
        for (BigDecimal current = start; current.compareTo(end) <= 0; current = current.add(STEP)) {
            values.add(current);
        }
        return values;
    }

    private void validateCommercialFields(BifocalCreateRequest request) {
        if (request.getPurchasePrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice is required");
        }
        if (request.getSellingPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice is required");
        }
        if (request.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity is required");
        }
        if (request.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice cannot be negative");
        }
        if (request.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice cannot be negative");
        }
        if (request.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity cannot be negative");
        }
    }

    private String normalizeAndValidateMaterial(String value) {
        String normalized = normalizeRequired(value, "material is required");
        String canonical = ALLOWED_MATERIALS.get(normalized.toLowerCase());
        if (canonical == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid material for BIFOCAL. Allowed: " + String.join(", ", ALLOWED_MATERIALS.values())
            );
        }
        return canonical;
    }

    private LensVariantDetails findBifocalByProductId(Long productId) {
        LensVariantDetails details = lensVariantDetailsRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Bifocal lens not found"));

        if (!LensSubType.BIFOCAL.name().equals(details.getLensSubType())) {
            throw new ResourceNotFoundException("Bifocal lens not found");
        }

        return details;
    }

    private BigDecimal requiredDecimal(BigDecimal value, String message) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private void validateQuarterStep(BigDecimal value, String fieldName) {
        if (value.remainder(STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be in 0.25 steps");
        }
    }

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " out of range");
        }
    }

    private Product createProduct(ProductType productType, String companyName, String productName, List<Long> supplierIds) {
        Product product = new Product();
        product.setProductType(productType);
        product.setBrandName(companyName);
        product.setName(productName);
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);
        productSupportService.linkSuppliersToProduct(savedProduct, supplierIds);
        return savedProduct;
    }

    private String generateSku(Long productId) {
        return "BF-" + productId + "-001";
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private List<Long> resolveSupplierIds(List<Long> supplierIds, Long supplierId) {
        if (supplierIds != null && !supplierIds.isEmpty()) {
            return productSupportService.resolveAndValidateSupplierIds(
                    supplierIds,
                    "At least one valid supplier id is required"
            );
        }
        if (supplierId != null) {
            return productSupportService.resolveAndValidateSupplierIds(
                    List.of(supplierId),
                    "supplierIds or supplierId is required"
            );
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierIds or supplierId is required");
    }

    private String buildBifocalName(String baseName, PowerTriple powerTriple) {
        String sphValue = formatSignedPower(powerTriple.sph);
        String cylValue = powerTriple.cyl != null ? "/" + formatSignedPower(powerTriple.cyl) : "";
        String addValue = formatSignedPower(powerTriple.addPower);
        return baseName + "(" + sphValue + cylValue + ") " + addValue;
    }

    private String formatSignedPower(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, java.math.RoundingMode.UNNECESSARY);
        String sign = scaled.signum() >= 0 ? "+" : "";
        return sign + scaled.toPlainString();
    }

    private BifocalDetailResponse mapBifocalDetail(LensVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return BifocalDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .name(product.getName())
                .lensSubType(LensSubType.BIFOCAL)
                .material(details.getMaterial())
                .index(details.getLensIndex())
                .sph(details.getSph())
                .cyl(details.getCyl())
                .addPower(details.getAddPower())
                .quantity(variant.getQuantity())
                .purchasePrice(variant.getPurchasePrice())
                .sellingPrice(variant.getSellingPrice())
                .extra(variant.getNotes())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .build();
    }

    private static Map<String, String> createAllowedMaterials() {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("glass", "Glass");
        allowed.put("plastic lense", "Plastic Lense");
        allowed.put("plastic lens", "Plastic Lense");
        allowed.put("polycarbonate lense", "Polycarbonate Lense");
        allowed.put("polycarbonate lens", "Polycarbonate Lense");
        return Map.copyOf(allowed);
    }

    private record PowerTriple(BigDecimal sph, BigDecimal cyl, BigDecimal addPower) {
    }
}
