package de.haug.sensor_location;

public abstract class Node {
    final long id;

    Node(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    static boolean isSensor(long nodeId) {
        return nodeId < 1000;
    }

    @Override
    public String toString() {
        return String.format("node %d", id);
    }
}
