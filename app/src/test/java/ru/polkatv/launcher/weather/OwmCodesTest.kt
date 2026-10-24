package ru.polkatv.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class OwmCodesTest {
    @Test fun `owm ids map to wmo codes`() {
        assertEquals(95, OwmCodes.toWmo(200)); assertEquals(95, OwmCodes.toWmo(211)); assertEquals(95, OwmCodes.toWmo(232))
        assertEquals(51, OwmCodes.toWmo(300)); assertEquals(53, OwmCodes.toWmo(301)); assertEquals(55, OwmCodes.toWmo(302)); assertEquals(53, OwmCodes.toWmo(321))
        assertEquals(61, OwmCodes.toWmo(500)); assertEquals(63, OwmCodes.toWmo(501)); assertEquals(65, OwmCodes.toWmo(502)); assertEquals(65, OwmCodes.toWmo(504))
        assertEquals(66, OwmCodes.toWmo(511))
        assertEquals(80, OwmCodes.toWmo(520)); assertEquals(81, OwmCodes.toWmo(521)); assertEquals(82, OwmCodes.toWmo(522)); assertEquals(82, OwmCodes.toWmo(531))
        assertEquals(71, OwmCodes.toWmo(600)); assertEquals(73, OwmCodes.toWmo(601)); assertEquals(75, OwmCodes.toWmo(602))
        assertEquals(77, OwmCodes.toWmo(611)); assertEquals(77, OwmCodes.toWmo(613))
        assertEquals(85, OwmCodes.toWmo(615)); assertEquals(85, OwmCodes.toWmo(620)); assertEquals(86, OwmCodes.toWmo(621)); assertEquals(86, OwmCodes.toWmo(622))
        assertEquals(45, OwmCodes.toWmo(701)); assertEquals(45, OwmCodes.toWmo(741)); assertEquals(45, OwmCodes.toWmo(761))
        assertEquals(82, OwmCodes.toWmo(771)); assertEquals(95, OwmCodes.toWmo(781))
        assertEquals(0, OwmCodes.toWmo(800)); assertEquals(1, OwmCodes.toWmo(801)); assertEquals(2, OwmCodes.toWmo(802)); assertEquals(2, OwmCodes.toWmo(803)); assertEquals(3, OwmCodes.toWmo(804))
        assertEquals(3, OwmCodes.toWmo(999))
    }
}
