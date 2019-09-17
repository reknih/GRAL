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
        g.setEdgeWeight(getRelay(1001L), getRelay(1002L), 50);
        g.addEdge(getRelay(1004L), getRelay(1002L));
        g.setEdgeWeight(getRelay(1004L), getRelay(1002L), 50);
        g.addEdge(getRelay(1002L), getRelay(1003L));
        g.setEdgeWeight(getRelay(1002L), getRelay(1003L), 50);

        //g.addEdge(getRelay(1003L), getRelay(1002L));
        //g.setEdgeWeight(getRelay(1003L), getRelay(1002L), 70);

        //g.addEdge(getRelay(1002L), getRelay(1004L));
        //g.setEdgeWeight(getRelay(1002L), getRelay(1004L), 50);
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
        if (p.getStart().equals(p.getDest())) return p;

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
        if (criticalEdge == null) {
            if (edgePosition.getStart().equals(edgePosition.getDest())) {
                if (edgePosition.getDest().equals(end)) {
                    var totalDistance = getDistance(start.getId(), end.getId());
                    return new Position(start, end, totalDistance, totalDistance);
                }
                if (edgePosition.getStart().equals(start)) {
                    var totalDistance = getDistance(start.getId(), end.getId());
                    return new Position(start, end, 0, totalDistance);
                }
            }

            try {
                var edgePosPath = getShortestPath(edgePosition.getStart(), edgePosition.getDest());
                if (path.getEdgeList().containsAll(edgePosPath.getEdgeList())) {
                    var weight = 0f;
                    for (var refEdge : path.getEdgeList()) {
                        if (edgePosPath.getEdgeList().contains(refEdge)) break;
                    }
                    weight += edgePosition.getPositionInBetween();
                    return new Position(start, end, weight, (float)path.getWeight());
                }

            } catch (RuntimeException e) {
                // No path between edgePosition.getStart() and edgePosition.getDest()
                throw e;
            }

            throw new RuntimeException("No edge between start and end vertex");
        }

        if (!path.getEdgeList().contains(criticalEdge)) {
            if (edgePosition.getPositionInBetween() == edgePosition.getTotalDistance()) {
                for(var v : path.getVertexList()) {
                    if (edgePosition.getDest().equals(v)) {
                        return new Position(start, end, getDistance(start.getId(), v.getId()), (float)path.getWeight());
                    }
                }


            } else if (edgePosition.getPositionInBetween() == 0 && edgePosition.getStart().equals(end)) {
                var length = (float)path.getWeight();
                return new Position(start, end, length, length);
            }

            throw new RuntimeException("Position edge not in path");
        }

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

    /**
     * @param start First vertex of the edge in question
     * @param dest Second vertex of the edge in question
     * @param pos Position interpreted as a path
     * @return Whether the path that the position pos describes contains the edge between start and dest
     */
    boolean contains(Node start, Node dest, Position pos) {
        if (start == null || dest == null || pos == null) return false;
        if (pos.getStart() == null || pos.getDest() == null) return false;

        var route = getShortestPath(pos.getStart(), pos.getDest());
        if (pos.getStart() == pos.getDest() && route.getVertexList().contains(pos.getStart())) return true;

        var edge = g.getEdge(start, dest);
        if (edge == null) return false;

        for (var e : route.getEdgeList()) {
            if (e.equals(edge)) return true;
        }

        return false;
    }
}
