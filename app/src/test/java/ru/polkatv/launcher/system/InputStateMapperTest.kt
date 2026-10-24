package ru.polkatv.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Test

class InputStateMapperTest {
    // TvInputManager constants: INPUT_STATE_CONNECTED=0, INPUT_STATE_CONNECTED_STANDBY=1, INPUT_STATE_DISCONNECTED=2
    @Test fun `maps framework states`() {
        assertEquals(InputState.CONNECTED, InputStateMapper.map(0))
        assertEquals(InputState.NO_SIGNAL, InputStateMapper.map(1))
        assertEquals(InputState.DISCONNECTED, InputStateMapper.map(2))
        assertEquals(InputState.UNKNOWN, InputStateMapper.map(99))
    }
}
