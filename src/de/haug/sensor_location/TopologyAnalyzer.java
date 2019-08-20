package de.haug.sensor_location;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * Class to reason about the graph topology
 */
public class TopologyAnalyzer {
    /**
     * Graph object that represents the topology
     */
    @SuppressWarnings("WeakerAccess")
    protected Graph<Node, DefaultWeightedEdge> g = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    /**
     * Lookup map for relay ids
     */
    @SuppressWarnings("WeakerAccess")
    protected Map<Long, Relay> relays;

    /**
     * Constructor that adds sample topology
     */
    public TopologyAnalyzer() {
        relays = new HashMap<>();

        addRelay(1001);
        addRelay(1002);
        addRelay(1003);
        addRelay(1004);

        g.addVertex(getRelay(1001L));
        g.addVertex(getRelay(1002L));
        g.addVertex(getRelay(1003L));
        g.addVertex(getRelay(1004L));

        g.addEdge(getRelay(1001L), getRelay(1002L));
        g.setEdgeWeight(getRelay(1001L), getRelay(1002L), 100);

        g.addEdge(getRelay(1003L), getRelay(1002L));
        g.setEdgeWeight(getRelay(1003L), getRelay(1002L), 70);

        g.addEdge(getRelay(1002L), getRelay(1004L));
        g.setEdgeWeight(getRelay(1002L), getRelay(1004L), 50);
    }

    /**
     * Adds a relay to the graph
     * @param id Id number of new relay
     */
    @SuppressWarnings("WeakerAccess")
    protected void addRelay(long id) {
        relays.put(id, new Relay(id));
    }

    /**
     * @param id Relay id
     * @return Relay object for id
     */
    Relay getRelay(long id) {
        return relays.get(id);
    }

    /**
     * @param startRelay Start of the path
     * @param destRelay Destination of the path
     * @return Path between startRelay and destRelay
     */
    private GraphPath<Node, DefaultWeightedEdge> getShortestPath(Node startRelay, Node destRelay) {
        if (startRelay == null || destRelay == null) throw new NoSuchElementException("Ids not found");

        ShortestPathAlgorithm<Node, DefaultWeightedEdge> shortestPathAlg = new DijkstraShortestPath<>(g);
        var path = shortestPathAlg.getPath(startRelay, destRelay);
        if (path == null) throw new RuntimeException("No such path in graph");
        return path;
    }

    /**
     * @param srcId Start of the path
     * @param destId Destination of the path
     * @return Distance between srcId and destId on the shortest path
     */
    float getDistance(long srcId, long destId) {
        return (float)getShortestPath(getRelay(srcId), getRelay(destId)).getWeight();
    }

    /**
     * Converts a position object to have its start and end points equal an edge in the graph
     * @param p Position to convert
     * @return Position for which an edge exists such that it shares its start and destination nodes
     */
    Position getGraphEdgePosition(Position p) {
        var path = getShortestPath(p.getStart(), p.getDest());

        float leftWeight = p.getPositionInBetween();

        for(DefaultWeightedEdge e : path.getEdgeList()) {
            var weight = g.getEdgeWeight(e);

            if (weight >= leftWeight) {
                return new Position(g.getEdgeSource(e), g.getEdgeTarget(e), leftWeight, (float) weight);
            } else {
                leftWeight -= weight;
            }
        }

        throw new RuntimeException("Path is shorter than length of position argument");
    }

    /**
     * Given a position that is equivalent to a graph edge,
     * the method returns an equivalent position with specified start and end points
     * @param edgePosition Position for which an edge exists such that it shares its start and destination nodes
     * @param start Starting point of the new position
     * @param end Destination of the new position
     * @return Position equivalent to edgePosition with start as start and end as dest
     */
    Position getTotalRoutePosition(Position edgePosition, Node start, Node end) {
        var path = getShortestPath(start, end);

        var criticalEdge = g.getEdge(edgePosition.getStart(), edgePosition.getDest());
        if (criticalEdge == null)
            throw new RuntimeException("No edge between start and end vertex");
        var weight = edgePosition.getPositionInBetween();

        for(var e : path.getEdgeList()) {
            if (e.equals(criticalEdge)) {
                break;
            }
            weight += g.getEdgeWeight(e);
        }

        return new Position(start, end, weight, (float)path.getWeight());
    }

    /**
     * @param start1 First source node
     * @param start2 Second source node
     * @param dest Destination of the shared paths
     * @return Earliest node that is on the path of both start1 and start2 to dest
     */
    Node getEarliestSharedNode(Node start1, Node start2, Node dest) {
        var path1 = getShortestPath(start1, dest);
        var path2 = getShortestPath(start2, dest);

        var longerPath = path1.getLength() <= path2.getLength() ? path1 : path2;
        var shorterPath = path1.getLength() <= path2.getLength() ? path2 : path1;

        var difference = longerPath.getLength() - shorterPath.getLength();

        var longerPathEdges = longerPath.getEdgeList();
        var shorterPathEdges = shorterPath.getEdgeList();

        for (int i = longerPath.getLength() - 1; i >= 0; i--) {
            var longEdge = longerPathEdges.get(i);
            var shortEdge = shorterPathEdges.get(i - difference);

            if (!g.getEdgeSource(longEdge).equals(g.getEdgeSource(shortEdge))) return g.getEdgeTarget(longEdge);
        }

        return start1;
    }
}
