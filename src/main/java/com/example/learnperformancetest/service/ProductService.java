package com.example.learnperformancetest.service;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.request.ProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductDto create(ProductRequest request);
    ProductDto update(Long id, ProductRequest request);
    void delete(Long id);
    ProductDto getById(Long id);
    Page<ProductDto> getAll(Pageable pageable);
}
