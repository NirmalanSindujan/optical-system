package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.LensAdditionMethod;
import com.optical.modules.product.dto.LensSubType;
import com.optical.modules.product.dto.SingleVisionCreateRequest;
import com.optical.modules.product.dto.SingleVisionCreateResponse;
import com.optical.modules.product.dto.SingleVisionVariantResponse;
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
public class SingleVisionService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = "LENS";
    private static final String DEFAULT_UOM_CODE = "PA";
    private static final BigDecimal SPH_MIN = BigDecimal.valueOf(-24);
    private static final BigDecimal SPH_MAX = BigDecimal.valueOf(24);
    private static final BigDecimal CYL_MIN = BigDecimal.valueOf(-12);
    private static final BigDecimal CYL_MAX = BigDecimal.valueOf(12);
    private static final BigDecimal STEP = BigDecimal.valueOf(0.25);
    private static final int MAX_VARIANTS_PER_REQUEST = 5000;
    private static final Map<String, String> ALLOWED_MATERIALS = createAllowedMaterials();
    private static final Set<String> ALLOWED_TYPES = Set.of("UC", "HMC", "PGHMC", "PBHMC", "BB", "PGBB");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final LensVariantDetailsRepository lensVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional
    public SingleVisionCreateResponse create(SingleVisionCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String material = normalizeAndValidateMaterial(request.getMaterial());
        String type = normalizeAndValidateLensType(request.getType());
        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");


        if (request.getIndex() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "index is required");
        }
        if (request.getAdditionMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "additionMethod is required");
        }

        validateCommercialFields(request);
        List<Long> supplierIds = resolveSupplierIds(request);
        List<PowerPair> powerPairs = buildPowerPairs(request);

        List<SingleVisionVariantResponse> variants = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (PowerPair powerPair : powerPairs) {
            Product savedProduct = createProduct(productType, companyName, buildSingleLenseName(productName,powerPair), supplierIds);
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
            details.setLensSubType(LensSubType.SINGLE_VISION.name());
            details.setMaterial(material);
            details.setLensIndex(request.getIndex());
            details.setLensType(type);
            details.setSph(powerPair.sph());
            details.setCyl(powerPair.cyl());
            lensVariantDetailsRepository.save(details);

            variants.add(SingleVisionVariantResponse.builder()
                    .productId(savedProduct.getId())
                    .variantId(savedVariant.getId())
                    .sku(savedVariant.getSku())
                    .sph(powerPair.sph())
                    .cyl(powerPair.cyl())
                    .build());
        }

        return SingleVisionCreateResponse.builder()
                .productId(productIds.isEmpty() ? null : productIds.get(0))
                .productIds(List.copyOf(productIds))
                .productTypeCode(productType.getCode())
                .companyName(companyName)
                .productName(productName)
                .lensSubType(LensSubType.SINGLE_VISION)
                .material(material)
                .type(type)
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

    private List<Long> resolveSupplierIds(SingleVisionCreateRequest request) {
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

    private List<PowerPair> buildPowerPairs(SingleVisionCreateRequest request) {
        boolean cylEnabled = Boolean.TRUE.equals(request.getCylEnabled());
        if (request.getAdditionMethod() == LensAdditionMethod.SINGLE) {
            BigDecimal sph = requiredDecimal(request.getSph(), "sph is required for SINGLE mode");
            validateQuarterStep(sph, "sph");
            validateRange(sph, SPH_MIN, SPH_MAX, "sph");

            BigDecimal cyl = null;
            if (cylEnabled) {
                cyl = requiredDecimal(request.getCyl(), "cyl is required when cylEnabled=true");
                validateQuarterStep(cyl, "cyl");
                validateRange(cyl, CYL_MIN, CYL_MAX, "cyl");
            }
            return List.of(new PowerPair(sph, cyl));
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

        List<BigDecimal> sphValues = expandRange(sphStart, sphEnd);
        List<BigDecimal> cylValues = new ArrayList<>();
        cylValues.add(null);

        if (cylEnabled) {
            BigDecimal cylStart = requiredDecimal(request.getCylStart(), "cylStart is required when cylEnabled=true");
            BigDecimal cylEnd = requiredDecimal(request.getCylEnd(), "cylEnd is required when cylEnabled=true");
            validateQuarterStep(cylStart, "cylStart");
            validateQuarterStep(cylEnd, "cylEnd");
            validateRange(cylStart, CYL_MIN, CYL_MAX, "cylStart");
            validateRange(cylEnd, CYL_MIN, CYL_MAX, "cylEnd");
            if (cylStart.compareTo(cylEnd) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cylStart must be less than or equal to cylEnd");
            }
            cylValues = expandRange(cylStart, cylEnd);
        }

        Set<PowerPair> pairs = new LinkedHashSet<>();
        for (BigDecimal sph : sphValues) {
            for (BigDecimal cyl : cylValues) {
                pairs.add(new PowerPair(sph, cyl));
                if (pairs.size() > MAX_VARIANTS_PER_REQUEST) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Too many variants generated. Reduce the selected range."
                    );
                }
            }
        }
        return List.copyOf(pairs);
    }

    private List<BigDecimal> expandRange(BigDecimal start, BigDecimal end) {
        List<BigDecimal> values = new ArrayList<>();
        for (BigDecimal current = start; current.compareTo(end) <= 0; current = current.add(STEP)) {
            values.add(current);
        }
        return values;
    }

    private void validateCommercialFields(SingleVisionCreateRequest request) {
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

    private String normalizeAndValidateLensType(String value) {
        String normalized = normalizeRequired(value, "type is required");
        String upperValue = normalized.toUpperCase();
        if (!ALLOWED_TYPES.contains(upperValue)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lens type for SINGLE_VISION");
        }
        return upperValue;
    }

    private String normalizeAndValidateMaterial(String value) {
        String normalized = normalizeRequired(value, "material is required");
        String canonical = ALLOWED_MATERIALS.get(normalized.toLowerCase());
        if (canonical == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid material for SINGLE_VISION. Allowed: " + String.join(", ", ALLOWED_MATERIALS.values())
            );
        }
        return canonical;
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
        return "SV-" + productId + "-001";
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String buildSingleLenseName(String baseName , PowerPair pair){
        String sphPrefix =  pair.sph.toString();
        String cylPrefix =  pair.cyl != null ? "/" +pair.cyl.toString() : "";
        return baseName + "("  +  sphPrefix  + cylPrefix + ")";
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

    private record PowerPair(BigDecimal sph, BigDecimal cyl) {
    }
}
