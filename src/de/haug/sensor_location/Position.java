package de.haug.sensor_location;

public class Position {
    private final Node start;
    private final Node dest;

    private final float positionInBetween;
    private final float totalDistance;

    Position(Node start, Node dest, float positionInBetween, float totalDistance) {
        this.start = start;
        this.dest = dest;
        this.positionInBetween = positionInBetween;
        this.totalDistance = totalDistance;
    }

    public Node getStart() {
        return start;
    }

    public Node getDest() {
        return dest;
    }

    public float getPositionInBetween() {
        return positionInBetween;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    @Override
    public String toString() {
        if (dest == null) {
            if (positionInBetween == 0) return "Exactly at " + start.toString();
        } else if (start == null) {
            if (positionInBetween == 0) return "Exactly at " + dest.toString();
        }

        return String.format("Travelled %f of %f units from %s to %s", positionInBetween, totalDistance, start.toString(), dest.toString());
    }
}
