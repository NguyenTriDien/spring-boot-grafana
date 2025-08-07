package com.example.learnperformancetest.Implementation;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.entity.Product;
import com.example.learnperformancetest.repository.ProductRepository;
import com.example.learnperformancetest.request.ProductRequest;
import com.example.learnperformancetest.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private ProductDto mapToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }

    @Override
    public ProductDto create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();
        return mapToDto(productRepository.save(product));
    }

    @Override
    public ProductDto update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        return mapToDto(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public ProductDto getById(Long id) {
        return productRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
    }

    @Override
    public List<ProductDto> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}
