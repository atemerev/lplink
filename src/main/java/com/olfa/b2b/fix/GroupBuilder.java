package com.olfa.b2b.fix;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class GroupBuilder {

    private final FixDictionary dictionary;
    private final FixTag groupStartTag;
    private final int groupDividerTag;
    private final int[] groupAllowedTags;
    private int groupTagIndex = 0;
    private final ArrayList<FixSpan> spans;
    private SpanBuilder currentSpan;
    private boolean dirty = false;

    public GroupBuilder(FixDictionary dictionary, FixTag groupTag) {
        this.dictionary = dictionary;
        this.groupStartTag = groupTag;
        this.groupAllowedTags = dictionary.getAllowedTags(groupTag.getInt());
        assert groupAllowedTags.length > 0;
        this.groupDividerTag = groupAllowedTags[0];
        this.spans = new ArrayList<>(groupTag.getInt());
        this.currentSpan = new SpanBuilder(dictionary);
    }

    public @Nullable FixGroup onTag(FixTag tag) {
        if (tag.number == groupDividerTag) {
            if (currentSpan != null) {
                spans.add(currentSpan.emit());
            }
            currentSpan = new SpanBuilder(dictionary);
            dirty = false;
        }
        while (groupTagIndex < groupAllowedTags.length) {
            if (tag.number == groupAllowedTags[groupTagIndex]) {
                dirty = true;
                boolean inGroup = currentSpan.onTag(tag);
                if (!inGroup) {
                    groupTagIndex++;
                }
                break;
            } else {
                groupTagIndex++;
            }
        }
        if (groupTagIndex == groupAllowedTags.length) {
            return new FixGroup(groupStartTag, spans);
        } else {
            return null;
        }
    }

    public FixGroup emit() {
        if (dirty) {
            spans.add(currentSpan.emit());
            dirty = false;
        }
        return new FixGroup(groupStartTag, spans);
    }
}
