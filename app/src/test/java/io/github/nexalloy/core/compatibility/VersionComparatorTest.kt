package io.github.nexalloy.core.compatibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {

    @Test
    fun numericSegmentOrdering() {
        // Multi-segment numeric comparisons (not alphabetical)
        assertTrue(VersionComparator.compare("21.41.525", "21.20.405") > 0)
        assertTrue(VersionComparator.compare("21.20.405", "21.16.256") > 0)
        assertTrue(VersionComparator.compare("21.16.256", "21.20.405") < 0)

        // Segment length doesn't break numeric priority (10 > 9)
        assertTrue(VersionComparator.compare("1.10.0", "1.9.0") > 0)
        assertTrue(VersionComparator.compare("10.0.0", "9.0.0") > 0)
        assertTrue(VersionComparator.compare("447.0.0.55.81", "447.0.0.55.80") > 0)
    }

    @Test
    fun trailingZeroEquivalence() {
        assertEquals(0, VersionComparator.compare("20.22.00", "20.22.0"))
        assertEquals(0, VersionComparator.compare("20.22.0", "20.22"))
        assertEquals(0, VersionComparator.compare("1.0.0.0", "1.0"))
        assertEquals(0, VersionComparator.compare("1.0", "1"))
        assertTrue(VersionComparator.isEqual("20.22.0", "20.22"))
    }

    @Test
    fun prefixAndWhitespaceHandling() {
        assertEquals(0, VersionComparator.compare("v21.41.525", "21.41.525"))
        assertEquals(0, VersionComparator.compare("V21.41.525", "21.41.525"))
        assertEquals(0, VersionComparator.compare(" 21.41.525 ", "21.41.525"))
        assertTrue(VersionComparator.isEqual("v1.2.3", "1.2.3"))
    }

    @Test
    fun prereleaseAndSuffixHandling() {
        // Release without suffix is considered newer than prerelease with suffix
        assertTrue(VersionComparator.compare("1.0.0", "1.0.0-alpha") > 0)
        assertTrue(VersionComparator.compare("1.0.0-alpha", "1.0.0") < 0)
        assertTrue(VersionComparator.compare("1.0.0-beta", "1.0.0-alpha") > 0)
    }

    @Test
    fun nullHandling() {
        assertEquals(0, VersionComparator.compare(null, null))
        assertTrue(VersionComparator.compare("1.0", null) > 0)
        assertTrue(VersionComparator.compare(null, "1.0") < 0)
    }

    @Test
    fun convenienceMethods() {
        assertTrue(VersionComparator.isAtLeast("21.41.525", "21.20.405"))
        assertTrue(VersionComparator.isAtLeast("21.41.525", "21.41.525"))
        assertFalse(VersionComparator.isAtLeast("21.20.405", "21.41.525"))

        assertTrue(VersionComparator.isAtMost("21.20.405", "21.41.525"))
        assertTrue(VersionComparator.isAtMost("21.20.405", "21.20.405"))
        assertFalse(VersionComparator.isAtMost("21.41.525", "21.20.405"))

        assertTrue(VersionComparator.isEqual("1.2.0", "1.2"))
        assertFalse(VersionComparator.isEqual("1.2.1", "1.2"))
    }
}

