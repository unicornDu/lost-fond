package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.QueryPageParam;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import com.example.common.Result;
import java.util.Arrays;
import org.springframework.transaction.annotation.Transactional;

import com.example.service.GoodsService;
import com.example.entity.Goods;


/**
 * <p>
 * 失物管理 前端控制器
 * </p>
 *
 * @author ren
 * @since 2023-02-13
 */
@RestController
@RequestMapping("/goods")
    public class GoodsController {

@Resource
private GoodsService goodsService;

@PostMapping
public Result  save(@RequestBody Goods goods) {
        return Result.success(goodsService.saveOrUpdate(goods));
        }

@DeleteMapping("/{id}")
public Result delete(@PathVariable Integer id) {
        return Result.success(goodsService.removeById(id));
        }

@GetMapping
public Result indAll() {
        return  Result.success(goodsService.list());
        }

@GetMapping("/{id}")
public Result findOne(@PathVariable Integer id) {
        return  Result.success(goodsService.list());
        }

@GetMapping("/page")
public Result findPage(
@RequestParam(defaultValue = "") String username,
@RequestParam(defaultValue = "1") Integer pageNum,
@RequestParam(defaultValue = "10") Integer pageSize) {
        return  Result.success(goodsService.page(new Page<>(pageNum, pageSize)));
        }
@PostMapping("/delete/{ids}")
@Transactional
public Result deleteByIds(@PathVariable String[] ids) {
        return Result.success(goodsService.removeByIds(Arrays.asList(ids)));
        }

        @PostMapping("/listPage")
        public Result listPage(@RequestBody QueryPageParam query){
                IPage page=new Page(query.getPageNum(),query.getPageSize());
                HashMap param = query.getParam();
                String name= (String) param.get("name");
                LambdaQueryWrapper<Goods> lambdaQueryWrapper=new LambdaQueryWrapper<>();
                if(StringUtils.isNotBlank(name) && !"null".equals(name)){
                        lambdaQueryWrapper.like(Goods::getName,name);
                }

                IPage iPage = goodsService.page(page,lambdaQueryWrapper);

                return Result.success(iPage.getRecords());
        }
        }

