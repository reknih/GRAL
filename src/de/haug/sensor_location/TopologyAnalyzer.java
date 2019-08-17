package de.haug.sensor_location;

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class TopologyAnalyzer {
    Graph<Node, DefaultWeightedEdge> g = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    Map<Long, Relay> relays;

    public TopologyAnalyzer() throws Exception {
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

    private void addRelay(long id) throws Exception {
        relays.put(id, new Relay(id));
    }

    public Relay getRelay(long id) {
        return relays.get(id);
    }

    public float getDistance(long srcId, long destId) {
        var startRelay = getRelay(srcId);
        var destRelay = getRelay(destId);

        if (startRelay == null || destRelay == null) throw new NoSuchElementException("Ids not found");

        ShortestPathAlgorithm<Node, DefaultWeightedEdge> shortestPathAlg = new DijkstraShortestPath<>(g);
        var path = shortestPathAlg.getPath(startRelay, destRelay);
        if (path == null) throw new RuntimeException("No such path in graph");

        return (float)path.getWeight();
    }
}
