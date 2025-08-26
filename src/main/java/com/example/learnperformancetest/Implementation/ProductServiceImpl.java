package com.example.learnperformancetest.Implementation;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.entity.Product;
import com.example.learnperformancetest.mapper.ProductMapper;
import com.example.learnperformancetest.repository.MerchantRepository;
import com.example.learnperformancetest.repository.ProductRepository;
import com.example.learnperformancetest.request.ProductRequest;
import com.example.learnperformancetest.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductDto create(ProductRequest request) {
        if (!merchantRepository.existsById(request.getMerchantId())) {
            throw new RuntimeException("Merchant with id " + request.getMerchantId() + " not found");
        }
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Override
    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productMapper.updateEntityFromRequest(request, product);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAll(Pageable pageable) {
        return productRepository.findAllByIsDeletedFalse(pageable)
                .map(productMapper::toDto);
    }
}
