/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.hazelcast;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import io.micronaut.cache.AsyncCache;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArgumentUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A {@link AsyncCache} implementation based on Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
public class HazelcastAsyncCache implements AsyncCache<IMap<Object, Object>> {

    private final ConversionService<?> conversionService;
    private final IMap<Object, Object> nativeCache;
    private final ExecutorService executorService;

    /**
     * @param conversionService the conversion service
     * @param nativeCache the native cache
     * @param executorService managers the pool of executors
     */
    public HazelcastAsyncCache(ConversionService<?> conversionService,
                               IMap<Object, Object> nativeCache,
                               ExecutorService executorService) {
        this.conversionService = conversionService;
        this.nativeCache = nativeCache;
        this.executorService = executorService;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> get(@Nonnull Object key, @Nonnull Argument<T> requiredType) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        nativeCache.getAsync(key).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                if (response != null) {
                    future.complete(conversionService.convert(response, ConversionContext.of(requiredType)));
                } else {
                    future.complete(Optional.empty());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        }, executorService);
        return future;
    }

    @Override
    public <T> CompletableFuture<T> get(@Nonnull Object key, @Nonnull Argument<T> requiredType, @Nonnull Supplier<T> supplier) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Optional<T>> optionalCompletableFuture = get(key, requiredType);
        return optionalCompletableFuture.thenApply(existingValue -> {
            if (existingValue.isPresent()) {
                return existingValue.get();
            } else {
                T value = supplier.get();
                put(key, value);
                return value;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> CompletableFuture<Optional<T>> putIfAbsent(@Nonnull Object key, @Nonnull T value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        nativeCache.submitToKey(key, new AbstractEntryProcessor()  {
            @Override
            public Object process(Map.Entry entry) {
                Object remoteValue = entry.getValue();
                if (remoteValue == null) {
                    entry.setValue(value);
                    future.complete(Optional.empty());
                } else {
                    final Class<T> aClass = (Class<T>) value.getClass();
                    future.complete(conversionService.convert(remoteValue, aClass));
                }
                return value;
            }
        });
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> put(@Nonnull Object key, @Nonnull Object value) {
        ArgumentUtils.requireNonNull("key", key);
        ArgumentUtils.requireNonNull("value", value);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        nativeCache.setAsync(key, value).andThen(new ExecutionCallback<Void>() {
            @Override
            public void onResponse(Void response) {
                future.complete(true);
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        }, executorService);
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> invalidate(@Nonnull Object key) {
        ArgumentUtils.requireNonNull("key", key);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        nativeCache.removeAsync(key).andThen(new ExecutionCallback<Object>() {
            @Override
            public void onResponse(Object response) {
                future.complete(true);
            }

            @Override
            public void onFailure(Throwable t) {
                future.completeExceptionally(t);
            }
        }, executorService);
        return future;
    }

    @Override
    public CompletableFuture<Boolean> invalidateAll() {
        return CompletableFuture.supplyAsync(() -> {
            nativeCache.clear();
            return true;
        }, executorService);
    }

    @Override
    public String getName() {
        return nativeCache.getName();
    }

    @Override
    public IMap getNativeCache() {
        return nativeCache;
    }
}
