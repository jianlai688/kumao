package com.kumao.common.constant;

/**
 * 评论 / 直播弹幕的审核状态。
 *
 * <p>0 已通过并公开展示；1 已驳回/屏蔽；2 AI 不建议通过，等待管理员复核。</p>
 */
public final class ModerationStatus {

    public static final int APPROVED = 0;
    public static final int REJECTED = 1;
    public static final int PENDING = 2;

    private ModerationStatus() {
    }

    public static boolean isApproved(Integer status) {
        return status != null && status == APPROVED;
    }

    public static boolean isPending(Integer status) {
        return status != null && status == PENDING;
    }

    public static boolean isValid(Integer status) {
        return status != null
                && (status == APPROVED || status == REJECTED || status == PENDING);
    }
}
