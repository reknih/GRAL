# GRAL
[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

> A tool to approximate locations of wireless sensors in a tree-like network topology with few known locations

This algorithm is suitable for a central batch approximation of the locations of nodes in a sensor network in topologies
that can be modeled as graphs with weighted vertices. GRAL stands for graph-based location.

## Install

Run the following command in the cloned GRAL directory.
```
mvn clean install
```
After that, you can either add this package to your
dependencies in your `pom.xml` or run the jar in the target folder.

## Usage
### As a dependency of your own project
First, you have to model your environment by constructing a `TopologyAnalyzer`. Call `addRelay(id, radius)` on it to
add the reference nodes of your environment. Then call `addEdge(start, dest, weight)` on the object with the previously
defined relay ids and `weight` as the length of the connecting link in the physical environment.

Then, construct a new `Locator(analyzer, checkpoints, pathRectification)` using the `TopologyAnalyzer` instance.
The latter two arguments are boolean option switches:
- `checkpoints`: Whether two mobile nodes with an encounter should be assigned the same position. Can decrease positional accuracy.
- `pathRectification`: Places lower bounds on positions of nodes that encounter peers from different original relays.
   Increases positional accuracy for deployments with many nodes and complex topographies.

Finally call the `feed` member of your `Locator` for each incoming package. It will output those packages with
positions assigned once they are ready. It may be useful to subclass the `Package` class to contain the sensor values
that the nodes in your WSN collect. The `Package` implementation of GRAL contains a useful `toJsonString` method for
outputting the localized packages to other applications.

### CLI
The JAR can also be run directly. It takes one argument and various flags can be set.
The first and only mandatory argument specifies a JSON file that defines the graph representing your environment.
For example, suppose you want to use GRAL with an environment with a relay 1001 and 1003 that each are on a link to a relay 1002, which,
in turn is connected to a fourth relay 1004, you would use the following file:
```json
[
  {"start": 1001, "destination": 1002, "weight": 50, "destinationRadius": 2.1},
  {"start": 1003, "destination": 1002, "weight": 70, "startRadius": 5.5},
  {"start": 1002, "destination": 1004, "weight": 50}
]
``` 
The `startRadius` and `destinationRadius` properties set the effective wireless radius of the respective reference node.
They only have to be set for the first time the relay is mentioned in the file and can be omitted afterwards. If the value
is not specified the first time a relay is used, its range radius will default to the square root of ten. All other
properties are required.

Please note that, as of now, relay ids have to be greater than 1000.

There are `--checkpoints` and `--pathRectification` flags that set the eponymous settings
described in the previous subsection.

If the `-f FILE` flag is set, the application will expect to find a `FILE` with one JSON representation of a package per
line. Example:
```json
{ "deviceId": 10, "timestamp": 50, "contacts": [{ "deviceId": 11, "strength": 0.7 }] }
```

The application will parse the file and output any localized packages to standard output.

If the flag is not present, the application will expect such packages in the command line input and output localized 
packages as they get ready. This is useful as an interactive mode or for piping.

## Background

I build this project for my bachelor's thesis. Its objective is to annotate readings from floating sensors in a 
sewage network with their estimated positions. The network does not contain localization hardware and the sensors rarely
contact nodes with fixed known positions.
To accomplish a precise approximation contact history with other sensors is taken into account. The solution also
leverages knowledge about the well-known sewage topology.

## Contributing

Feel free to create a Pull Request!

## License

 Apache-2.0 © Martin Haug
 