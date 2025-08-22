package com.example.learnperformancetest.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long merchantId;
    private Boolean isDeleted = false;

}
