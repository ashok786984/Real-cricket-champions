package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CareerProfileLocal
import com.example.data.model.GraphicsQuality
import com.example.data.model.TimeOfDay
import com.example.data.model.WeatherCondition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "real_cricket_prefs")

data class AppSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val hapticsEnabled: Boolean = true,
    val commentarySoundEnabled: Boolean = true,
    val defaultTimeOfDay: TimeOfDay = TimeOfDay.NIGHT,
    val defaultWeather: WeatherCondition = WeatherCondition.CLEAR,
    val preferredTeamId: String = "ind_tigers"
)

class LocalPreferencesRepository(private val context: Context) {

    private object Keys {
        val GRAPHICS = stringPreferencesKey("graphics_quality")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val TIME_OF_DAY = stringPreferencesKey("default_time_of_day")
        val WEATHER = stringPreferencesKey("default_weather")
        val TEAM_ID = stringPreferencesKey("preferred_team_id")

        val CAREER_NAME = stringPreferencesKey("career_name")
        val CAREER_LEVEL = intPreferencesKey("career_level")
        val CAREER_XP = intPreferencesKey("career_xp")
        val CAREER_SP = intPreferencesKey("career_sp")
        val CAREER_BAT = intPreferencesKey("career_bat")
        val CAREER_BOWL = intPreferencesKey("career_bowl")
        val CAREER_FIELD = intPreferencesKey("career_field")
        val CAREER_MATCHES = intPreferencesKey("career_matches")
        val CAREER_WINS = intPreferencesKey("career_wins")
        val CAREER_RUNS = intPreferencesKey("career_runs")
        val CAREER_WICKETS = intPreferencesKey("career_wickets")
        val CAREER_HIGH = intPreferencesKey("career_high")
        val CAREER_SIXES = intPreferencesKey("career_sixes")
        val CAREER_FOURS = intPreferencesKey("career_fours")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            graphicsQuality = enumValues<GraphicsQuality>().firstOrNull { it.name == prefs[Keys.GRAPHICS] }
                ?: GraphicsQuality.HIGH,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            commentarySoundEnabled = prefs[Keys.SOUND] ?: true,
            defaultTimeOfDay = enumValues<TimeOfDay>().firstOrNull { it.name == prefs[Keys.TIME_OF_DAY] }
                ?: TimeOfDay.NIGHT,
            defaultWeather = enumValues<WeatherCondition>().firstOrNull { it.name == prefs[Keys.WEATHER] }
                ?: WeatherCondition.CLEAR,
            preferredTeamId = prefs[Keys.TEAM_ID] ?: "ind_tigers"
        )
    }

    val careerFlow: Flow<CareerProfileLocal> = context.dataStore.data.map { prefs ->
        CareerProfileLocal(
            playerName = prefs[Keys.CAREER_NAME] ?: "Aarav Sharma",
            level = prefs[Keys.CAREER_LEVEL] ?: 1,
            xp = prefs[Keys.CAREER_XP] ?: 0,
            skillPoints = prefs[Keys.CAREER_SP] ?: 3,
            battingRating = prefs[Keys.CAREER_BAT] ?: 72,
            bowlingRating = prefs[Keys.CAREER_BOWL] ?: 68,
            fieldingRating = prefs[Keys.CAREER_FIELD] ?: 74,
            matchesPlayed = prefs[Keys.CAREER_MATCHES] ?: 0,
            matchesWon = prefs[Keys.CAREER_WINS] ?: 0,
            totalRuns = prefs[Keys.CAREER_RUNS] ?: 0,
            totalWickets = prefs[Keys.CAREER_WICKETS] ?: 0,
            highScore = prefs[Keys.CAREER_HIGH] ?: 0,
            sixesHit = prefs[Keys.CAREER_SIXES] ?: 0,
            foursHit = prefs[Keys.CAREER_FOURS] ?: 0
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                graphicsQuality = enumValues<GraphicsQuality>().firstOrNull { it.name == prefs[Keys.GRAPHICS] }
                    ?: GraphicsQuality.HIGH,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
                commentarySoundEnabled = prefs[Keys.SOUND] ?: true,
                defaultTimeOfDay = enumValues<TimeOfDay>().firstOrNull { it.name == prefs[Keys.TIME_OF_DAY] }
                    ?: TimeOfDay.NIGHT,
                defaultWeather = enumValues<WeatherCondition>().firstOrNull { it.name == prefs[Keys.WEATHER] }
                    ?: WeatherCondition.CLEAR,
                preferredTeamId = prefs[Keys.TEAM_ID] ?: "ind_tigers"
            )
            val next = transform(current)
            prefs[Keys.GRAPHICS] = next.graphicsQuality.name
            prefs[Keys.HAPTICS] = next.hapticsEnabled
            prefs[Keys.SOUND] = next.commentarySoundEnabled
            prefs[Keys.TIME_OF_DAY] = next.defaultTimeOfDay.name
            prefs[Keys.WEATHER] = next.defaultWeather.name
            prefs[Keys.TEAM_ID] = next.preferredTeamId
        }
    }

    suspend fun updateCareer(transform: (CareerProfileLocal) -> CareerProfileLocal) {
        context.dataStore.edit { prefs ->
            val current = CareerProfileLocal(
                playerName = prefs[Keys.CAREER_NAME] ?: "Aarav Sharma",
                level = prefs[Keys.CAREER_LEVEL] ?: 1,
                xp = prefs[Keys.CAREER_XP] ?: 0,
                skillPoints = prefs[Keys.CAREER_SP] ?: 3,
                battingRating = prefs[Keys.CAREER_BAT] ?: 72,
                bowlingRating = prefs[Keys.CAREER_BOWL] ?: 68,
                fieldingRating = prefs[Keys.CAREER_FIELD] ?: 74,
                matchesPlayed = prefs[Keys.CAREER_MATCHES] ?: 0,
                matchesWon = prefs[Keys.CAREER_WINS] ?: 0,
                totalRuns = prefs[Keys.CAREER_RUNS] ?: 0,
                totalWickets = prefs[Keys.CAREER_WICKETS] ?: 0,
                highScore = prefs[Keys.CAREER_HIGH] ?: 0,
                sixesHit = prefs[Keys.CAREER_SIXES] ?: 0,
                foursHit = prefs[Keys.CAREER_FOURS] ?: 0
            )
            val next = transform(current)
            prefs[Keys.CAREER_NAME] = next.playerName
            prefs[Keys.CAREER_LEVEL] = next.level
            prefs[Keys.CAREER_XP] = next.xp
            prefs[Keys.CAREER_SP] = next.skillPoints
            prefs[Keys.CAREER_BAT] = next.battingRating
            prefs[Keys.CAREER_BOWL] = next.bowlingRating
            prefs[Keys.CAREER_FIELD] = next.fieldingRating
            prefs[Keys.CAREER_MATCHES] = next.matchesPlayed
            prefs[Keys.CAREER_WINS] = next.matchesWon
            prefs[Keys.CAREER_RUNS] = next.totalRuns
            prefs[Keys.CAREER_WICKETS] = next.totalWickets
            prefs[Keys.CAREER_HIGH] = next.highScore
            prefs[Keys.CAREER_SIXES] = next.sixesHit
            prefs[Keys.CAREER_FOURS] = next.foursHit
        }
    }
}
