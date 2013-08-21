package com.olfa.b2b.test;

import com.olfa.b2b.util.AmountConverter;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class AmountConverterTest {
    @Test
    public void testThousands() {
        assertEquals("100k is 100000", 100000, AmountConverter.expand("100k"));
        assertEquals("100000 is 100k", "100k", AmountConverter.contract(100000));
        assertEquals("3500k is 3500000", 3500000, AmountConverter.expand("3500k"));
        assertEquals("3500000 is 3500k", "3500k", AmountConverter.contract(3500000));
    }

    @Test
    public void testMillions() {
        assertEquals("3m is 3000000", 3000000, AmountConverter.expand("3m"));
        assertEquals("3000000 is 3m", "3m", AmountConverter.contract(3000000));
    }

    @Test
    public void testUnits() {
        assertEquals("3145 is 3415", 3415, AmountConverter.expand("3415"));
        assertEquals("5226200 is 5226200", "5226200", AmountConverter.contract(5226200));
    }
}
