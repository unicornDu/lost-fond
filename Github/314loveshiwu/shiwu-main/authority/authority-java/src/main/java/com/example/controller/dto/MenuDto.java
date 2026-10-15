package com.example.controller.dto;/*

/**
 * @Description
 * @Author
 * @Data 2023/1/9 15:32
 */


import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class MenuDto {
    private Long id;
    private String name;
    private String title;
    private String icon;
    private String path;
    private String component;
    private Integer orderNum;
    private List<MenuDto> children=new ArrayList<>();
}
