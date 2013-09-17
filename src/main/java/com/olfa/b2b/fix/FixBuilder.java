package com.olfa.b2b.fix;

import com.olfa.b2b.exception.NotImplementedException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FixBuilder {

    private final LinkedHashMap<Integer, FixTag> tags = new LinkedHashMap<>();
    private final List<FixBuilder> groups = new ArrayList<>();
    private final FixDictionary dictionary;
    private FixBuilder nextBuilder = null;

    public FixBuilder(FixDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void addTag(FixTag tag) {
        int[] groupTags = dictionary.getAllowedTags(tag.number);
        if (nextBuilder == null) {
            tags.put(tag.number, tag);
            if (groupTags.length > 0) {
                this.nextBuilder = new FixBuilder(dictionary);
            }
        } else {
            nextBuilder.addTag(tag);
            if (isEndGroup(groupTags, tag)) {
                groups.add(nextBuilder);
                nextBuilder = null;
            }
        }
    }


    public FixSpan emit() {
        // todo implement
        throw new NotImplementedException();
    }

    private boolean isEndGroup(int[] groupTags, FixTag tag) {
        // todo implement
        throw new NotImplementedException();
    }
}
