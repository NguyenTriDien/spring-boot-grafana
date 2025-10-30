package com.example.learnperformancetest.service;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.request.ProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductService {
    ProductDto create(ProductRequest request);
    ProductDto update(Long id, ProductRequest request);
    void delete(Long id);
    ProductDto getById(Long id);
    Page<ProductDto> getAll(Pageable pageable);

    Page<ProductDto> getProductsByName(String name, Pageable pageable);
    
    // Lấy danh sách products theo khoảng thời gian tạo
    Page<ProductDto> getProductsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
