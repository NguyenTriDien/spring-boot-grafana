package com.example.learnperformancetest.mapper;

import com.example.learnperformancetest.dto.MerchantDto;
import com.example.learnperformancetest.entity.Merchant;
import com.example.learnperformancetest.request.MerchantRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MerchantMapper {
    MerchantDto toDto(Merchant merchant);
    Merchant toEntity(MerchantRequest request);
}
