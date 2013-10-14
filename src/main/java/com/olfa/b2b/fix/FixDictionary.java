package com.olfa.b2b.fix;

public abstract class FixDictionary {

    /**
     * Check for whether a specific tags marks the beginning of a FIX repeating group.
     *
     * @param groupTag Tag number to check.
     * @return If this tag is not group tag, returns empty array. If it is a group tag, returns array of allowed group
     *         members (tag numbers). The first number of this array is a group divider.
     */
    public abstract int[] getAllowedTags(int groupTag);

    public boolean isGroup(int tag) {
        return getAllowedTags(tag).length > 0;
    }
}
