package com.olfa.b2b.fix;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.olfa.b2b.fix.FixParser.ByteState.*;

public class FixParser {

    private ByteState state = GARBAGE;
    private final ByteBuffer tagNumBuffer = ByteBuffer.allocate(10);
    private final ByteBuffer valueBuffer = ByteBuffer.allocate(65535);
    private final SpanBuilder builder = new SpanBuilder(new MockFixDictionary());
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
                        reset();
                    }
                    break;
                case EQUALS:
                    if (isSplit(b)) {
                        reset();
                    } else {
                        valueBuffer.put(b);
                        state = VALUE;
                    }
                    break;
                case VALUE:
                    if (isSplit(b)) {
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
                    } else if (isMessageSplit(b)) {
                        FixSpan span = builder.emit();
                        messages.add(span);
                        reset();
                    } else {
                        reset();
                    }
                    break;
            }
        }
        if (messages.isEmpty()) {
            //noinspection unchecked
            return Collections.EMPTY_LIST;
        } else {
            List<FixSpan> result = new ArrayList<>(messages);
            messages.clear();
            return result;
        }
    }

    public void onTag(FixTag tag) {
        builder.onTag(tag);
    }

    private void emitTag() {
        byte[] tagBytes = new byte[tagNumBuffer.remaining()];
        tagNumBuffer.get(tagBytes);
        int tagNum = Integer.parseInt(new String(tagBytes));
        byte[] valueBytes = new byte[valueBuffer.remaining()];
        valueBuffer.get(valueBytes);
        String value = new String(valueBytes);
        FixTag tag = new FixTag(tagNum, value);
        tagNumBuffer.clear();
        valueBuffer.clear();
        onTag(tag);
    }

    private void reset() {
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
        return b == '|';
    }

    private boolean isMessageSplit(byte b) {
        return b == ' ';
    }

    enum ByteState {
        GARBAGE, TAGNUM, EQUALS, VALUE, SPLIT
    }
}
