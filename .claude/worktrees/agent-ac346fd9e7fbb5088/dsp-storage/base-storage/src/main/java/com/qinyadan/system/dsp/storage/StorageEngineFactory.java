package com.qinyadan.system.dsp.storage;

import java.util.Iterator;
import java.util.ServiceLoader;

public class StorageEngineFactory {

    public static StorageEngine getStorageEngine() {
        ServiceLoader loader = ServiceLoader.load(StorageEngine.class);
        Iterator<StorageEngine> iterator = loader.iterator();
        while (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

}
