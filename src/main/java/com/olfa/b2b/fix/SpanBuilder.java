package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                groups.put(groupOrNull.getNumber(), groupOrNull);
                tags.put(tag.number, tag);
                currentBuilder = null;
                return false;
            } else {
                return true;
            }
        } else {
            if (dictionary.isGroup(tag.number)) {
                this.currentBuilder = new GroupBuilder(dictionary, tag);
                return true;
            } else {
                tags.put(tag.number, tag);
                return false;
            }
        }
    }

    public FixSpan emit() {
        if (currentBuilder != null) {
            FixGroup group = currentBuilder.emit();
            groups.put(group.getNumber(), group);
            currentBuilder = null;
        }
        return new FixSpan(tags, groups);
    }
}
