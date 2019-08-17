package de.haug.sensor_location;

import java.util.LinkedList;
import java.util.List;

public class Package {
    long sensorId;
    long timestamp;
    List<WirelessContact> contacts;
    Position position = null;

    public Package() {
        contacts = new LinkedList<>();
    }

    public Package(long sensorId, long timestamp, List<WirelessContact> contacts) {
        this();
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.contacts = contacts == null ? this.contacts : contacts;
    }

    public Package(long sensorId, long timestamp, WirelessContact contact) {
        this();
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        if (contact != null) {
            this.contacts.add(contact);
        }
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /** Finds a WirelessContact object of the package for a specified id
     * @param id The node id to look for
     * @return The WirelessContact from contacts if found or null otherwise
     */
    public WirelessContact getContactToNode(long id) {
        for (var c : contacts) {
            if (c.getNodeId() == id) {
                return c;
            }
        }

        return null;
    }

    public WirelessContact getStrongestRelay() {
        LinkedList<WirelessContact> detectedRelays = new LinkedList<>(this.contacts);
        detectedRelays.removeIf(c -> Node.isSensor(c.getNodeId()));
        return WirelessContact.getStrongestSignal(detectedRelays);
    }
}
