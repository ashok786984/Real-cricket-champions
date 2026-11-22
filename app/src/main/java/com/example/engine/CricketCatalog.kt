package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.data.model.CricketPlayer
import com.example.data.model.CricketStadium
import com.example.data.model.CricketTeam
import com.example.data.model.FranchiseStanding
import com.example.data.model.GameMode
import com.example.data.model.PlayerRole
import com.example.data.model.TimeOfDay
import com.example.data.model.TournamentFixture
import com.example.data.model.TournamentState
import com.example.data.model.WeatherCondition

object CricketCatalog {

    private fun buildSquad(
        prefix: String,
        names: List<String>,
        batBias: Int,
        bowlBias: Int
    ): List<CricketPlayer> {
        val roles = listOf(
            PlayerRole.BATSMAN,
            PlayerRole.BATSMAN,
            PlayerRole.BATSMAN,
            PlayerRole.WICKETKEEPER,
            PlayerRole.BATSMAN,
            PlayerRole.ALL_ROUNDER,
            PlayerRole.ALL_ROUNDER,
            PlayerRole.SPIN_BOWLER,
            PlayerRole.FAST_BOWLER,
            PlayerRole.FAST_BOWLER,
            PlayerRole.FAST_BOWLER
        )
        return names.mapIndexed { idx, name ->
            val role = roles[idx]
            val bat = when (role) {
                PlayerRole.BATSMAN -> (82 + batBias - idx).coerceIn(65, 95)
                PlayerRole.WICKETKEEPER -> (80 + batBias).coerceIn(65, 93)
                PlayerRole.ALL_ROUNDER -> (75 + batBias).coerceIn(60, 90)
                PlayerRole.SPIN_BOWLER -> (52 + batBias).coerceIn(40, 72)
                PlayerRole.FAST_BOWLER -> (46 + batBias).coerceIn(35, 68)
            }
            val bowl = when (role) {
                PlayerRole.FAST_BOWLER -> (84 + bowlBias - (idx - 8)).coerceIn(70, 95)
                PlayerRole.SPIN_BOWLER -> (85 + bowlBias).coerceIn(70, 95)
                PlayerRole.ALL_ROUNDER -> (76 + bowlBias).coerceIn(62, 88)
                PlayerRole.WICKETKEEPER -> 25
                PlayerRole.BATSMAN -> 32
            }
            CricketPlayer(
                id = "${prefix}_p$idx",
                name = name,
                role = role,
                battingSkill = bat,
                bowlingSkill = bowl,
                fieldingSkill = (74 + ((idx * 3) % 16)).coerceIn(68, 94),
                isLeftHanded = idx == 1 || idx == 5,
                isSpinBowler = role == PlayerRole.SPIN_BOWLER || idx == 6
            )
        }
    }

