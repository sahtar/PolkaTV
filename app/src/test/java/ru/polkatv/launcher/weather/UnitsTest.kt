package ru.polkatv.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test fun `hPa to mmHg rounds to integer`() {
        assertEquals(753, Units.hpaToMmHg(1003.4))
        assertEquals(760, Units.hpaToMmHg(1013.25))
    }

    @Test fun `wind degrees to eight sectors`() {
        assertEquals(WindDir.N, Units.windDir(0.0)); assertEquals(WindDir.N, Units.windDir(359.0)); assertEquals(WindDir.N, Units.windDir(22.4))
        assertEquals(WindDir.NE, Units.windDir(22.5)); assertEquals(WindDir.NE, Units.windDir(45.0))
        assertEquals(WindDir.E, Units.windDir(90.0)); assertEquals(WindDir.SE, Units.windDir(135.0))
        assertEquals(WindDir.S, Units.windDir(180.0)); assertEquals(WindDir.SW, Units.windDir(225.0))
        assertEquals(WindDir.W, Units.windDir(250.0)); assertEquals(WindDir.NW, Units.windDir(315.0))
    }
}
