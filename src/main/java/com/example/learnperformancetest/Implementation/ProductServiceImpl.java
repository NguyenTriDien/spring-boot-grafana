package com.example.learnperformancetest.Implementation;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.entity.Merchant;
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

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductDto create(ProductRequest request) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        Product product = productMapper.toEntity(request);
        product.setMerchant(merchant);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @CachePut(value = "products", key = "#id")
    public ProductDto update(Long id, ProductRequest request) {
        productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        productMapper.updateEntityFromRequest(request, product);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return productMapper.toDto(product);
    }
    @Override
    public Page<ProductDto> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toDto);
    }
}
