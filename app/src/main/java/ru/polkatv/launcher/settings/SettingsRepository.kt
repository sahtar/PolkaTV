package ru.polkatv.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.polkaDataStore: DataStore<Preferences> by preferencesDataStore(name = "polka_prefs")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("prefs_json")

    val prefs: Flow<Prefs> = dataStore.data
        .map { PrefsCodec.decode(it[key]) }
        .catch { emit(Prefs()) }

    suspend fun update(transform: (Prefs) -> Prefs) {
        dataStore.edit { it[key] = PrefsCodec.encode(transform(PrefsCodec.decode(it[key]))) }
    }
}
