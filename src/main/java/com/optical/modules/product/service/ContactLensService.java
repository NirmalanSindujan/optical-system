package com.optical.modules.product.service;

import com.optical.common.exception.ResourceNotFoundException;
import com.optical.modules.product.dto.ContactLensCreateRequest;
import com.optical.modules.product.dto.ContactLensCreateResponse;
import com.optical.modules.product.dto.ContactLensDetailResponse;
import com.optical.modules.product.dto.ContactLensListResponse;
import com.optical.modules.product.dto.ContactLensPageResponse;
import com.optical.modules.product.dto.ContactLensUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class ContactLensService {

    private static final String DEFAULT_PRODUCT_TYPE_CODE = "LENS";
    private static final String DEFAULT_UOM_CODE = "PA";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final UomRepository uomRepository;
    private final LensVariantDetailsRepository lensVariantDetailsRepository;
    private final ProductSupportService productSupportService;

    @Transactional
    public ContactLensCreateResponse create(ContactLensCreateRequest request) {
        ProductType productType = productTypeRepository.findById(DEFAULT_PRODUCT_TYPE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default product type not found: " + DEFAULT_PRODUCT_TYPE_CODE));
        Uom uom = uomRepository.findById(DEFAULT_UOM_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Default UOM not found: " + DEFAULT_UOM_CODE));

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        String color = normalizeRequired(request.getColor(), "color is required");
        String baseCurve = normalizeRequired(request.getBaseCurve(), "baseCurve is required");

        validateCommercialFields(request.getPurchasePrice(), request.getSellingPrice(), request.getQuantity());
        List<Long> supplierIds = resolveSupplierIds(request.getSupplierIds(), request.getSupplierId());

        Product product = createProduct(productType, companyName, productName, supplierIds);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(generateSku(product.getId()));
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
        details.setLensSubType(LensSubType.CONTACT_LENS.name());
        details.setColor(color);
        details.setBaseCurve(baseCurve);
        lensVariantDetailsRepository.save(details);

        return ContactLensCreateResponse.builder()
                .productId(product.getId())
                .variantId(savedVariant.getId())
                .sku(savedVariant.getSku())
                .productTypeCode(productType.getCode())
                .companyName(companyName)
                .productName(productName)
                .lensSubType(LensSubType.CONTACT_LENS)
                .color(color)
                .baseCurve(baseCurve)
                .uomCode(uom.getCode())
                .supplierIds(supplierIds)
                .suppliers(productSupportService.resolveSupplierInfos(supplierIds))
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantity(request.getQuantity())
                .extra(normalize(request.getExtra()))
                .build();
    }

    @Transactional(readOnly = true)
    public ContactLensDetailResponse getById(Long productId) {
        return mapContactLensDetail(findContactLensByProductId(productId));
    }

    @Transactional(readOnly = true)
    public ContactLensPageResponse search(String q, int page, int size) {
        Page<LensVariantDetails> result = lensVariantDetailsRepository.searchByLensSubType(
                LensSubType.CONTACT_LENS.name(),
                normalize(q),
                PageRequest.of(page, size)
        );

        List<ContactLensListResponse> items = result.getContent().stream()
                .map(this::mapContactLensListItem)
                .toList();

        return ContactLensPageResponse.builder()
                .items(items)
                .totalCounts(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public ContactLensDetailResponse update(Long productId, ContactLensUpdateRequest request) {
        LensVariantDetails details = findContactLensByProductId(productId);
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();

        String companyName = normalizeRequired(request.getCompanyName(), "companyName is required");
        String productName = normalizeRequired(request.getName(), "name is required");
        String color = normalizeRequired(request.getColor(), "color is required");
        String baseCurve = normalizeRequired(request.getBaseCurve(), "baseCurve is required");
        List<Long> supplierIds = resolveSupplierIds(request.getSupplierIds(), request.getSupplierId());

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

        details.setColor(color);
        details.setBaseCurve(baseCurve);

        productSupportService.replaceSupplierLinks(product, supplierIds);

        return mapContactLensDetail(details);
    }

    @Transactional
    public void delete(Long productId) {
        findContactLensByProductId(productId);
        productSupportService.deleteProduct(productId);
    }

    private LensVariantDetails findContactLensByProductId(Long productId) {
        LensVariantDetails details = lensVariantDetailsRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact lens not found"));

        if (!LensSubType.CONTACT_LENS.name().equals(details.getLensSubType())) {
            throw new ResourceNotFoundException("Contact lens not found");
        }

        return details;
    }

    private ContactLensDetailResponse mapContactLensDetail(LensVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return ContactLensDetailResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .name(product.getName())
                .lensSubType(LensSubType.CONTACT_LENS)
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

    private ContactLensListResponse mapContactLensListItem(LensVariantDetails details) {
        ProductVariant variant = details.getVariant();
        Product product = variant.getProduct();
        List<Long> supplierIds = productSupportService.resolveSupplierIdsForProduct(product.getId());

        return ContactLensListResponse.builder()
                .productId(product.getId())
                .variantId(variant.getId())
                .companyName(product.getBrandName())
                .name(product.getName())
                .sku(variant.getSku())
                .uomCode(variant.getUom().getCode())
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

    private void validateCommercialFields(BigDecimal purchasePrice, BigDecimal sellingPrice, BigDecimal quantity) {
        if (purchasePrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice is required");
        }
        if (sellingPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice is required");
        }
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity is required");
        }
        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchasePrice cannot be negative");
        }
        if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sellingPrice cannot be negative");
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity cannot be negative");
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

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String generateSku(Long productId) {
        return "CL-" + productId + "-001";
    }
}
