package dev.huskuraft.effortless.vanilla.core;

import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import dev.huskuraft.effortless.api.core.Registry;
import dev.huskuraft.effortless.api.platform.PlatformReference;

public record MinecraftRegistry<T extends PlatformReference, R>(
        net.minecraft.core.Registry<R> referenceValue,
        Function<R, T> typeConvertor
) implements Registry<T> {

    @Override
    public int getId(T value) {
        return referenceValue().getId(value.reference());
    }

    @Nullable
    @Override
    public T byId(int key) {
        return typeConvertor().apply(referenceValue().byId(key));
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.transform(referenceValue().iterator(), typeConvertor());
    }
}
