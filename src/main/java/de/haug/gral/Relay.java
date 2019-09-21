package de.haug.gral;

public class Relay extends Node {
    /**
     * The radius of the circle in which the Relay may be contacted wirelessly
     */
    private float radius;

    /**
     * Constructs a new relay node.
     * @param id The id of the new relay node (has to stay clear of sensor ids)
     * @param radius The effective wireless radius of the relay
     */
    @SuppressWarnings("WeakerAccess")
    public Relay(long id, float radius) {
        super(id);
        this.radius = radius;
        if (Node.isSensor(id)) {
            throw new RuntimeException("Id does not match Relay status");
        }
    }

    /**
     * Constructs a new relay node.
     * @param id The id of the new relay node (has to stay clear of sensor ids)
     */
    @SuppressWarnings("WeakerAccess")
    public Relay(long id) {
        super(id);
        this.radius = (float) Math.sqrt(10);
        if (Node.isSensor(id)) {
            throw new RuntimeException("Id does not match Relay status");
        }
    }

    /**
     * @return Wireless range radius of the relay
     */
    float getRadius() {
        return radius;
        //return 10;
    }

    /**
     * @return Human-readable description
     */
    @Override
    public String toString() {
        return String.format("relay %d", id);
    }
}
