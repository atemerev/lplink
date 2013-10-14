package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FixSpan {

    private final LinkedHashMap<Integer, FixTag> tags;
    private final Map<Integer, FixGroup> groups;

    public FixSpan(@NotNull List<FixTag> tags, @NotNull List<FixGroup> groups) {
        final LinkedHashMap<Integer, FixTag> tagMap = new LinkedHashMap<>();
        final Map<Integer, FixGroup> groupMap = new HashMap<>();
        for (FixTag tag : tags) {
            tagMap.put(tag.number, tag);
        }
        for (FixGroup group : groups) {
            final int groupNum = group.getGroupTag().number;
            if (tagMap.containsKey(groupNum)) {
                groupMap.put(groupNum, group);
            } else {
                throw new IllegalArgumentException("Group tag is not defined in tags list");
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
        // todo implement
        return new byte[0];
    }
}