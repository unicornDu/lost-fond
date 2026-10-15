package com.shiguang.lostfound.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiguang.lostfound.config.FileProperties;
import com.shiguang.lostfound.entity.ClaimRecord;
import com.shiguang.lostfound.entity.Item;
import com.shiguang.lostfound.entity.ItemImage;
import com.shiguang.lostfound.entity.Message;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.ClaimRecordMapper;
import com.shiguang.lostfound.mapper.ItemImageMapper;
import com.shiguang.lostfound.mapper.ItemMapper;
import com.shiguang.lostfound.mapper.MessageMapper;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.service.FileStorageService;
import com.shiguang.lostfound.util.PHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 演示数据初始化：读取 uploads/seed/manifest.json，建用户、建启事，
 * 并用真实 JImageHash 为每张图片计算 64 位 pHash 入库（保证以图搜图开箱即用）。
 * 幂等：item 表非空则跳过。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ClaimRecordMapper claimMapper;
    private final MessageMapper messageMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorage;
    private final PHashUtil pHashUtil;
    private final FileProperties fileProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 演示口令外置在 application-local.yml（seed.*）；为空则回退随机并打印警告（审查 2.3）。 */
    @Value("${seed.admin-password:}")
    private String seedAdminPassword;
    @Value("${seed.user-password:}")
    private String seedUserPassword;

    private static final DateTimeFormatter[] FMTS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    };

    @Override
    public void run(String... args) {
        // 修复 B-08：每次启动先补生成缺失缩略图（幂等：已存在即跳过；只扫 seed 与 yyyy/MM），
        // 保证列表页 thumb 字段指向真实文件（老库/新克隆都能自愈）
        try {
            int thumbs = fileStorage.ensureThumbs();
            if (thumbs > 0) log.info("补生成缩略图 {} 张", thumbs);
        } catch (Exception e) {
            log.warn("补生成缩略图失败（忽略）", e);
        }
        Long cnt = itemMapper.selectCount(null);
        if (cnt != null && cnt > 0) {
            log.info("数据库已有 {} 条启事，跳过初始化", cnt);
            return;
        }
        File manifest = Paths.get(fileProperties.getUploadDir(), "seed", "manifest.json")
                .toAbsolutePath().normalize().toFile();
        if (!manifest.exists()) {
            log.warn("未找到种子清单 {}，跳过初始化（可先运行 scripts/gen_seed_images.py）", manifest);
            return;
        }
        try {
            seed(manifest);
        } catch (Exception e) {
            log.error("初始化演示数据失败", e);
        }
    }

    private void seed(File manifest) throws Exception {
        JsonNode root = objectMapper.readTree(manifest);

        // 演示口令：优先取配置（application-local.yml 的 seed.*），为空回退随机（审查 2.3）
        String adminPwd = (seedAdminPassword == null || seedAdminPassword.isBlank())
                ? UUID.randomUUID().toString() : seedAdminPassword;
        String userPwd = (seedUserPassword == null || seedUserPassword.isBlank())
                ? UUID.randomUUID().toString() : seedUserPassword;
        if (seedAdminPassword == null || seedAdminPassword.isBlank())
            log.warn("seed.admin-password 未配置，管理员口令回退为随机值（见启动日志）");
        String adminAcc = null;
        String sampleUser = null;

        // 管理员账号
        JsonNode adminNode = root.path("admin");
        if (adminNode != null && adminNode.has("account")) {
            String acc = adminNode.get("account").asText();
            adminAcc = acc;
            if (userMapper.selectCount(new QueryWrapper<User>().eq("account", acc)) == 0) {
                User admin = new User();
                admin.setAccount(acc);
                admin.setPassword(passwordEncoder.encode(adminPwd));
                admin.setNickname(adminNode.path("nickname").asText("管理员"));
                admin.setAvatar("🛡️");
                admin.setRole(1);
                admin.setStatus(1);
                userMapper.insert(admin);
                log.info("已创建管理员账号 {}", acc);
            }
        }

        // 发布人账号 → userId 映射（口令随机，仅作展示用发布者）
        Map<String, Long> accountToId = new HashMap<>();
        Map<String, String> accountToNick = new HashMap<>();
        JsonNode items = root.path("items");
        for (JsonNode it : items) {
            String acc = it.path("account").asText();
            accountToNick.putIfAbsent(acc, it.path("nickname").asText("同学"));
        }
        for (Map.Entry<String, String> e : accountToNick.entrySet()) {
            User u = userMapper.selectOne(new QueryWrapper<User>().eq("account", e.getKey()));
            if (u == null) {
                u = new User();
                u.setAccount(e.getKey());
                u.setPassword(passwordEncoder.encode(userPwd));
                u.setNickname(e.getValue());
                u.setAvatar(pickAvatar());
                u.setRole(0);
                u.setStatus(1);
                userMapper.insert(u);
                if (sampleUser == null) sampleUser = e.getKey();
            }
            accountToId.put(e.getKey(), u.getId());
        }

        int itemCount = 0, imgCount = 0, hashOk = 0;
        List<Item> insertedItems = new ArrayList<>();
        for (JsonNode it : items) {
            Item item = new Item();
            item.setType(it.path("type").asText("lost"));
            item.setTitle(it.path("title").asText());
            item.setCategory(it.path("category").asText("其他"));
            item.setLocation(it.path("location").asText(""));
            item.setHappenTime(parseTime(it.path("happenTime").asText(null)));
            item.setDescription(it.path("desc").asText(""));
            int audit = it.path("auditStatus").asInt(1);
            // 修复 B-09：待审核/已驳回启事的业务状态一律为初始状态（lost→寻找中 / found→待认领），
            // 杜绝"还没过审却已团聚"的脏数据（它曾让首页 reunited 与墙页可见数对不上）
            item.setStatus(audit != 1
                    ? ("found".equals(item.getType()) ? "待认领" : "寻找中")
                    : it.path("status").asText("寻找中"));
            item.setViews(it.path("views").asInt(0));
            item.setUserId(accountToId.get(it.path("account").asText()));
            item.setAuditStatus(audit); // 审查 2.4：种子可带待审核
            itemMapper.insert(item);
            itemCount++;
            insertedItems.add(item);

            String cover = null;
            int sort = 0;
            for (JsonNode imgNode : it.path("images")) {
                String url = imgNode.asText();
                if (cover == null) cover = url;
                Long phash = null;
                try {
                    File f = fileStorage.resolve(url);
                    if (f != null && f.exists()) {
                        phash = pHashUtil.hash(f);
                        hashOk++;
                    } else {
                        log.warn("种子图片不存在，跳过 pHash: {}", url);
                    }
                } catch (Exception ex) {
                    log.warn("计算 pHash 失败: {}", url, ex);
                }
                ItemImage img = new ItemImage();
                img.setItemId(item.getId());
                img.setImgUrl(url);
                img.setPhash(phash);
                img.setSort(sort++);
                itemImageMapper.insert(img);
                imgCount++;
            }
            if (cover != null) {
                item.setCoverImg(cover);
                itemMapper.updateById(item);
            }
        }
        log.info("演示数据初始化完成：用户 {} 个，启事 {} 条，图片 {} 张，成功计算 pHash {} 张",
                accountToId.size(), itemCount, imgCount, hashOk);
        // 打印演示账号，便于直接登录演示（审查 2.3 验收：不再需要手工 SQL 提权）
        log.info("演示账号 → 管理员: {} / {} ； 普通用户示例: {} / {}",
                adminAcc, adminPwd, sampleUser, userPwd);

        // 审查 2.10：预置认领记录（三态）与站内消息，避免演示时消息/认领列表为空
        seedRelations(insertedItems, accountToId);
    }

    /** 为前若干条启事预置 审核中/已通过/已拒绝 三种认领记录 + 对应站内消息（审查 2.10）。 */
    private void seedRelations(List<Item> items, Map<String, Long> accountToId) {
        List<Long> userIds = new ArrayList<>(accountToId.values());
        if (userIds.size() < 3 || items.size() < 6) return;
        int made = 0;
        for (int i = 0; i < 3 && i < items.size(); i++) {
            Item item = items.get(i);
            Long owner = item.getUserId();
            // 修复 B-09：三条认领记录必须用互不相同的申请人（原实现 c1 复用两次，
            // 违反 ClaimService.apply 自己的"同人同物仅一条审核中"不变量）
            List<Long> candidates = userIds.stream().filter(u -> !u.equals(owner)).toList();
            if (candidates.size() < 3) continue;
            Long c1 = candidates.get(0);
            Long c2 = candidates.get(1);
            Long c3 = candidates.get(2);

            String[] statuses = {"审核中", "已通过", "已拒绝"};
            Long[] claimants = {c1, c2, c3};
            for (int k = 0; k < 3; k++) {
                ClaimRecord c = new ClaimRecord();
                c.setItemId(item.getId());
                c.setClaimantId(claimants[k]);
                c.setProof("演示凭证" + (k + 1) + "：物品内侧有我的姓名贴/划痕特征");
                c.setContact("1380000000" + k);
                c.setStatus(statuses[k]);
                c.setReply(k == 1 ? "确认是失主，已通过" : (k == 2 ? "特征不符，已拒绝" : null));
                claimMapper.insert(c);

                Message m = new Message();
                m.setUserId(owner);
                m.setType("claim");
                m.setTitle("收到新的认领申请");
                m.setContent("有人申请认领「" + item.getTitle() + "」，请查看凭证并审核。");
                m.setRefId(item.getId());
                m.setIsRead(0);
                messageMapper.insert(m);
                made++;
            }
            // 物品状态与认领结果保持一致
            item.setStatus("lost".equals(item.getType()) ? "已找回" : "已认领");
            itemMapper.updateById(item);
        }
        // 再留几条"审核中"的物品处于 认领申请中 状态
        for (int i = 3; i < 6 && i < items.size(); i++) {
            Item item = items.get(i);
            if (!"寻找中".equals(item.getStatus()) && !"待认领".equals(item.getStatus())) continue;
            Long owner = item.getUserId();
            Long c1 = userIds.stream().filter(u -> !u.equals(owner)).findFirst().orElse(null);
            if (c1 == null) continue;
            ClaimRecord c = new ClaimRecord();
            c.setItemId(item.getId());
            c.setClaimantId(c1);
            c.setProof("演示凭证：只有失主知道的细节特征");
            c.setStatus("审核中");
            claimMapper.insert(c);
            item.setStatus("认领申请中");
            itemMapper.updateById(item);
            Message m = new Message();
            m.setUserId(owner); m.setType("claim"); m.setTitle("收到新的认领申请");
            m.setContent("有人申请认领「" + item.getTitle() + "」，请查看凭证并审核。");
            m.setRefId(item.getId()); m.setIsRead(0);
            messageMapper.insert(m);
            made++;
        }
        log.info("已预置认领/消息种子数据 {} 条", made);
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        String v = s.trim();
        for (DateTimeFormatter f : FMTS) {
            try { return LocalDateTime.parse(v, f); } catch (Exception ignored) { }
        }
        try {
            return LocalDateTime.parse(v + " 00:00", FMTS[1]);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String pickAvatar() {
        String[] a = {"🦊", "🐰", "🐼", "🐯", "🦁", "🐨", "🐷", "🐸", "🐵", "🦄"};
        return a[(int) (Math.random() * a.length)];
    }
}
