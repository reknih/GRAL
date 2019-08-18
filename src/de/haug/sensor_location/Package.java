package de.haug.sensor_location;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Package {
    private final long sensorId;
    private final long timestamp;
    Set<WirelessContact> contacts;
    Position position = null;

    public Package(long sensorId, long timestamp) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        contacts = new HashSet<>();
    }

    public Package(long sensorId, long timestamp, Set<WirelessContact> contacts) {
        this(sensorId, timestamp);
        this.contacts = contacts == null ? this.contacts : contacts;
    }

    public Package(long sensorId, long timestamp, WirelessContact contact) {
        this(sensorId, timestamp);
        if (contact != null) {
            this.contacts.add(contact);
        }
    }

    public Position getPosition() {
        return position;
    }

    public long getSensorId() {
        return sensorId;
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

    public class PackageFactory {
        private Package result;

        public PackageFactory(long id, long timestamp) {
            result = new Package(id, timestamp);
        }

        public PackageFactory addContact(WirelessContact wirelessContact) {
            result.contacts.add(wirelessContact);
            return this;
        }

        public PackageFactory setPosition(Position p) {
            result.setPosition(p);
            return this;
        }

        public Package create() {
            return result;
        }
    }
}
