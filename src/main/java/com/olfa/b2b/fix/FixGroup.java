package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FixGroup implements FixElement, ByteSerializable {

    private final FixTag groupTag;
    private final List<FixSpan> spans;

    public FixGroup(int groupTagNum, List<FixSpan> spans) {
        this.groupTag = new FixTag(groupTagNum, spans.size());
        this.spans = Collections.unmodifiableList(spans);
    }

    public FixGroup(int groupTagNum, FixSpan... spans) {
        this.groupTag = new FixTag(groupTagNum, spans.length);
        this.spans = Collections.unmodifiableList(Arrays.asList(spans));
    }

    @Override
    public FixTag asTag() {
        return groupTag;
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

    @Override
    public String toString() {
        return toString(" | ");
    }

    public String toString(String divider) {
        StringBuilder builder = new StringBuilder();
        for (FixSpan span : spans) {
            builder.append(span.toString(divider));
        }
        return builder.toString();
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    public byte[] encode() {
        return toString(String.valueOf((char) ByteSerializable.FIELD_DIVIDER)).getBytes();
    }
}
