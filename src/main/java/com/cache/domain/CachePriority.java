package com.cache.domain;

/**
 * @author zhao tailen
 * @description 当使用两级缓存的时候需要指定的缓存优先级
 * @date 2019-11-14
 */
public enum CachePriority {
    /**
     * 只存本地
     */
    ONLY_LOCAL,
    /**
     * 只存远程
     */
    ONLY_REMOTE,
    /**
     * 先本地后远程
     */
    FIRST_LOCAL,
    /**
     * 先远程后本地
     */
    FIRST_REMOTE,
    /**
     * 本地和远程同时设置
     */
    LOCAL_REMOTE;
}
