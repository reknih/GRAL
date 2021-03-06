package de.haug.gral;

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

    public static Object contactListJsonString(Collection<WirelessContact> contacts) {
        String json = "[";
        for (WirelessContact w : contacts) {
            json += w.toJsonString();
            json += ", ";
        }

        if (contacts.size() > 0) {
            json = json.substring(0, json.length() - 2);
        }

        json += "]";
        return json;
    }

    private String toJsonString() {
        return String.format("{ \"deviceId\": %d, \"strength\": %f }", getNodeId(), getStrength());
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

    @Override
    public String toString() {
        return "Contact to " + String.valueOf(nodeId) + String.format(" (strength %.2f)", strength)
                + " of type " + direction.name();
    }
}
