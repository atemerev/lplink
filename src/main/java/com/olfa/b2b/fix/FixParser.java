package com.olfa.b2b.fix;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.olfa.b2b.fix.FixParser.ByteState.*;

public class FixParser {

    private ByteState state = GARBAGE;
    private final ByteBuffer tagNumBuffer = ByteBuffer.allocate(10);
    private final ByteBuffer valueBuffer = ByteBuffer.allocate(65535);
    private final List<FixSpan> messages = new ArrayList<>();

    public List<FixSpan> onData(byte[] bytes) {
        for (byte b : bytes) {
            switch (state) {
                case GARBAGE:
                    if (isNumber(b)) {
                        tagNumBuffer.put(b);
                        state = TAGNUM;
                    }
                    break;
                case TAGNUM:
                    if (isNumber(b)) {
                        tagNumBuffer.put(b);
                    } else if (isEqualsSign(b)) {
                        tagNumBuffer.flip();
                        state = EQUALS;
                    } else {
                        fault();
                    }
                    break;
                case EQUALS:
                    if (isSplit(b)) {
                        fault();
                    } else {
                        valueBuffer.put(b);
                        state = VALUE;
                    }
                    break;
                case VALUE:
                    if (isEqualsSign(b)) {
                        valueBuffer.flip();
                        emitTag();
                        state = SPLIT;
                    } else {
                        valueBuffer.put(b);
                    }
                    break;
                case SPLIT:
                    if (isNumber(b)) {
                        tagNumBuffer.put(b);
                        state = TAGNUM;
                    } else {
                        // todo add double split for FIXLIKE
                        fault();
                    }
                    break;
            }
        }
        return messages;
    }

    public void onTag(FixTag tag) {
    }

    private void emitTag() {
        int tagNum = tagNumBuffer.getInt();
        byte[] bytes = new byte[valueBuffer.remaining()];
        valueBuffer.get(bytes);
        String value = new String(bytes);
        FixTag tag = new FixTag(tagNum, value);
        tagNumBuffer.clear();
        valueBuffer.clear();
        onTag(tag);
    }

    private void fault() {
        tagNumBuffer.clear();
        valueBuffer.clear();
        state = GARBAGE;
    }

    private boolean isNumber(byte b) {
        return b >= 0x30 && b <= 0x39;
    }

    private boolean isEqualsSign(byte b) {
        return b == 0x3D;
    }

    private boolean isSplit(byte b) {
        return b == 0x01;
    }

    enum ByteState {
        GARBAGE, TAGNUM, EQUALS, VALUE, SPLIT
    }
}
