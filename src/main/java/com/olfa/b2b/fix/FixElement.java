package com.olfa.b2b.fix;

public interface FixElement {
    FixTag asTag();
    boolean isGroup();
}
