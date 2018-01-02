package de.schildbach.wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexk on 12/21/17.
 */

public class ScriptNum {
    private final long value;

    public ScriptNum(long value) {
        this.value = value;
    }

    public byte[] getvch() {
        return serialize(value);
    }

    private static byte[] serialize(long value) {
        if (value == 0)
            return new byte[]{};

        List<Byte> result = new ArrayList<>();
        final boolean neg = value < 0;
        long absvalue = neg ? -value : value;

        while (absvalue > 0) {
            final byte b = (byte) (absvalue & 0xff);
            result.add(b);
            absvalue >>= 8;
        }
        final byte last = result.get(result.size() - 1);
        if ((last & 0x80) != 0) {
            byte b = (byte) (neg ? 0x80 : 0);
            result.add(b);
        } else if (neg) {
            byte b = result.get(result.size() - 1);
            b |= 0x80;
            result.set(result.size() - 1, b);
        }

        byte[] out = new byte[result.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = result.get(i);
        }
        return out;
    }
}
