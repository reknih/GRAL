package de.haug.sensor_location;

import java.util.Collection;

/**
 * Class representing a detection of another wireless-enabled node
 */
@SuppressWarnings("WeakerAccess")
public class WirelessContact {
    /**
     * Id of the contacted node
     */
    private long nodeId;
    /**
     * Signal strength
     */
    private float strength;
    /**
     * Direction of the contact relative to the last package
     */
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

    /**
     * @return The Id of the contacted node
     */
    long getNodeId() {
        return nodeId;
    }

    /**
     * @return The signal strength of the contact
     */
    float getStrength() {
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

    /**
     * @param direction The direction of the contact relative to the last package
     */
    void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * @return The direction of the contact relative to the last package
     */
    Direction getDirection() {
        return direction;
    }
}
