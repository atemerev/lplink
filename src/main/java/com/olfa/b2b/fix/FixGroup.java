package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixGroup {
    private final FixTag groupTag;
    private final List<FixSpan> spans;

    public FixGroup(FixTag groupTag, ArrayList<FixSpan> spans) {
        this.groupTag = groupTag;
        FixSpan first = null;
        // todo validate group dividers
        this.spans = Collections.unmodifiableList(spans);
    }

    @NotNull
    public FixTag getGroupTag() {
        return groupTag;
    }

    public int getNumber() {
        return groupTag.number;
    }

    @NotNull
    public FixTag getTag(int tagNumber, int groupCount) throws NotFoundException {
        if (groupCount < spans.size()) {
            return spans.get(groupCount).getTag(tagNumber);
        } else {
            throw new NotFoundException(String.format("Group index %d requested, but there are only %d groups",
                    groupCount, spans.size()));
        }
    }
}
