package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class Schema {
    private Long id;
    private String type;
    private List<Field> fields;
    private boolean optional;
    private String name;
}
