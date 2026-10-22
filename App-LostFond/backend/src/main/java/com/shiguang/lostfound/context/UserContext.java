package com.shiguang.lostfound.context;

/**
 * 当前登录用户上下文（由 JwtInterceptor 写入，请求结束清理）。
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }
    public static Long getUserId() { return USER_ID.get(); }
    public static Integer getRole() { return ROLE.get(); }
    public static boolean isAdmin() { return ROLE.get() != null && ROLE.get() == 1; }
    public static void clear() { USER_ID.remove(); ROLE.remove(); }

    /** 要求已登录，否则抛 401。 */
    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) throw new com.shiguang.lostfound.common.BusinessException(401, "请先登录");
        return id;
    }

    /** 要求管理员身份，否则抛 403。 */
    public static Long requireAdmin() {
        Long id = requireUserId();
        if (!isAdmin()) throw new com.shiguang.lostfound.common.BusinessException(403, "需要管理员权限");
        return id;
    }
}
