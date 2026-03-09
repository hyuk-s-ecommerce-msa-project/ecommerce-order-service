package com.ecommerce.order_service.config;

public class ShardContextHolder {
    private static final ThreadLocal<Integer> CONTEXT = new ThreadLocal<>();

    public static void setShardIndex(Integer shardIndex) {
        CONTEXT.set(shardIndex);
    }

    public static Integer getShardIndex() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
