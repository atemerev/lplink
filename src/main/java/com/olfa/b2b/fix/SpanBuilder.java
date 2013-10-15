package com.olfa.b2b.fix;

import java.util.*;

public class SpanBuilder {

    private final LinkedHashMap<Integer, FixTag> tags = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, FixGroup> groups = new LinkedHashMap<>();
    private GroupBuilder currentBuilder;
    private final FixDictionary dictionary;

    public SpanBuilder(FixDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public boolean onTag(FixTag tag) {
        if (currentBuilder != null) {
            FixGroup groupOrNull = currentBuilder.onTag(tag);
            if (groupOrNull != null) {
                groups.put(groupOrNull.asTag().getNumber(), groupOrNull);
                tags.put(tag.getNumber(), tag);
                currentBuilder = null;
                return false;
            } else {
                return true;
            }
        } else {
            if (dictionary.isGroup(tag.getNumber())) {
                this.currentBuilder = new GroupBuilder(dictionary, tag);
                tags.put(tag.getNumber(), tag);
                return true;
            } else {
                tags.put(tag.getNumber(), tag);
                return false;
            }
        }
    }

    public FixSpan emit() {
        if (currentBuilder != null) {
            FixGroup group = currentBuilder.emit();
            groups.put(group.asTag().getNumber(), group);
            currentBuilder = null;
        }
        FixSpan result = new FixSpan(new LinkedHashMap<>(tags), new LinkedHashMap<>(groups));
        tags.clear();
        groups.clear();
        return result;
    }

    public boolean isDirty() {
        return !(tags.isEmpty() && groups.isEmpty());
    }
}
