package com.shiguang.lostfound.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果包装。
 */
@Data
public class PageResult<T> implements Serializable {
    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> p = new PageResult<>();
        p.records = records; p.total = total; p.page = page; p.size = size;
        return p;
    }
}
