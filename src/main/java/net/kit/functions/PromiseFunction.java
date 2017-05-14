package net.kit.functions;

import net.kit.promises.PromisePermit;
import org.jetbrains.annotations.NotNull;

public interface PromiseFunction<T> {
    void submit(@NotNull PromisePermit<T> resolver);
}
