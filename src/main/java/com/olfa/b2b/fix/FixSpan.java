package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FixSpan implements ByteSerializable {

    private final LinkedHashMap<Integer, FixTag> tags;
    private final Map<Integer, FixGroup> groups;

    public FixSpan(FixElement... contents) {
        final LinkedHashMap<Integer, FixTag> tagMap = new LinkedHashMap<>();
        final Map<Integer, FixGroup> groupMap = new HashMap<>();
        for (FixElement element : contents) {
            if (element != null) {
                tagMap.put(element.asTag().getNumber(), element.asTag());
                if (element.isGroup()) {
                    groupMap.put(element.asTag().getNumber(), (FixGroup) element);
                }
            }
        }
        this.tags = tagMap;
        this.groups = groupMap;
    }

    public FixSpan(@NotNull LinkedHashMap<Integer, FixTag> tags, @NotNull Map<Integer, FixGroup> groups) {
        this.tags = tags;
        this.groups = groups;
    }

    @NotNull
    public FixTag getTag(Integer number) throws NotFoundException {
        final FixTag tag = tags.get(number);
        if (tag != null) {
            return tag;
        } else {
            throw new NotFoundException("Tag not found: " + number);
        }
    }

    @NotNull
    public FixGroup getGroup(Integer number) throws NotFoundException {
        final FixGroup group = groups.get(number);
        if (group != null) {
            return group;
        } else {
            throw new NotFoundException("Group not found: " + number);
        }
    }

    public byte[] encode() {
        return toString(String.valueOf((char) ByteSerializable.FIELD_DIVIDER)).getBytes();
    }

    @Override
    public String toString() {
        return toString(" | ");
    }

    public String toString(String divider) {
        StringBuilder builder = new StringBuilder();
        for (FixTag tag : tags.values()) {
            builder.append(tag.toString());
            builder.append(divider);
            if (groups.containsKey(tag.getNumber())) {
                builder.append(groups.get(tag.getNumber()).toString(divider));
            }
        }
        return builder.toString();
    }
}