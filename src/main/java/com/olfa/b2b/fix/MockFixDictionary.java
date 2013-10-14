package com.olfa.b2b.fix;

public class MockFixDictionary extends FixDictionary {

    private static final int FIX_NO_COMP_IDS = 936;
    private static final int FIX_NO_SUBSCRIPTIONS = 11201;

    private static final int[] FIX_NO_COMP_IDS_GROUP = new int[]{930, 928, 11200, 929, 11201};
    private static final int[] FIX_NO_SUBSCRIPTIONS_GROUP = new int[]{55, 53, 11202, 928, 11200};
    private static final int[] EMPTY = new int[0];

    @Override
    public int[] getAllowedTags(int groupTag) {
        switch (groupTag) {
            case FIX_NO_COMP_IDS:
                return FIX_NO_COMP_IDS_GROUP;
            case FIX_NO_SUBSCRIPTIONS:
                return FIX_NO_SUBSCRIPTIONS_GROUP;
            default:
                return EMPTY;
        }
    }
}
