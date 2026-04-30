package com.stephanofer.prismapractice.api.match;

public record MatchScore(int leftRoundsWon, int rightRoundsWon, int currentRound) {

    public MatchScore {
        if (leftRoundsWon < 0 || rightRoundsWon < 0 || currentRound < 1) {
            throw new IllegalArgumentException("Invalid match score values");
        }
    }

    public static MatchScore initial() {
        return new MatchScore(0, 0, 1);
    }

    public MatchScore withRoundWinner(MatchSide side) {
        return switch (side) {
            case LEFT -> new MatchScore(leftRoundsWon + 1, rightRoundsWon, currentRound + 1);
            case RIGHT -> new MatchScore(leftRoundsWon, rightRoundsWon + 1, currentRound + 1);
        };
    }
}
