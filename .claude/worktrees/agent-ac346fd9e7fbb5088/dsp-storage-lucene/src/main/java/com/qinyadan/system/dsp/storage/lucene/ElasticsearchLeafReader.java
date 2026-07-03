package com.qinyadan.system.dsp.storage.lucene;

import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.LeafReader;

/**
 * Wraps a leaf reader with shard-aware caching support, mirroring
 * the original ElasticsearchLeafReader from base-storage.
 */
public class ElasticsearchLeafReader extends FilterLeafReader {

    private final int shardId;

    public ElasticsearchLeafReader(LeafReader in, int shardId) {
        super(in);
        this.shardId = shardId;
    }

    @Override
    public CacheHelper getCoreCacheHelper() {
        return in.getCoreCacheHelper();
    }

    @Override
    public CacheHelper getReaderCacheHelper() {
        return in.getReaderCacheHelper();
    }
}
