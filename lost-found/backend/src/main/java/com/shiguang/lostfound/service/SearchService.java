package com.shiguang.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiguang.lostfound.common.BusinessException;
import com.shiguang.lostfound.config.ImageSearchProperties;
import com.shiguang.lostfound.dto.ImageMatch;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.ItemImage;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.util.PHashUtil;
import com.shiguang.lostfound.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M3 以图搜图（核心亮点）：
 * 上传查询图 → 计算 64 位 pHash → 在 item_image 表用 BIT_COUNT(phash XOR q) 求汉明距离
 * → 阈值(≤10)过滤 → 按距离升序取 Top-N → 转成相似度百分比返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ItemImageMapper itemImageMapper;
    private final ItemMapper itemMapper;
    private final PHashUtil pHashUtil;
    private final ImageSearchProperties props;
    private final ItemService itemService;
    private final FileStorageService fileStorage;

    public List<ItemVO> searchByImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请上传查询图片");
        long q;
        try {
            q = pHashUtil.hash(file.getBytes());
        } catch (Exception e) {
            throw new BusinessException("图片解析失败，请上传 JPG/PNG");
        }
        return searchByHash(q);
    }

    public List<ItemVO> searchByHash(long q) {
        List<ImageMatch> matches = itemImageMapper.searchByHash(q, props.getHammingThreshold(), props.getTopN());
        if (matches.isEmpty()) return List.of();

        List<Long> ids = matches.stream().map(ImageMatch::getItemId).collect(Collectors.toList());
        Map<Long, Item> itemMap = itemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Item::getId, i -> i, (a, b) -> a));
        Map<Long, String> nick = itemService.loadNicknames(
                itemMap.values().stream().map(Item::getUserId).collect(Collectors.toSet()));

        List<ItemVO> result = new ArrayList<>();
        for (ImageMatch m : matches) {
            Item it = itemMap.get(m.getItemId());
            if (it == null || isHidden(it.getStatus())) continue;
            if (it.getAuditStatus() != null && it.getAuditStatus() != 1) continue;
            ItemVO vo = itemService.toVO(it, nick, null);
            vo.setSimilarity(similarityFromDist(m.getDist()));
            result.add(vo);
        }
        result.sort(Comparator.comparingDouble(ItemVO::getSimilarity).reversed());
        return result;
    }

    private boolean isHidden(String status) {
        return "已下架".equals(status) || "已关闭".equals(status);
    }

    private double similarityFromDist(int dist) {
        double sim = (1.0 - (double) dist / props.getBitResolution()) * 100.0;
        return Math.round(sim * 10.0) / 10.0;
    }

    /**
     * 为已入库但尚未计算 pHash 的图片补算指纹（用于初始化演示数据 / 修复漏算）。
     * 返回成功补算的图片数量。
     */
    public int reindex() {
        List<ItemImage> pending = itemImageMapper.selectList(
                new QueryWrapper<ItemImage>().isNull("phash"));
        int ok = 0;
        for (ItemImage img : pending) {
            try {
                File f = fileStorage.resolve(img.getImgUrl());
                if (f == null || !f.exists()) continue;
                long h = pHashUtil.hash(f);
                img.setPhash(h);
                itemImageMapper.updateById(img);
                ok++;
            } catch (Exception e) {
                log.warn("reindex 失败 itemId={} url={}", img.getItemId(), img.getImgUrl(), e);
            }
        }
        log.info("reindex 完成，补算 {} 张图片的 pHash", ok);
        return ok;
    }

    /**
     * 审查 2.26：真·视觉相似推荐。取该启事封面图的 pHash，复用同一条汉明距离 SQL
     * 查最近邻，排除自身后返回 Top-n。详情页"以图搜图联想"改调本方法（有接口出处）。
     */
    public List<ItemVO> similarTo(Long itemId, int n) {
        List<ItemImage> imgs = itemImageMapper.selectList(new QueryWrapper<ItemImage>()
                .eq("item_id", itemId).isNotNull("phash").orderByAsc("sort"));
        if (imgs.isEmpty()) return List.of();
        long q = imgs.get(0).getPhash();
        List<ItemVO> hits = searchByHash(q);
        return hits.stream()
                .filter(v -> !v.getId().equals(itemId))
                .limit(Math.max(1, n))
                .collect(Collectors.toList());
    }

    /** 已建立 pHash 索引的图片总数（以图搜图页展示「已在 N 张图片中比对」）。 */
    public long indexedCount() {
        return itemImageMapper.selectCount(new QueryWrapper<ItemImage>().isNotNull("phash"));
    }
}
