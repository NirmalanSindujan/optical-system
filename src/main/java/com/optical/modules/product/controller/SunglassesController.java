package com.optical.modules.product.controller;

import com.optical.modules.product.dto.ProductCreateResponse;
import com.optical.modules.product.dto.SunglassesCreateRequest;
import com.optical.modules.product.dto.SunglassesPageResponse;
import com.optical.modules.product.service.ProductService;
import com.optical.modules.product.service.SunglassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/sunglasses")
@RequiredArgsConstructor
@Tag(name = "Sunglasses", description = "Sunglasses management APIs")
public class SunglassesController {

    private final ProductService productService;
    private final SunglassService sunglassService;

    @GetMapping
    public SunglassesPageResponse getSunglasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return sunglassService.searchSunglasses(q, page, size);
    }


    @PostMapping
    public ProductCreateResponse createSunglasses(@Valid @RequestBody SunglassesCreateRequest request) {
        return sunglassService.createSunglasses(request);
    }


}
