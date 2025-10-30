package com.example.learnperformancetest.controller;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.request.ProductRequest;
import com.example.learnperformancetest.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("v1/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody ProductRequest request) {
        logger.info("REQUEST: POST /v1/api/products body={}", request);

        ProductDto response = productService.create(request);

        logger.info("RESPONSE: status=200 body={}", response.toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        logger.info("REQUEST: PUT /v1/api/products/{} body={}", id, request);

        ProductDto response = productService.update(id, request);

        logger.info("RESPONSE: status=200 body={}", response.toString());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        logger.info("REQUEST: DELETE /v1/api/products/{}", id);

        productService.delete(id);

        logger.info("RESPONSE: status=204 (no content)");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        logger.info("REQUEST: GET /v1/api/products/{}", id);
        ProductDto response = productService.getById(id);
        logger.info("RESPONSE: status=200 body={}", response.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAll(Pageable pageable) {
        logger.info("REQUEST: GET /v1/api/products page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductDto> response = productService.getAll(pageable);

        logger.info("RESPONSE: status=200 body=Page(totalElements={}, totalPages={})", response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> getProductsByName(@RequestParam String name, Pageable pageable) {
        logger.info("REQUEST: GET /v1/api/products?name={} page={} size={}", name, pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductDto> response = productService.getProductsByName(name, pageable);

        logger.info("RESPONSE: status=200 body=Page(totalElements={}, totalPages={})", response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<Page<ProductDto>> getProductsByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, Pageable pageable) {
        logger.info("REQUEST: GET /v1/api/products/by-date-range?startDate={}&endDate={} page={} size={}", startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductDto> response = productService.getProductsByDateRange(startDate, endDate, pageable);

        logger.info("RESPONSE: status=200 body=Page(totalElements={}, totalPages={})", response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
