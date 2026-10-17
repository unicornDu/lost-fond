package com.example.common;

import lombok.Data;

import java.util.HashMap;
@Data
public class QueryPageParam {
    private static int  PAGE_SIZE=10;
    private static int PAGE_CURRENT=1;
    private int pageNum=PAGE_CURRENT;
    private int pageSize=PAGE_SIZE;
    private HashMap param=new HashMap();
}
