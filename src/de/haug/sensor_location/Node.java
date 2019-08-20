package de.haug.sensor_location;

/**
 * A node in the topology graph.
 */
public abstract class Node {
    /**
     * The unique node id
     */
    final long id;

    /**
     * Constructs a new node using the id.
     * @param id The new node's id
     */
    Node(long id) {
        this.id = id;
    }

    /**
     * @return The node id
     */
    @SuppressWarnings("WeakerAccess")
    public long getId() {
        return id;
    }

    /**
     * Checks if a node id is a sensor (does not guarantee existence of said sensor).
     * @param nodeId The id to check
     * @return True if it is a sensor id, false if not
     */
    static boolean isSensor(long nodeId) {
        return nodeId < 1000;
    }

    /**
     * @return Returns a string representation of the node.
     */
    @Override
    public String toString() {
        return String.format("node %d", id);
    }
}
