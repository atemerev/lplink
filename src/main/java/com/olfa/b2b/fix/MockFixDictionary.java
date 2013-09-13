package com.olfa.b2b.fix;

public class MockFixDictionary implements FixDictionary {

    private static final int FIX_NO_COMP_IDS = 936;
    private static final int FIX_NO_SUBSCRIPTIONS = 11201;

    private static final int[] empty = new int[0];
    private static final int[] noCompIdsGroup = new int[]{930, 928, 11200, 929, FIX_NO_SUBSCRIPTIONS};
    private static final int[] noSubscriptionsGroup = new int[]{55, 53, 11202, 928, 11200};

    @Override
    public int[] getAllowedTags(int groupTag) {
        switch (groupTag) {
            case FIX_NO_COMP_IDS:
                return noCompIdsGroup;
            case FIX_NO_SUBSCRIPTIONS:
                return noSubscriptionsGroup;
            default:
                return empty;
        }
    }
}
