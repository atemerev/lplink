package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Reject<REQ> {
    @NotNull public final REQ request;
    @Nullable public final String reason;

    public Reject(@NotNull REQ request, @Nullable String reason) {
        this.request = request;
        this.reason = reason;
    }
}
