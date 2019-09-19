package de.haug.gral;

class RendezVous extends Position {
    private final Node interceptor;
    private final long timestamp;

    /**
     * Position-like constructor
     * @param start The node at which the trajectory starts
     * @param dest The node at which the trajectory ends
     * @param interceptor The node that was seen during the RendezVous
     * @param timestamp The timestamp at which the RendezVous happened
     * @param positionInBetween Distance between this point and start
     * @param totalDistance Total distance between start and dest
     */
    private RendezVous(Node start, Node dest, Node interceptor, long timestamp, float positionInBetween,
                       float totalDistance) {
        super(start, dest, positionInBetween, totalDistance);
        this.interceptor = interceptor;
        this.timestamp = timestamp;
    }

    /**
     * Constructs a new instance using a position object.
     * @param reference Set inherited properties according to this reference
     * @param interceptor The node that was seen during the RendezVous
     * @param timestamp The timestamp at which the RendezVous happened
     */
    RendezVous(Position reference, Node interceptor, long timestamp) {
        this(reference.getStart(), reference.getDest(), interceptor, timestamp,
                reference.getPositionInBetween(), reference.getTotalDistance());
    }

    /**
     * @return The timestamp at which the RendezVous happened
     */
    long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The node that was seen during the RendezVous
     */
    public Node getInterceptor() {
        return interceptor;
    }
}
