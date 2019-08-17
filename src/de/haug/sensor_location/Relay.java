package de.haug.sensor_location;

public class Relay extends Node {
    public Relay(long id) throws Exception {
        super(id);
        if (Node.isSensor(id)) {
            throw new Exception("Id does not match Relay status");
        }
    }

    public float getRadius() {
        return 10;
    }

    @Override
    public String toString() {
        return String.format("relay %d", id);
    }
}