    val teams: List<CricketTeam> = listOf(
        CricketTeam(
            id = "ind_tigers",
            name = "Emerald Tigers",
            shortCode = "EMT",
            primaryColor = Color(0xFF0E7490),
            accentColor = Color(0xFFF59E0B),
            squad = buildSquad(
                "emt",
                listOf(
                    "R. Verma", "S. Gillan", "V. Chauhan", "K. Panton", "S. Iyeran",
                    "H. Pandya", "R. Jadhav", "K. Yadav", "M. Siraj", "A. Singh", "J. Bumra"
                ),
                batBias = 5,
                bowlBias = 4
            )
        ),
        CricketTeam(
            id = "aus_sharks",
            name = "Southern Sharks",
            shortCode = "SSK",
            primaryColor = Color(0xFFCA8A04),
            accentColor = Color(0xFF15803D),
            squad = buildSquad(
                "ssk",
                listOf(
                    "T. Headley", "M. Marshon", "S. Smithers", "J. Inglis", "G. Maxon",
                    "M. Stoinis", "C. Greener", "A. Zampa", "P. Cumming", "M. Starc", "J. Hazel"
                ),
                batBias = 4,
                bowlBias = 5
            )
        ),
        CricketTeam(
            id = "eng_lions",
            name = "Crown Lions",
            shortCode = "CRN",
            primaryColor = Color(0xFF1D4ED8),
            accentColor = Color(0xFFEF4444),
            squad = buildSquad(
                "crn",
                listOf(
                    "P. Salton", "B. Duckett", "J. Rooten", "J. Butler", "H. Brooker",
                    "L. Living", "B. Stokes", "A. Rashid", "J. Archer", "M. Wood", "C. Woakes"
                ),
                batBias = 4,
                bowlBias = 3
            )
        ),
        CricketTeam(
            id = "pak_falcons",
            name = "Crescent Falcons",
            shortCode = "CRF",
            primaryColor = Color(0xFF15803D),
            accentColor = Color(0xFFA3E635),
            squad = buildSquad(
                "crf",
                listOf(
                    "S. Ayub", "F. Zaman", "B. Azhar", "M. Rizwan", "I. Ahmed",
                    "S. Khan", "I. Wasim", "A. Ahmed", "S. Afridi", "H. Rauf", "N. Shah"
                ),
                batBias = 2,
                bowlBias = 5
            )
        ),
        CricketTeam(
            id = "sa_rhinos",
            name = "Highveld Rhinos",
            shortCode = "HVR",
            primaryColor = Color(0xFF166534),
            accentColor = Color(0xFFFACC15),
            squad = buildSquad(
                "hvr",
                listOf(
                    "Q. Kock", "R. Hendricks", "A. Markram", "H. Klaasen", "D. Miller",
                    "T. Stubbs", "M. Jansen", "K. Maharaj", "K. Rabada", "A. Nortje", "L. Ngidi"
                ),
                batBias = 3,
                bowlBias = 4
            )
        ),
        CricketTeam(
            id = "nz_kiwis",
            name = "Alpine Strikers",
            shortCode = "ALP",
            primaryColor = Color(0xFF1E293B),
            accentColor = Color(0xFF38BDF8),
            squad = buildSquad(
                "alp",
                listOf(
                    "F. Allen", "D. Conway", "K. William", "G. Phillips", "D. Mitchell",
                    "R. Ravindra", "M. Santner", "I. Sodhi", "T. Boult", "L. Ferguson", "M. Henry"
                ),
                batBias = 3,
                bowlBias = 3
            )
        ),
        CricketTeam(
            id = "wi_royals",
            name = "Island Royals",
            shortCode = "ISR",
            primaryColor = Color(0xFF881337),
            accentColor = Color(0xFFFBBF24),
            squad = buildSquad(
                "isr",
                listOf(
                    "B. King", "J. Charles", "N. Pooran", "S. Hope", "R. Powell",
                    "A. Russell", "J. Holder", "A. Hosein", "A. Joseph", "G. Motie", "S. Joseph"
                ),
                batBias = 4,
                bowlBias = 2
            )
        ),
        CricketTeam(
            id = "sl_cobras",
            name = "Harbour Cobras",
            shortCode = "HRC",
            primaryColor = Color(0xFF1E3A8A),
            accentColor = Color(0xFFF59E0B),
            squad = buildSquad(
                "hrc",
                listOf(
                    "P. Nissanka", "K. Mendis", "C. Asalanka", "S. Samara", "A. Mathews",
                    "D. Silva", "W. Hasaranga", "M. Theekshana", "M. Pathirana", "D. Chameera", "L. Kumara"
                ),
                batBias = 2,
                bowlBias = 3
            )
        )
    )

