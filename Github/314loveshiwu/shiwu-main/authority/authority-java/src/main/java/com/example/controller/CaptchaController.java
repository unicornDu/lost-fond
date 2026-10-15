package com.example.controller;/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import com.example.common.Constant;
import com.example.common.Result;
import com.example.utils.RedisUtil;
import com.google.code.kaptcha.Producer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @Description
 * @Author
 * @Data 2023/1/9 17:05
 */
@RestController

public class CaptchaController {
    @Resource
    private Producer producer;
    @Resource
    private RedisUtil redisUtil;

    @GetMapping("/captcha")
    public Result getCaotcha() throws IOException {
        String key = UUID.randomUUID().toString();
//        String code = producer.createText();
//        使用hutool生成验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 30,5,100);
        captcha.setBackground(Color.white);


        String code=captcha.getCode();

        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(captcha.getImage(), "jpg", os);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        BufferedImage image = producer.createImage(code);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        ImageIO.write(image, "jpg", outputStream);
//        BASE64Encoder encoder = new BASE64Encoder();
//        String str = "data:image/jpeg;base64,";
//        String base64Img = str + encoder.encode(outputStream.toByteArray());

        redisUtil.hset(Constant.CAPTCHA_KEY, key, code, 120);
        return Result.success(
                MapUtil.builder()
                        .put("token", key)
                        .put("captchaImg", Base64.encode(os.toByteArray()))
                        .build()

        );

    }
}
