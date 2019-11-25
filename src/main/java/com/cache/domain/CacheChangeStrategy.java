package com.cache.domain;

/**
 * @author zhao tailen
 * @description 当设置了两级缓存的时候，指定不同级缓存之间的切换策略
 * @date 2019-11-14
 */
public enum CacheChangeStrategy {

    /**
     * 突破访问次数阀值【先远程后本地】
     * */
    ACCESS_THRESHOLD,
    /**
     * 空间溢出【先本地后远程】
     * */
    OVERFLOW_MAX_SIZE;
}
