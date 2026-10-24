package ru.polkatv.launcher.system

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.util.Log

enum class InputState { CONNECTED, NO_SIGNAL, DISCONNECTED, UNKNOWN }

data class TvInput(val id: String, val label: String, val state: InputState)

object InputStateMapper {
    fun map(tvInputManagerState: Int): InputState = when (tvInputManagerState) {
        TvInputManager.INPUT_STATE_CONNECTED -> InputState.CONNECTED            // 0
        TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> InputState.NO_SIGNAL    // 1
        TvInputManager.INPUT_STATE_DISCONNECTED -> InputState.DISCONNECTED      // 2
        else -> InputState.UNKNOWN
    }
}

class InputsController(private val context: Context, private val actions: SystemActions) {
    private val tim = context.getSystemService(TvInputManager::class.java)

    fun list(): List<TvInput> {
        val infos: List<TvInputInfo> = runCatching { tim.tvInputList }.getOrDefault(emptyList())
        return infos.filter { it.isPassthroughInput }
            .map { info ->
                val label = info.loadLabel(context)?.toString()?.takeIf { it.isNotBlank() } ?: info.id.substringAfterLast('/')
                TvInput(info.id, label, InputStateMapper.map(runCatching { tim.getInputState(info.id) }.getOrDefault(-1)))
            }
            .sortedBy { it.label }
    }

    /**
     * Strategy order (spec §6.2): 1) passthrough URI to Live TV, 2) passthrough URI to any handler, 3) plain Live TV launch.
     * Once the working strategy is confirmed on the device, keep the others as fallbacks.
     *
     * Verified on the Xiaomi MiTV-MOEU1 (Android 14): strategy 1 (passthrough URI to com.mitv.livetv) logs success;
     * Live TV (com.mitv.livetv/.tv.MainActivity) comes to the top and the TV input session is on the picked HDMI input.
     * Strategies 2 and 3 remain as fallbacks and were not needed.
     */
    fun switchTo(input: TvInput): Boolean {
        val uri = TvContract.buildChannelUriForPassthroughInput(input.id)
        val toLiveTv = Intent(Intent.ACTION_VIEW, uri).setPackage("com.mitv.livetv")
        if (actions.launch(toLiveTv)) { Log.i("Polka", "input switch via Live TV passthrough"); return true }
        val any = Intent(Intent.ACTION_VIEW, uri)
        if (actions.launch(any)) { Log.i("Polka", "input switch via generic passthrough"); return true }
        Log.i("Polka", "input switch fallback: launching Live TV")
        return actions.openLiveTv()
    }
}
