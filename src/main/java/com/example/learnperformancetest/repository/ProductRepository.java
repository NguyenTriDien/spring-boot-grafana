package com.example.learnperformancetest.repository;

import com.example.learnperformancetest.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndIsDeletedFalse(Long id);
    Optional<Product> findByNameAndMerchantIdAndIsDeletedFalse(String name, Long merchantId);
    Page<Product> findAllByIsDeletedFalse(Pageable pageable);
}
