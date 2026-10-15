package com.example.controller.dto;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import lombok.Data;

/**
 * @Description
 * @Author
 * @Data 2023/1/15 13:47
 */
@Data
public class ColumnDto {
    private String name;
    private String type;
    private String description;
    private String comment;
    private String field;
    private Boolean affirmative;

}
