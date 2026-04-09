package com.eaglepoint.workforce.masking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilTest {

    @Test
    void masksLongString() {
        assertEquals("*****6789", MaskingUtil.maskToLastFour("123456789"));
    }

    @Test
    void masksExactlyFiveCharString() {
        assertEquals("*2345", MaskingUtil.maskToLastFour("12345"));
    }

    @Test
    void returnsFourCharStringUnchanged() {
        assertEquals("1234", MaskingUtil.maskToLastFour("1234"));
    }

    @Test
    void returnsShortStringUnchanged() {
        assertEquals("ab", MaskingUtil.maskToLastFour("ab"));
    }

    @Test
    void returnsNullForNull() {
        assertNull(MaskingUtil.maskToLastFour(null));
    }

    @Test
    void returnsEmptyForEmpty() {
        assertEquals("", MaskingUtil.maskToLastFour(""));
    }

    @Test
    void masksSsnFormat() {
        assertEquals("*****6789", MaskingUtil.maskToLastFour("123456789"));
    }
}
