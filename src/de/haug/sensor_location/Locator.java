package de.haug.sensor_location;

import java.util.*;

public class Locator {

    // The signal strength above which a sensor is
    // assumed to have the same location as the relay node
    float maxSignal = .9f;
    // Tolerance w.r.t maxSignal
    float tolerance = .1f;

    Map<Long, Sensor> sensors;
    TopologyAnalyzer topologyAnalyzer;

    public Locator() throws Exception {
        sensors = new HashMap<>();
        topologyAnalyzer = new TopologyAnalyzer();
    }

    public List<Package> feed(Package p) throws Exception {
        // TODO If second-last element had same direction as this one,
        //  change direction of the last one and merge epochs

        // Add sensor to dict if new
        if (!sensors.containsKey(p.getSensorId())) {
            sensors.put(p.getSensorId(), new Sensor(p.getSensorId()));
        }

        Sensor s = sensors.get(p.getSensorId());

        // Add wireless neighbourhood to dictionaries and set maxSignal
        for (var w : p.contacts) {
            if (Node.isSensor(w.getNodeId()) && !sensors.containsKey(w.getNodeId())) {
                sensors.put(w.getNodeId(), new Sensor(w.getNodeId()));
            }

            maxSignal = Math.max(w.getStrength(), maxSignal);
        }

        // Get a list of the relay contacts
        LinkedList<WirelessContact> detectedRelays = new LinkedList<>(p.contacts);
        detectedRelays.removeIf(c -> Node.isSensor(c.getNodeId()));

        if (detectedRelays.size() > 0) {
            var strongestRelayContact = WirelessContact.getStrongestSignal(detectedRelays);

            // For every contacted relay, determine heading
            for (var relayContact : detectedRelays) {
                var relay = topologyAnalyzer.getRelay(relayContact.getNodeId());
                var lastContact = s.getLastPackage() == null ? null : s.getLastPackage().getContactToNode(relay.getId());

                if (lastContact == null) {
                    // If the last contact is null and there are packages (i.e. not seen before)
                    // the sensor approaches.
                    // If there are none the sensor has had a purge due to
                    // passing under the relay and thus withdraws.
                    if (s.getLastPackage() == null) {
                        relayContact.setDirection(Direction.WITHDRAWAL);
                    } else {
                        relayContact.setDirection(Direction.APPROACH);
                    }
                } else if (lastContact.getStrength() < relayContact.getStrength()) {
                    relayContact.setDirection(Direction.APPROACH);
                } else if (lastContact.getStrength() >= relayContact.getStrength()) {
                    relayContact.setDirection(Direction.WITHDRAWAL);
                    try {
                        if (s.getMysteryEpochs().get(s.getMysteryEpochs().size() - 1).getType() == Epoch.EpochType.RELAY_APPROACH) {
                            Locator.addToEpochs(s, p, Epoch.EpochType.RELAY_WITHDRAWAL);
                            return clearSensorEpochs(s);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // continue
                    }

                }
            }

            // Clear sensor history if a location gets known
            // because it is in the direct vicinity of a relay
            if (strongestRelayContact.getStrength() + tolerance >= maxSignal) {
                //p.setPosition(new Position(strongestRelay, null, 0, 0));
                Locator.addToEpochs(s, p, Epoch.EpochType.RELAY_APPROACH);
                return clearSensorEpochs(s);
            }

            Locator.addToEpochs(s, p, Epoch.typeFromDirection(strongestRelayContact.getDirection()));
        } else {
            Locator.addToEpochs(s, p, Epoch.EpochType.VOYAGE);
        }

        return new LinkedList<>();
    }

    static void addToEpochs(Sensor s, Package p, Epoch.EpochType type) throws EpochException {
        var e = s.getLatestEpoch();
        if (e == null) {
            s.addEpoch(type, p);
        } else if (!e.getType().equals(type)) {
            s.addEpoch(type, p);
        } else {
            e.addPackage(p);
        }
    }


    List<Package> clearSensorEpochs(Sensor s) throws EpochException {
        var epochs = s.getMysteryEpochs();

        // Merge start epoch with withdrawal if applicable
        if (epochs.size() > 1) {
            var firstEpoch = epochs.get(0);
            var secondEpoch = epochs.get(1);
            if (firstEpoch.getPackages().size() == 1 && firstEpoch.getType() == Epoch.EpochType.RELAY_APPROACH) {
                long relayId = firstEpoch.getPackages().get(0).getStrongestRelay().getNodeId();
                if (secondEpoch.getPackages().get(0).getContactToNode(relayId) != null) {
                    secondEpoch.packages.add(0, firstEpoch.getPackages().get(0));
                    epochs.remove(0);
                }
            }
        }

        for (int i = 0; i < epochs.size(); i++) {
            var epoch = epochs.get(i);
            float distance;
            Position startingPosition;
            if (epoch.getType().equals(Epoch.EpochType.VOYAGE)) {
                long lastId;

                try {
                    lastId = epochs.get(i - 1).getLatest().getStrongestRelay().getNodeId();
                } catch (IndexOutOfBoundsException e) {
                    if (s.getLastKnownPosition() != null) {
                        lastId = s.getLastKnownPosition().getDest().getId();
                    } else {
                        s.mysteryEpochs.remove(i);
                        return new LinkedList<>();
                    }
                }

                var strongestLastContact = topologyAnalyzer.getRelay(lastId);
                var strongestFutureContact = topologyAnalyzer.getRelay(
                        epochs.get(i + 1).getLatest().getStrongestRelay().getNodeId());

                var totalDistance =
                        topologyAnalyzer.getDistance(strongestLastContact.getId(), strongestFutureContact.getId());
                distance = Math.abs(totalDistance)
                        - (strongestLastContact.getRadius() + strongestFutureContact.getRadius());
                startingPosition = new Position(strongestLastContact, strongestFutureContact,
                        strongestLastContact.getRadius(), totalDistance);
            } else {
                var strongestContact = topologyAnalyzer.getRelay(epoch.getLatest().getStrongestRelay().getNodeId());
                distance = strongestContact.getRadius();

                // Find distance and starting position.
                Relay prevRelay = null;
                Relay nextRelay = null;
                try {
                    prevRelay = topologyAnalyzer.getRelay(Epoch.getNeighbourRelay(epochs, i, true).getNodeId());
                } catch (NoSuchElementException e) {
                    try {
                        if (s.getLastKnownPosition() != null) {
                            prevRelay = (Relay)s.getLastKnownPosition().getDest();
                        }
                    } catch (ClassCastException f) {
                        // Remain null
                    }
                }
                try {
                    nextRelay = topologyAnalyzer.getRelay(Epoch.getNeighbourRelay(epochs, i, false).getNodeId());
                } catch (NoSuchElementException e) {
                    // Remain null
                }

                if (epoch.getType().equals(Epoch.EpochType.RELAY_APPROACH)) {
                    // If the sensor is approaching it may have entered on either side of the relay radius.
                    if (prevRelay != null) {
                        var totalDistance = topologyAnalyzer.getDistance(prevRelay.getId(),
                                strongestContact.getId());
                        startingPosition = new Position(prevRelay, strongestContact,
                                topologyAnalyzer.getDistance(prevRelay.getId(),
                                        strongestContact.getId()) - strongestContact.getRadius(), totalDistance);
                    } else {
                        // Can not determine last node
                        // TODO what if this is the first time this happens?
                        //  If the previous package position is unknown, set it to new Position(null, node, INF, INF)
                        //   and delete the epoch
                        //  If it is known then process normally
                        return new LinkedList<>();
                    }
                } else {
                    // If the sensor is withdrawing, the starting position is always assumed to be the center
                    // The next or prev relay are used to determine which sign the direction has (abs = r)

                    // Push out the already cleared epochs
                    if (nextRelay == null) {
                        if (i > 0) {
                            return s.mergeAndClearEpochs(i);
                        }
                        return new LinkedList<>();
                    }

                    var totalDistance = topologyAnalyzer.getDistance(nextRelay.getId(),
                            strongestContact.getId());
                    startingPosition = new Position(strongestContact, nextRelay, 0, totalDistance);
                    // Assume there was no change in directionality
                    distance = strongestContact.getRadius();
                }
            }

            epoch.setDistance(distance);
            epoch.setPackagePositions(startingPosition);
        }

        return s.mergeAndClearEpochs(s.getMysteryEpochs().size());
    }
}
