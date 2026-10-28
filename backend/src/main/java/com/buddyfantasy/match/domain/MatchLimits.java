package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Business limits of the match module ({@code match.limits}). Checked on every write, never when loading stored
 * data, so lowering a limit does not break existing matches. Each bound must fit its storage type.
 */
public record MatchLimits(int opponentMaxLength, int matchdayMin, int goalsMax, int statCountMax,
                          int sheetPlayersMax) {

    public MatchLimits {
        require(opponentMaxLength >= 1, "match.limits.opponent-max-length must be >= 1");
        require(matchdayMin >= Matchday.MIN && matchdayMin <= Matchday.MAX,
                "match.limits.matchday-min must be " + Matchday.MIN + "-" + Matchday.MAX);
        require(goalsMax >= 0 && goalsMax <= MatchResult.MAX_STORED_GOALS,
                "match.limits.goals-max must be 0-" + MatchResult.MAX_STORED_GOALS);
        require(statCountMax >= 0 && statCountMax <= PlayerPerformance.MAX_STORED_COUNT,
                "match.limits.stat-count-max must be 0-" + PlayerPerformance.MAX_STORED_COUNT);
        require(sheetPlayersMax >= 1, "match.limits.sheet-players-max must be >= 1");
    }

    public void requireValidOpponent(String opponent) {
        require(opponent != null && !opponent.isBlank() && opponent.length() <= opponentMaxLength,
                MatchError.INVALID_OPPONENT, "Opponent must be 1-" + opponentMaxLength + " characters");
    }

    public void requireValid(Matchday matchday) {
        require(matchday == null || matchday.value() >= matchdayMin, MatchError.INVALID_MATCHDAY,
                "Matchday must be >= " + matchdayMin);
    }

    /** Lets callers reject an oversized sheet before doing any work with its lines. */
    public void requireSheetSize(int players) {
        require(players <= sheetPlayersMax, MatchError.SHEET_TOO_LARGE,
                "A match sheet has at most " + sheetPlayersMax + " players");
    }

    public void requireValid(MatchSheet sheet) {
        requireSheetSize(sheet.performances().size());
        MatchResult result = sheet.result();
        require(result == null || (result.goalsFor() <= goalsMax && result.goalsAgainst() <= goalsMax),
                MatchError.INVALID_GOALS, "Goals must be 0-" + goalsMax);
        for (PlayerPerformance p : sheet.performances()) {
            requireCount("goals", p.goals());
            requireCount("penalty goals", p.penaltyGoals());
            requireCount("penalties saved", p.penaltiesSaved());
            requireCount("penalties missed", p.penaltiesMissed());
            requireCount("assists", p.assists());
            requireCount("own goals", p.ownGoals());
        }
    }

    private void requireCount(String name, int value) {
        require(value <= statCountMax, MatchError.INVALID_STAT_COUNT, "Number of " + name + " must be 0-"
                + statCountMax);
    }

    /** An invalid configuration, which only stops the application from starting, has no code. */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainValidationException(message);
        }
    }

    private static void require(boolean condition, MatchError code, String message) {
        if (!condition) {
            throw new DomainValidationException(code, message);
        }
    }
}
