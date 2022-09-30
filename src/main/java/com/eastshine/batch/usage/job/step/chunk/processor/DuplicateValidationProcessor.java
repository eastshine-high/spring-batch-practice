package com.eastshine.batch.usage.job.step.chunk.processor;

import org.springframework.batch.item.ItemProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DuplicateValidationProcessor<T> implements ItemProcessor<T, T> {

    private final Map<String, Object> keyPool = new ConcurrentHashMap<>();
    private final Function<T, String> keyExtractor;
    private final boolean allowDuplicate;

    public DuplicateValidationProcessor(Function<T, String> keyExtractor) {
        this(keyExtractor, true);
    }

    public DuplicateValidationProcessor(Function<T, String> keyExtractor,
                                        boolean allowDuplicate) {

        this.keyExtractor = keyExtractor;
        this.allowDuplicate = allowDuplicate;
    }

    @Override
    public T process(T item) throws Exception {
        if (allowDuplicate) {
            return item;
        }

        String key = keyExtractor.apply(item);

        if (keyPool.containsKey(key)) {
            return null;
        }

        keyPool.put(key, key);
        return item;
    }
}
