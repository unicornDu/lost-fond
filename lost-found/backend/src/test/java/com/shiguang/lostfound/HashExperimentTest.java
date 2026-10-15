package com.shiguang.lostfound;

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审查 1.3：以图搜图准确率实验。
 * 索引 = 每物体一张 ref；查询分两组：
 *  - mild_*：现实重拍场景（裁剪85%/亮度/对比度/灰度模糊/缩放75%）—— 平台核心场景
 *  - pos_* ：极端攻击（旋转5°/10°、裁剪70%、JPEG q30、模糊2px、亮度1.3、缩放50%）
 * 对 aHash/dHash/pHash × 阈值{4,6,8,10,12} 分组统计 Precision/Recall/F1
 * （检索库含全部 6 个物体，跨物体误检拉低 Precision，即自带负样本）。
 * 输出 scripts/exp_results.csv（分组指标）与 scripts/exp_per_transform.csv（逐变换召回@10）。
 */
public class HashExperimentTest {

    private static final int BITS = 64;
    private static final List<String> OBJS = List.of("cup", "keys", "earbuds", "card", "umbrella", "scarf");
    private static final int[] THRESHOLDS = {4, 6, 8, 10, 12};

    private static long hash(HashingAlgorithm alg, File f) throws Exception {
        BufferedImage img = ImageIO.read(f);
        return alg.hash(img).getHashValue().longValue();
    }
    private static int dist(long a, long b) { return Long.bitCount(a ^ b); }

    @Test
    public void runExperiment() throws Exception {
        Path exp = Paths.get("uploads/_exp").toAbsolutePath();
        Map<String, HashingAlgorithm> algs = new LinkedHashMap<>();
        algs.put("pHash", new PerceptiveHash(BITS));
        algs.put("aHash", new AverageHash(BITS));
        algs.put("dHash", new DifferenceHash(BITS, DifferenceHash.Precision.Simple));

        Map<String, Map<String, Long>> refHash = new LinkedHashMap<>();
        for (String o : OBJS) {
            Map<String, Long> m = new LinkedHashMap<>();
            for (Map.Entry<String, HashingAlgorithm> e : algs.entrySet())
                m.put(e.getKey(), hash(e.getValue(), exp.resolve(o).resolve("ref.jpg").toFile()));
            refHash.put(o, m);
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"group,algorithm,threshold,precision,recall,f1,queries"});
        List<String> perTransform = new ArrayList<>();
        perTransform.add("transform,group,pHash@10,aHash@10,dHash@10,n");

        for (String group : List.of("mild", "strong")) {
            String prefix = group.equals("mild") ? "mild_" : "pos_";
            for (Map.Entry<String, HashingAlgorithm> ae : algs.entrySet()) {
                String alg = ae.getKey();
                for (int t : THRESHOLDS) {
                    double pSum = 0, rSum = 0; int n = 0;
                    for (String o : OBJS) {
                        List<File> qs = filesWithPrefix(exp.resolve(o), prefix);
                        for (File q : qs) {
                            long qh = hash(ae.getValue(), q);
                            Set<String> retrieved = new LinkedHashSet<>();
                            for (String r : OBJS)
                                if (dist(qh, refHash.get(r).get(alg)) <= t) retrieved.add(r);
                            boolean hit = retrieved.contains(o);
                            pSum += retrieved.isEmpty() ? 0 : (hit ? 1.0 / retrieved.size() : 0.0);
                            rSum += hit ? 1.0 : 0.0;
                            n++;
                        }
                    }
                    double P = pSum / n, R = rSum / n;
                    double F1 = (P + R) == 0 ? 0 : 2 * P * R / (P + R);
                    rows.add(new String[]{String.format("%s,%s,%d,%.4f,%.4f,%.4f,%d", group, alg, t, P, R, F1, n)});
                }
            }
            // 逐变换召回 @t=10
            Set<String> transforms = new TreeSet<>();
            for (String o : OBJS)
                filesWithPrefix(exp.resolve(o), prefix)
                        .forEach(f -> transforms.add(f.getName().replace(prefix, "").replace(".jpg", "")));
            for (String tr : transforms) {
                StringBuilder sb = new StringBuilder(tr + "," + group);
                for (String alg : List.of("pHash", "aHash", "dHash")) {
                    int hit = 0, tot = 0;
                    for (String o : OBJS) {
                        File q = exp.resolve(o).resolve(prefix + tr + ".jpg").toFile();
                        if (!q.exists()) continue;
                        tot++;
                        if (dist(hash(algs.get(alg), q), refHash.get(o).get(alg)) <= 10) hit++;
                    }
                    sb.append(',').append(tot == 0 ? "NA" : String.format("%.2f", (double) hit / tot));
                }
                sb.append(',').append(OBJS.size());
                perTransform.add(sb.toString());
            }
        }

        try (PrintWriter w = new PrintWriter("scripts/exp_results.csv", StandardCharsets.UTF_8)) {
            rows.forEach(r -> w.println(r[0]));
        }
        Files.write(Paths.get("scripts/exp_per_transform.csv"), perTransform, StandardCharsets.UTF_8);
        rows.forEach(r -> System.out.println(r[0]));
        System.out.println("---- per-transform recall @t=10 ----");
        perTransform.forEach(System.out::println);
    }

    private List<File> filesWithPrefix(Path dir, String prefix) throws Exception {
        return Files.list(dir).map(Path::toFile)
                .filter(f -> f.getName().startsWith(prefix))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }
}