    val stadiums: List<CricketStadium> = listOf(
        CricketStadium(
            id = "emerald_bowl",
            name = "Emerald Champions Bowl",
            city = "Mumbai",
            pitchPace = 0.85f,
            pitchBounce = 0.82f,
            pitchTurn = 0.78f,
            boundaryMeters = 68,
            defaultTime = TimeOfDay.NIGHT,
            defaultWeather = WeatherCondition.CLEAR
        ),
        CricketStadium(
            id = "harbour_oval",
            name = "Harbour Grand Oval",
            city = "Melbourne",
            pitchPace = 0.94f,
            pitchBounce = 0.95f,
            pitchTurn = 0.45f,
            boundaryMeters = 78,
            defaultTime = TimeOfDay.DAY,
            defaultWeather = WeatherCondition.CLEAR
        ),
        CricketStadium(
            id = "crown_pavilion",
            name = "Crown Heritage Ground",
            city = "London",
            pitchPace = 0.88f,
            pitchBounce = 0.84f,
            pitchTurn = 0.52f,
            boundaryMeters = 72,
            defaultTime = TimeOfDay.TWILIGHT,
            defaultWeather = WeatherCondition.OVERCAST
        ),
        CricketStadium(
            id = "skyline_arena",
            name = "Skyline Desert Arena",
            city = "Dubai",
            pitchPace = 0.78f,
            pitchBounce = 0.72f,
            pitchTurn = 0.92f,
            boundaryMeters = 70,
            defaultTime = TimeOfDay.NIGHT,
            defaultWeather = WeatherCondition.DRY_DUSTY
        )
    )

    fun teamById(id: String): CricketTeam = teams.firstOrNull { it.id == id } ?: teams.first()

    fun stadiumById(id: String): CricketStadium = stadiums.firstOrNull { it.id == id } ?: stadiums.first()

    fun buildWorldCup(userTeamId: String): TournamentState {
        val others = teams.filter { it.id != userTeamId }.shuffled()
        val bracketTeams = listOf(userTeamId) + others.take(7).map { it.id }
        val fixtures = listOf(
            TournamentFixture("qf1", "Quarter-Final 1", bracketTeams[0], bracketTeams[1]),
            TournamentFixture("qf2", "Quarter-Final 2", bracketTeams[2], bracketTeams[3]),
            TournamentFixture("qf3", "Quarter-Final 3", bracketTeams[4], bracketTeams[5]),
            TournamentFixture("qf4", "Quarter-Final 4", bracketTeams[6], bracketTeams[7])
        )
        return TournamentState(
            title = "Champions World Cup",
            mode = GameMode.WORLD_CUP,
            userTeamId = userTeamId,
            fixtures = fixtures
        )
    }

    fun buildFranchiseLeague(userTeamId: String): TournamentState {
        val others = teams.filter { it.id != userTeamId }.take(5).map { it.id }
        val leagueTeams = listOf(userTeamId) + others
        val fixtures = mutableListOf<TournamentFixture>()
        var idx = 1
        for (i in leagueTeams.indices) {
            for (j in i + 1 until leagueTeams.size) {
                fixtures += TournamentFixture(
                    id = "fl_$idx",
                    roundLabel = "League Match $idx",
                    homeTeamId = leagueTeams[i],
                    awayTeamId = leagueTeams[j]
                )
                idx++
            }
        }
        val standings = leagueTeams.map { FranchiseStanding(teamId = it) }
        return TournamentState(
            title = "Premier Franchise T20",
            mode = GameMode.FRANCHISE,
            userTeamId = userTeamId,
            fixtures = fixtures,
            standings = standings
        )
    }

    fun buildOfflineCup(userTeamId: String): TournamentState {
        val others = teams.filter { it.id != userTeamId }.shuffled().take(3).map { it.id }
        val pool = listOf(userTeamId) + others
        return TournamentState(
            title = "Champions Knockout Cup",
            mode = GameMode.TOURNAMENT,
            userTeamId = userTeamId,
            fixtures = listOf(
                TournamentFixture("sf1", "Semi-Final 1", pool[0], pool[1]),
                TournamentFixture("sf2", "Semi-Final 2", pool[2], pool[3])
            )
        )
    }
}
