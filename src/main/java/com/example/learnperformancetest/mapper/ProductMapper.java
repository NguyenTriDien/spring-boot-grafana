package com.example.learnperformancetest.mapper;

import com.example.learnperformancetest.dto.ProductDto;
import com.example.learnperformancetest.entity.Merchant;
import com.example.learnperformancetest.entity.Product;
import com.example.learnperformancetest.request.ProductRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "merchantId", source = "merchant.id")
    ProductDto toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "merchant", source = "merchantId", qualifiedByName = "merchantFromId")
    Product toEntity(ProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "merchant", source = "merchantId", qualifiedByName = "merchantFromId")
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);

    @Named("merchantFromId")
    default Merchant merchantFromId(Long id) {
        if (id == null) return null;
        Merchant m = new Merchant();
        m.setId(id);
        return m;
    }
}
