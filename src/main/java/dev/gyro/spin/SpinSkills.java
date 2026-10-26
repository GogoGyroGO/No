package dev.gyro.spin;

/** Shared progression rules. Only the server is allowed to apply purchases. */
public final class SpinSkills {
    private static final int[] COSTS = {0, 0, 3, 5, 8, 12};
    private SpinSkills() { }
    public static int cost(int rank) {
        if (rank < 1 || rank > 5) throw new IllegalArgumentException("Invalid Spin rank: " + rank);
        return COSTS[rank];
    }
    public static boolean canLearn(int currentRank, int levels, boolean creative) {
        return currentRank >= 1 && currentRank < 5 && (creative || levels >= cost(currentRank + 1));
    }
}
