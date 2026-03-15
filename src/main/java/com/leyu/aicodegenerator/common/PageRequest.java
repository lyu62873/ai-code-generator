package com.leyu.aicodegenerator.common;

import lombok.Data;

@Data
public class PageRequest {

    // Page Number
    private int pageNum = 1;

    // Page Size
    private int pageSize = 10;

    // Sort Field
    private String sortField;

    // Sort Order
    private String sortOrder = "descend";

}
