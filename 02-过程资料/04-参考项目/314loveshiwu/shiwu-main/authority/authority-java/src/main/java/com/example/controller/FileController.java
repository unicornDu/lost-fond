package com.example.controller;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

/**
 * @Description
 * @Author
 * @Data 2023/1/11 22:52
 */
@RestController
@RequestMapping("/file")
public class FileController {


    @Value("${files.path}")
    private String path;

    /**
     * 文件上传
     *
     * @param
     * @return
     */
    @PostMapping("/upload")
    public Result upload(@RequestBody MultipartFile file) {
        if (file == null ||file.isEmpty()) {
            return Result.success("请选择文件再上传，参数名multipartFile");
        }
        //获取文件类型
        String contentType = file.getContentType();
        //获取文件名
        String fileName = file.getOriginalFilename();
//        获取文件后缀名
        String fileSuffix = FileUtil.extName(fileName);
//        获取文件大小
        long size = file.getSize();
        //使用时间戳+3位随机数作为新的文件名
        long milli = Instant.now().toEpochMilli();
        String randomNumbers = RandomUtil.randomNumbers(3);
        //生成新的文件名
        String newFileName = milli + randomNumbers + "." + fileSuffix;
        //创建文件夹,如果存在则不创建
        FileUtil.mkdir(path);
        //文件路径
        String filePath = path + newFileName;
        try {
            FileUtil.writeBytes(file.getBytes(), filePath);
            return Result.success("http://localhost:8888/files/"+newFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
