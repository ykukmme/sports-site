package com.esports.domain.pandascore;

import com.esports.domain.game.Game;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PandaScoreTeamMatcherTest {

    @Mock
    private TeamRepository teamRepository;

    @Test
    void matchesByExternalIdFirst() {
        PandaScoreTeamMatcher matcher = new PandaScoreTeamMatcher(teamRepository);
        Team team = team("Gen.G", "GEN", 1L, "2882");
        when(team.getGame().getId()).thenReturn(1L);

        when(teamRepository.findByExternalId("2882")).thenReturn(Optional.of(team));

        PandaScoreTeamPreview result = matcher.match(
                1L,
                new PandaScoreApiClient.PandaScoreTeam(2882L, "Gen.G", "gen-g", "GEN", null)
        );

        assertThat(result.matchMethod()).isEqualTo(PandaScoreTeamMatchMethod.EXTERNAL_ID);
        assertThat(result.matchedTeamId()).isEqualTo(team.getId());
        assertThat(result.matchedTeamName()).isEqualTo("Gen.G");
    }

    @Test
    void matchesByAcronymWhenExternalIdIsMissing() {
        PandaScoreTeamMatcher matcher = new PandaScoreTeamMatcher(teamRepository);
        Team team = team("Dplus Kia", "DK", 1L, null);

        when(teamRepository.findByExternalId("20")).thenReturn(Optional.empty());
        when(teamRepository.findByGameId(1L)).thenReturn(List.of(team));

        PandaScoreTeamPreview result = matcher.match(
                1L,
                new PandaScoreApiClient.PandaScoreTeam(20L, "Dplus KIA", "dplus-kia", "DK", null)
        );

        assertThat(result.matchMethod()).isEqualTo(PandaScoreTeamMatchMethod.NAME_CANDIDATE);
        assertThat(result.matchedTeamId()).isEqualTo(team.getId());
        assertThat(result.matchedTeamName()).isEqualTo("Dplus Kia");
    }

    @Test
    void matchesBySlugStyleNameIgnoringPunctuation() {
        PandaScoreTeamMatcher matcher = new PandaScoreTeamMatcher(teamRepository);
        Team team = team("Hanwha Life Esports", "HLE", 1L, null);

        when(teamRepository.findByExternalId("42")).thenReturn(Optional.empty());
        when(teamRepository.findByGameId(1L)).thenReturn(List.of(team));

        PandaScoreTeamPreview result = matcher.match(
                1L,
                new PandaScoreApiClient.PandaScoreTeam(42L, "Hanwha-Life Esports", "hanwha-life-esports", "HLE", null)
        );

        assertThat(result.matchMethod()).isEqualTo(PandaScoreTeamMatchMethod.NAME_CANDIDATE);
        assertThat(result.matchedTeamId()).isEqualTo(team.getId());
    }

    private Team team(String name, String shortName, Long gameId, String externalId) {
        Game game = mock(Game.class);
        Team team = new Team(name, shortName, game);
        team.setExternalId(externalId);
        return team;
    }
}
