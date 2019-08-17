package de.haug.sensor_location;

import java.util.Collection;

public class WirelessContact {
    private long nodeId;
    private float strength;
    private Direction direction;

    /**
     * Constructs a new instance of a wireless contact.
     * @param id Id for the detected node
     * @param signalStrength Detected signal strength
     */
    public WirelessContact(final long id, final float signalStrength) {
        this.nodeId = id;
        this.strength = signalStrength;
        this.direction = Direction.UNKNOWN;
    }

    public long getNodeId() {
        return nodeId;
    }

    public float getStrength() {
        return strength;
    }

    /**
     * @param candidates Collection of WirelessContact objects
     * @return The item of the collection with the strongest signal or null if the collection is empty
     */
    static WirelessContact getStrongestSignal(Collection<WirelessContact> candidates) {
        WirelessContact max = null;

        for (WirelessContact c : candidates) {
            if (max == null) {
                max = c;
            }
            if (max.getStrength() < c.getStrength()) {
                max = c;
            }
        }

        return max;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}
