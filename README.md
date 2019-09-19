# GRAL

> A tool to approximate locations of wireless sensors in a tree-like network topology with few known locations

This algorithm is suitable for a central batch approximation of the locations of nodes in a sensor network in topologies
that can be modeled as graphs with weighted vertices. GRAL stands for graph-based location.

## Usage

Model your topology in the `TopologyAnalyzer.java` file using `addRelay` calls and
the JGraphT commands to assemble the graph. An example implementation is provided within the file.
You could also overwrite its example constructor in a child class to do it more cleanly.

Then instantiate a `Locator` class to run `feed` on incoming packages. The method will return a chronological list of
fed packages with their `location` fields populated.

## Background

I build this project for my bachelor's thesis. Its objective is to annotate readings from floating sensors in a 
sewage network with their estimated positions. The network does not contain localization hardware and the sensors rarely
contact nodes with fixed known positions.
To accomplish a precise approximation contact history with other sensors is taken into account. The solution also
leverages knowledge about the well-known sewage topology.

## Installation and Usage

Compile the project in a Java IDE with JGraphT version 1.3 and JUnit version 5.4 in the classpath.
Try running the tests.

## License

 Apache-2.0 © Martin Haug
 