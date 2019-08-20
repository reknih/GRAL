package de.haug.sensor_location;

public class Position {
    /**
     * The start node of the current trajectory
     */
    private final Node start;

    /**
     * The end node of the current trajectory
     */
    private final Node dest;

    /**
     * How much distance is the device removed from the start node
     */
    private final float positionInBetween;

    /**
     * How much distance there is between start and dest
     */
    private final float totalDistance;

    /**
     * Constructs a new Position between start and dest.
     * @param start The node at which the trajectory starts
     * @param dest The node at which the trajectory ends
     * @param positionInBetween Distance between this point and start
     * @param totalDistance Total distance between start and dest
     */
    Position(Node start, Node dest, float positionInBetween, float totalDistance) {
        this.start = start;
        this.dest = dest;
        this.positionInBetween = positionInBetween;
        this.totalDistance = totalDistance;
    }

    /**
     * @return The start node of the current trajectory
     */
    @SuppressWarnings("WeakerAccess")
    public Node getStart() {
        return start;
    }

    /**
     * @return The end node of the current trajectory
     */
    @SuppressWarnings("WeakerAccess")
    public Node getDest() {
        return dest;
    }

    /**
     * @return Distance between this point and start
     */
    @SuppressWarnings("WeakerAccess")
    public float getPositionInBetween() {
        return positionInBetween;
    }

    /**
     * @return Total distance between start and dest
     */
    @SuppressWarnings("WeakerAccess")
    public float getTotalDistance() {
        return totalDistance;
    }

    /**
     * @return A string representation of this Position
     */
    @Override
    public String toString() {
        if (dest == null) {
            if (positionInBetween == 0) return "Exactly at " + start.toString();
            if (start == null) return "My life is a mystery.";
        } else if (start == null) {
            if (positionInBetween == 0) return "Exactly at " + dest.toString();
        }

        assert start != null;
        assert dest != null;
        return String.format("Travelled %f of %f units from %s to %s", positionInBetween, totalDistance,
                start.toString(), dest.toString());
    }
}
