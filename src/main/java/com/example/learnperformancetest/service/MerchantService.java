package com.example.learnperformancetest.service;

import com.example.learnperformancetest.dto.MerchantDto;
import com.example.learnperformancetest.request.MerchantRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MerchantService {
    MerchantDto create(MerchantRequest request);
    MerchantDto update(Long id, MerchantRequest request);
    void delete(Long id);
    MerchantDto getById(Long id);
    Page<MerchantDto> getAll(Pageable pageable);
}
