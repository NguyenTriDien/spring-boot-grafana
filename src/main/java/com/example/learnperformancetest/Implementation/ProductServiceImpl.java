package com.example.learnperformancetest.Implementation;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.entity.Product;
import com.example.learnperformancetest.exception.ProductNotFoundException;
import com.example.learnperformancetest.mapper.ProductMapper;
import com.example.learnperformancetest.repository.MerchantRepository;
import com.example.learnperformancetest.repository.ProductRepository;
import com.example.learnperformancetest.request.ProductRequest;
import com.example.learnperformancetest.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final ProductMapper productMapper;
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Override
    @Transactional
    @CachePut(value = "products", key = "#result.id")
    public ProductDto create(ProductRequest request) {
        logger.info("Creating product with request: {}", request);

        if (!merchantRepository.existsById(request.getMerchantId())) {
            logger.error("Merchant with id {} not found", request.getMerchantId());
            throw new RuntimeException("Merchant with id " + request.getMerchantId() + " not found");
        }
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        logger.info("Product created with id: {}", savedProduct.getId());
        return productMapper.toDto(savedProduct);
    }

    @Override
    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productMapper.updateEntityFromRequest(request, product);
        logger.info("Updating product with id: {}", id);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(Long id) {
        logger.info("Deleting product with id: {}", id);
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setIsDeleted(true);
        logger.info("Product with id: {} marked as deleted", id);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(Long id) {
        logger.info("Fetching product with id: {}", id);
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    logger.warn("Product with id {} not found", id);
                    return new ProductNotFoundException("Product " + id + " not found");
                });
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAll(Pageable pageable) {
        logger.info("Fetching all products with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAllByIsDeletedFalse(pageable)
                .map(productMapper::toDto);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByName(String name, Pageable pageable) {
        logger.info("Fetching products by name: {} with pagination: page {}, size {}", name, pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findByNameAndIsDeletedFalse(name, pageable)
                .map(productMapper::toDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        logger.info("Fetching products by date range: {} to {} with pagination: page {}, size {}", 
                startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());
        Page<Product> products = productRepository.findByCreatedAtBetweenAndIsDeletedFalse(startDate, endDate, pageable);
        logger.info("Start mappting to product dto");
        return products.map(productMapper::toDto);
    }
}
