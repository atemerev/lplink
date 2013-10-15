package com.olfa.b2b.fix;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GroupBuilder {

    private final FixDictionary dictionary;
    private final FixTag groupStartTag;
    private final int groupDividerTag;
    private final Set<Integer> groupAllowedTags;
    private final ArrayList<FixSpan> spans;
    private SpanBuilder currentSpan;
    private boolean inNestedGroup = false;

    public GroupBuilder(FixDictionary dictionary, FixTag groupTag) {
        this.dictionary = dictionary;
        this.groupStartTag = groupTag;
        int[] allowedTags = dictionary.getAllowedTags(groupTag.number);
        assert allowedTags.length > 0;
        this.groupDividerTag = allowedTags[0];
        this.groupAllowedTags = new HashSet<>();
        for (Integer num : allowedTags) {
            groupAllowedTags.add(num);
        }
        this.spans = new ArrayList<>(groupTag.getInt());
        this.currentSpan = null;
    }

    public @Nullable FixGroup onTag(FixTag tag) {
        if (tag.number == groupDividerTag) {
            if (currentSpan != null) {
                spans.add(currentSpan.emit());
            }
            currentSpan = new SpanBuilder(dictionary);
        }
        if (groupAllowedTags.contains(tag.number) || inNestedGroup) {
            this.inNestedGroup = currentSpan.onTag(tag);
            return null;
        } else {
            this.inNestedGroup = false;
            spans.add(currentSpan.emit());
            currentSpan = null;
            return new FixGroup(groupStartTag, spans);
        }
    }

    public FixGroup emit() {
        if (currentSpan.isDirty()) {
            spans.add(currentSpan.emit());
        }
        return new FixGroup(groupStartTag, spans);
    }
}
