package com.shiguang.lostfound.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务后置动作工具（2026-10-18 审查修复）。
 * 背景：在 @Transactional 方法内直接做"磁盘删除 / 生成交付文件"这类不可回滚的副作用，
 * 一旦事务回滚会出现"库回滚了、文件已经没了"或"库没提交、文件已落盘"的不一致。
 * 约定：副作用统一推迟到事务提交之后执行；无事务上下文（单测/非事务调用）时立即执行。
 */
public final class TxUtil {

    private TxUtil() { }

    /** 事务提交后执行；没有活动事务同步时立即执行。异常由调用方自行捕获（这里兜底吞掉并打印）。 */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runQuietly(action);
                }
            });
        } else {
            runQuietly(action);
        }
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(TxUtil.class)
                    .warn("事务后置动作失败（已忽略，不影响主流程）", e);
        }
    }
}
