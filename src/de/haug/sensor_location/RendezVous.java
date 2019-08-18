package de.haug.sensor_location;

public class RendezVous extends Position {
    private final Node interceptor;
    private final long timestamp;

    RendezVous(Node start, Node dest, Node interceptor, long timestamp, float positionInBetween, float totalDistance) {
        super(start, dest, positionInBetween, totalDistance);
        this.interceptor = interceptor;
        this.timestamp = timestamp;
    }

    RendezVous(Position reference, Node interceptor, long timestamp) {
        this(reference.getStart(), reference.getDest(), interceptor, timestamp,
                reference.getPositionInBetween(), reference.getTotalDistance());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Node getInterceptor() {
        return interceptor;
    }
}
