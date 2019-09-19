package de.haug.gral;

public class Relay extends Node {
    /**
     * Constructs a new relay node.
      * @param id The id of the new relay node (has to stay clear of sensor ids)
     */
    @SuppressWarnings("WeakerAccess")
    public Relay(long id) {
        super(id);
        if (Node.isSensor(id)) {
            throw new RuntimeException("Id does not match Relay status");
        }
    }

    /**
     * @return Wireless range radius of the relay
     */
    float getRadius() {
        return (float) Math.sqrt(10);
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
