package com.olfa.b2b.test;

import quickfix.FieldMap;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.Side;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

import java.lang.reflect.Method;
import java.util.Date;

public class QuickfixSerializationTest {

    static Method calculateString;

    public static void main(String[] args) throws Exception {

        calculateString = FieldMap.class.getDeclaredMethod("calculateString", StringBuffer.class, int[].class, int[].class);
        calculateString.setAccessible(true);

        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID("test"),
                new Side(Side.BUY),
                new TransactTime(new Date()),
                new OrdType(OrdType.MARKET)
        );
        StringBuffer out = new StringBuffer();
        calculateString.invoke(order, out, null, null);
        System.out.println(out.toString());
    }
}
