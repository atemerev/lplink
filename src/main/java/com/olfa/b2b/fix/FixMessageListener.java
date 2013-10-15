package com.olfa.b2b.fix;

import java.util.List;

public interface FixMessageListener {
    List<FixSpan> onFixMessage(FixSpan message);
}
