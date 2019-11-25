package com.cach.domain;

/**
 * @author zhao tailen
 * @description 缓存驱逐策略
 * @date 2019-11-05
 */
public enum CacheEvictionPolicy {
    //统计元素的使用频率，淘汰用得最少（最不常用）的。
    LFU,
    // 按元素使用时间排序比较，淘汰最早（最久远）的。
    LRU,
    // 元素用Java的WeakReference来保存，缓存元素通过GC过程清除。
    SOFT,
    // 元素用Java的SoftReference来保存, 缓存元素通过GC过程清除。
    WEAK,
    // 永不淘汰清除缓存元素。
    NONE;
}
