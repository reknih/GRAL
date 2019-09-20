package de.haug.gral;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * A package containing sensor measurements
 */
public class Package implements Serializable {
    /**
     * The id of the sensor that has created the package
     */
    private final long sensorId;

    /**
     * The time of creation of the package
     */
    private final long timestamp;

    /**
     * The other wireless devices encountered at that moment
     */
    Set<WirelessContact> contacts;

    /**
     * Where this package has been measured
     */
    Position position = null;

    /**
     * Creates a new package with a sensorId and a timestamp
     * @param sensorId The sensor which has registered the package
     * @param timestamp The time at which the package has been created
     */
    public Package(long sensorId, long timestamp) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        contacts = new HashSet<>();
    }

    /**
     * Creates a new package with a sensorId, a timestamp and a list of encountered devices
     * @param sensorId The sensor which has registered the package
     * @param timestamp The time at which the package has been created
     * @param contacts Encountered devices
     */
    @SuppressWarnings("unused")
    public Package(long sensorId, long timestamp, Set<WirelessContact> contacts) {
        this(sensorId, timestamp);
        this.contacts = contacts == null ? this.contacts : contacts;
    }

    /**
     * Creates a new package with a sensorId, a timestamp and a single encountered device
     * @param sensorId The sensor which has registered the package
     * @param timestamp The time at which the package has been created
     * @param contact Encountered device
     */
    public Package(long sensorId, long timestamp, WirelessContact contact) {
        this(sensorId, timestamp);
        if (contact != null) {
            this.contacts.add(contact);
        }
    }

    /**
     * @return The position at which the package was measured
     */
    @SuppressWarnings("WeakerAccess")
    public Position getPosition() {
        return position;
    }

    /**
     * @return The id of the sensor that created this package
     */
    @SuppressWarnings("WeakerAccess")
    public long getSensorId() {
        return sensorId;
    }

    /**
     * Setter for the position.
     * @param position Position at which this package was registered
     */
    void setPosition(Position position) {
        this.position = position;
    }

    /**
     * @return The time at which this package was registered
     */
    @SuppressWarnings("WeakerAccess")
    public long getTimestamp() {
        return timestamp;
    }

    /** Finds a WirelessContact object of the package for a specified id.
     * @param id The node id to look for
     * @return The WirelessContact from contacts if found or null otherwise
     */
    WirelessContact getContactToNode(long id) {
        for (WirelessContact c : contacts) {
            if (c.getNodeId() == id) {
                return c;
            }
        }

        return null;
    }

    /**
     * @return A string representing the object as JSON
     */
    public String toJsonString() {
        String positionString = position != null ? position.toJsonString() : "null";
        return String.format("{ \"deviceId\": %d, \"timestamp\": %d, \"contacts\": %s, \"position\": %s }",
                getSensorId(), getTimestamp(), WirelessContact.contactListJsonString(contacts), positionString);
    }

    /**
     * @return The strongest relay contact in the package
     */
    WirelessContact getStrongestRelay() {
        LinkedList<WirelessContact> detectedRelays = new LinkedList<>(this.contacts);
        detectedRelays.removeIf(c -> Node.isSensor(c.getNodeId()));
        return WirelessContact.getStrongestSignal(detectedRelays);
    }
}
