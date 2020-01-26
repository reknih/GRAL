package de.haug.gral;

import java.io.Serializable;
import java.util.*;

/**
 * Class that can annotate packages with estimated positions.
 */
public class Locator implements Serializable {

    /**
     * The signal strength above which a sensor is
     * assumed to have the same location as the relay node.
     */
    private float maxSignal = .9f;

    /**
     * Tolerance w.r.t maxSignal
     */
    private float tolerance = .1f;

    /**
     * Tolerance for clock sync error
     */
    private final int TIME_TOLERANCE = 3;

    private boolean checkpoints;
    private boolean pathRectification;

    /**
     * Mapping the sensor's id's to the objects
     */
    Map<Long, Sensor> sensors;
    TopologyAnalyzer topologyAnalyzer;

    /**
     * Constructs a new Locator instance
     * @param t Your populated TopologyAnalyzer
     * @param checkpoints Whether to use checkpoints for mobile node encounters
     * @param pathRectification Whether to use graph-based path rectification
     */
    public Locator(TopologyAnalyzer t, boolean checkpoints, boolean pathRectification) {
        this.checkpoints = checkpoints;
        this.pathRectification = pathRectification;
        sensors = new HashMap<>();
        this.topologyAnalyzer = t;
        if (topologyAnalyzer == null) {
            topologyAnalyzer = new TopologyAnalyzer();
            topologyAnalyzer.addSampleNetwork();
        }
    }

    /**
     * Constructs a new Locator instance with a sample topology
     * @param checkpoints Whether to use checkpoints for mobile node encounters
     * @param pathRectification Whether to use graph-based path rectification
     */
    public Locator(boolean checkpoints, boolean pathRectification) {
        this(null, checkpoints, pathRectification);
    }

    /**
     * Constructs a new Locator instance with a sample topology and all features activated
     */
    public Locator() {
        this(null, true, true);
    }

    public void baseLineFeed(Package p) {
        Sensor.ensureAddedSensor(p.getSensorId(), sensors);
        Sensor s = sensors.get(p.getSensorId());

        if (s.getMysteryEpochs().size() <= 0) {
            s.addEpoch(Epoch.EpochType.BASELINE, p);
        } else {
            s.getMysteryEpochs().get(0).addPackage(p);
        }
    }

    public List<Package> baseLineProcess(Long sensorId) {
        Sensor s = sensors.get(sensorId);
        if (s == null) throw new RuntimeException("Unknown sensor id requested for processing");

        if (s.getMysteryEpochs().size() <= 0) return new LinkedList<>();

        Epoch baselineEpoch = s.getMysteryEpochs().get(0);
        List<Package> sameRelayPacks = new LinkedList<>();
        Long lastId = -1L;
        List<List<Package>> etapList = new LinkedList<>();

        for (Package p : baselineEpoch.getPackages()) {
            if (etapList.size() <= 0) {
                etapList.add(new LinkedList<>());
            }

            WirelessContact wcStrongest = p.getStrongestRelay();
            if (sameRelayPacks.size() > 0 && (wcStrongest == null || wcStrongest.getNodeId() != sameRelayPacks.get(0).getStrongestRelay().getNodeId())) {
                float max = Float.NEGATIVE_INFINITY;
                int maxi = 0;
                for (int i = 0; i < sameRelayPacks.size(); i++) {
                    WirelessContact wcStrongestPrev = sameRelayPacks.get(i).getStrongestRelay();
                    if (wcStrongestPrev.getStrength() >= max) {
                        max = wcStrongestPrev.getStrength();
                        maxi = i;
                    }
                }

                lastId = sameRelayPacks.get(0).getStrongestRelay().getNodeId();
                etapList.get(etapList.size() - 1).addAll(sameRelayPacks.subList(0, maxi + 1));
                etapList.add(new LinkedList<>(sameRelayPacks.subList(maxi + 1, sameRelayPacks.size())));
                sameRelayPacks.clear();
            }

            if (wcStrongest == null || wcStrongest.getNodeId() == lastId) {
                // No relay contact
                // Add to existing etap or create new if none
                etapList.get(etapList.size() - 1).add(p);
            } else {
                sameRelayPacks.add(p);
            }
        }
        if (sameRelayPacks.size() > 0) {
            float max = Float.NEGATIVE_INFINITY;
            int maxi = 0;
            for (int i = 0; i < sameRelayPacks.size(); i++) {
                WirelessContact wcStrongestPrev = sameRelayPacks.get(i).getStrongestRelay();
                if (wcStrongestPrev.getStrength() >= max) {
                    max = wcStrongestPrev.getStrength();
                    maxi = i;
                }
            }

            etapList.get(etapList.size() - 1).addAll(sameRelayPacks.subList(0, maxi + 1));
            etapList.add(new LinkedList<>(sameRelayPacks.subList(maxi + 1, sameRelayPacks.size())));
            sameRelayPacks.clear();
        }

        List<Package> output = new LinkedList<>();

        for (int i = 0; i < etapList.size(); i++){
            if (i == 0) continue;
            List<Package> etap = etapList.get(i);
            if (i == etapList.size() - 1 && etap.size() <= 0) continue;
            WirelessContact wcStrongestEnd = etap.get(etap.size() - 1).getStrongestRelay();
            if (wcStrongestEnd == null) continue;
            WirelessContact wcStrongestStart = etapList.get(i - 1).get(etapList.get(i - 1).size() - 1).getStrongestRelay();
            if (wcStrongestStart == null)
                throw new RuntimeException("Etap ends with consecutive etaps should always feature a relay");

            float dist = topologyAnalyzer.getDistance(wcStrongestStart.getNodeId(), wcStrongestEnd.getNodeId());
            float timeDelta = etap.get(etap.size() - 1).getTimestamp() - etapList.get(i - 1).get(etapList.get(i - 1).size() - 1).getTimestamp();

            for (Package p : etap) {
                float start = p.getTimestamp() - etapList.get(i - 1).get(etapList.get(i - 1).size() - 1).getTimestamp();
                p.setPosition(new Position(
                        topologyAnalyzer.getRelay(wcStrongestStart.getNodeId()),
                        topologyAnalyzer.getRelay(wcStrongestEnd.getNodeId()),
                        Math.min(dist * start / timeDelta, dist),
                        dist));
            }
            output.addAll(etap);
        }

        s.getMysteryEpochs().clear();
        return output;
    }

    /**
     * Feed a package into the locator. The package will be added to the localization queue of the sensors.
     * The return value will be an empty list if no new localizations could be made or have several items if
     * localization using p has become possible.
     * @param p The package to feed
     * @return List of previously fed, localized packages
     */
    public List<Package> feed(Package p) {
        // Add sensor to dict if new
        Sensor.ensureAddedSensor(p.getSensorId(), sensors);
        Sensor s = sensors.get(p.getSensorId());

        // Add wireless neighbourhood to dictionaries and set maxSignal
        for (WirelessContact w : p.contacts) {
            if (Node.isSensor(w.getNodeId())) {
                if (!sensors.containsKey(w.getNodeId())) {
                    sensors.put(w.getNodeId(), new Sensor(w.getNodeId()));
                }
            } else {
                maxSignal = Math.max(w.getStrength(), maxSignal);
            }
        }

        // Get a list of the relay contacts
        LinkedList<WirelessContact> detectedRelays = new LinkedList<>(p.contacts);
        detectedRelays.removeIf(c -> Node.isSensor(c.getNodeId()));

        if (detectedRelays.size() > 0) {
            WirelessContact strongestRelayContact = WirelessContact.getStrongestSignal(detectedRelays);

            // For every contacted relay, determine heading
            for (WirelessContact relayContact : detectedRelays) {
                Relay relay = topologyAnalyzer.getRelay(relayContact.getNodeId());
                WirelessContact lastContact = s.getLastPackage() == null ? null : s.getLastPackage().getContactToNode(relay.getId());

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
                        int index = s.getMysteryEpochs().size();

                        // Find latest non-same epoch
                        if (s.getLatestEpoch() != null) {
                            if (s.getLatestEpoch().getType() == Epoch.EpochType.RELAY_WITHDRAWAL) {
                                index--;
                            }
                        }

                        if (Epoch.getLastNonVoyageEpoch(s.getMysteryEpochs(), index,
                                true).getType() == Epoch.EpochType.RELAY_APPROACH) {
                            addToEpochs(s, p, Epoch.EpochType.RELAY_WITHDRAWAL);
                            return clearSensorEpochs(s);
                        }
                    } catch (NoSuchElementException e) {
                        // continue
                    }

                }
            }

            // Clear sensor history if a location gets known
            // because it is in the direct vicinity of a relay
            if (strongestRelayContact.getStrength() + tolerance >= maxSignal) {
                //p.setPosition(new Position(strongestRelay, null, 0, 0));
                try {
                    Epoch lastMeaningful = Epoch.getLastNonVoyageEpoch(s.getMysteryEpochs(),
                            s.getMysteryEpochs().size(), true);
                    if (lastMeaningful.getType() == Epoch.EpochType.RELAY_APPROACH) {
                        addToEpochs(s, p, Epoch.typeFromDirection(strongestRelayContact.getDirection()));
                        return clearSensorEpochs(s);
                    }
                } catch (NoSuchElementException e) {
                    if (addToEpochs(s, p, Epoch.EpochType.RELAY_APPROACH) == Epoch.EpochType.RELAY_APPROACH) {
                        return clearSensorEpochs(s);
                    }
                }

            }

            Epoch.EpochType type = addToEpochs(s, p, Epoch.typeFromDirection(strongestRelayContact.getDirection()));
            if (type == Epoch.EpochType.RELAY_WITHDRAWAL) {
                try {
                    if (Epoch.getLastNonVoyageEpoch(s.getMysteryEpochs(), s.getMysteryEpochs().size() - 1,
                            true).getType() == Epoch.EpochType.RELAY_APPROACH) {
                        return clearSensorEpochs(s);
                    }
                } catch (NoSuchElementException e) {
                    // That's fine
                }
            } else if (type == Epoch.EpochType.RELAY_APPROACH) {
                try {
                    Epoch.getLastNonVoyageEpoch(s.getMysteryEpochs(), s.getMysteryEpochs().size() - 1,
                            true);
                    return clearSensorEpochs(s, s.getMysteryEpochs().size() - 1);
                } catch (NoSuchElementException e) {
                    // Do nothing
                }
            }

        } else {
            addToEpochs(s, p, Epoch.EpochType.VOYAGE);
        }

        return new LinkedList<>();
    }

    /**
     * Adds a package to an existing Epoch of s if type matches and no checkpoint was found,
     * creates a new epoch otherwise.
     * @param s The sensor that recorded p
     * @param p The package in question
     * @param type The required EpochType
     */
    Epoch.EpochType addToEpochs(Sensor s, Package p, Epoch.EpochType type) {
        Epoch e = s.getLatestEpoch();
        if (e == null) {
            s.addEpoch(type, p);
        } else if (!e.getType().equals(type)) {
            if (type != Epoch.EpochType.VOYAGE) {
                List<Epoch> epochs = s.getMysteryEpochs();

                WirelessContact strongest = p.getStrongestRelay();
                Relay prevStrongestRelay = null;
                int nonVoyageId;
                try {
                    Epoch nonVoyage = Epoch.getLastNonVoyageEpoch(epochs, epochs.size(), true);
                    prevStrongestRelay = topologyAnalyzer.getRelay(nonVoyage.getRelayContact().getNodeId());
                    nonVoyageId = epochs.indexOf(nonVoyage);
                } catch (NoSuchElementException err) {
                    nonVoyageId = -1;

                    if (s.getLastKnownPosition() != null) {
                        Position pos = s.getLastKnownPosition();
                        if (pos.getDest() instanceof Relay) {
                            if (pos.getTotalDistance() - pos.getPositionInBetween() <= ((Relay)pos.getDest()).getRadius()) {
                                prevStrongestRelay = (Relay)pos.getDest();
                            }
                        }
                    }
                }
                if (prevStrongestRelay != null) {
                    if (strongest.getNodeId() == prevStrongestRelay.getId()) {
                        LinkedList<Package> packages = new LinkedList<>();

                        // Collapse all epochs after this and the sensed epochs, add packages including p to it if W, otherwise create new W epoch
                        for (int i = epochs.size() - 1; i > nonVoyageId; i--) {
                            packages.addAll(0, epochs.get(i).getPackages());
                            epochs.remove(i);
                        }

                        packages.add(p);
                        if (nonVoyageId < 0 || epochs.get(nonVoyageId).getType() != Epoch.EpochType.RELAY_WITHDRAWAL) {
                            s.addEpoch(Epoch.EpochType.RELAY_WITHDRAWAL, packages.get(0));
                            packages.remove(0);
                        }

                        for (Package pack : packages) {
                            s.getMysteryEpochs().get(s.getMysteryEpochs().size() - 1).addPackage(pack);
                        }
                        return Epoch.EpochType.RELAY_WITHDRAWAL;
                    }
                }
            }

            s.addEpoch(type, p);
        } else {
            // If a checkpoint is found for a time before the timestamp add new epoch
            // set end position of previous epoch
            RendezVous checkpoint = s.getCheckpoint(e.packages.get(0).getTimestamp(), p.getTimestamp());
            if (checkpoint != null) {
                // Index of last package to retain in old epoch
                int timestampSmaller = e.getPackages().size();
                for (int i = e.getPackages().size() - 1; i >= 0; i--) {
                    if (e.getPackages().get(i).getTimestamp() <= checkpoint.getTimestamp()) {
                        timestampSmaller = i;
                        break;
                    }
                }
                if (timestampSmaller + 1 <= e.getPackages().size()) {
                    List<Package> sublist = e.getPackages().subList(timestampSmaller + 1, e.getPackages().size());
                    List<Package> newPackages = new LinkedList<>(sublist);
                    newPackages.add(p);
                    sublist.clear();

                    e.endPosition = checkpoint;
                    e.renewStrongestContactInfo();
                    s.addEpoch(type, newPackages.get(0));
                    newPackages.remove(0);

                    for (Package pack : newPackages) {
                        s.getLatestEpoch().addPackage(pack);
                    }

                    if (s.getLatestEpoch().getRelayContact() != null) {
                        e.setRelayContact(s.getLatestEpoch().getRelayContact());
                    }

                    return type;
                } else {
                    e.addPackage(p);
                }
            }

            e.addPackage(p);
        }
        return type;
    }

    /**
     * Estimate the position of packages within a epoch.
     * @param s Sensor for which to do that
     * @param i Index of the epoch
     * @return Null if the superior function may continue execution, an empty List of packages if
     * localization remains impossible and a List of packages if localization is complete
     */    private List<Package> calculateEpochPosition(Sensor s, int i) {
        return calculateEpochPosition(s, i, false);
    }

    /**
     * Estimate the position of packages within a epoch.
     * @param s Sensor for which to do that
     * @param i Index of the epoch
     * @param recursed Indicates whether the function was called by itself
     * @return Null if the superior function may continue execution, an empty List of packages if
     * localization remains impossible and a List of packages if localization is complete
     */
    private List<Package> calculateEpochPosition(Sensor s, int i, boolean recursed) {
        float distance;
        Position startingPosition;

        List<Epoch> epochs = s.getMysteryEpochs();
        Epoch epoch = epochs.get(i);

        if (epoch.getType().equals(Epoch.EpochType.VOYAGE)) {
            Long lastId = null;
            Position lastKnownPosition = null;

            Relay strongestFutureContact;

            try {
                strongestFutureContact = topologyAnalyzer.getRelay(
                        Epoch.getNeighbourRelay(epochs, i, false).getNodeId());
            } catch (NoSuchElementException e) {
                if (i - 1 >= 0) {
                    if (epochs.get(i - 1).endPosition != null) {
                        return s.mergeAndClearEpochs(i);
                    }
                }
                return new LinkedList<>();
            }

            if (i - 1 >= 0) {
                if (epochs.get(i - 1).endPosition != null) {
                    lastKnownPosition = epochs.get(i - 1).endPosition;
                    if (lastKnownPosition.getPositionInBetween() == lastKnownPosition.getTotalDistance()) {
                        lastKnownPosition = new Position(lastKnownPosition.getDest(), strongestFutureContact,
                                0, topologyAnalyzer.getDistance(lastKnownPosition.getDest().getId(),
                                strongestFutureContact.getId()));
                    }
                    lastId = lastKnownPosition.getStart().getId();
                }
            }

            if (lastKnownPosition == null) {
                if (s.getLastKnownPosition() != null) {
                    // If the sensor had a last known position and use its relay
                    lastId = s.getLastKnownPosition().getDest().getId();
                    lastKnownPosition = s.getLastKnownPosition();
                } else if (strongestFutureContact != null) {
                    // If the next relay is known and no previous contacts are saved,
                    // set incomplete position data since the origin will remain unknown
                    // no matter what
                    for (Package pack : epoch.getPackages()) {
                        pack.setPosition(new Position(null, topologyAnalyzer.getRelay(
                                strongestFutureContact.getId()), 0,
                                Float.POSITIVE_INFINITY));
                    }
                    return null;
                } else {
                    // Data not sufficient, sensor has to have contacted at least one relay
                    return new LinkedList<>();
                }
            }

            Relay strongestLastContact = topologyAnalyzer.getRelay(lastId);

            float totalDistance =
                    topologyAnalyzer.getDistance(strongestLastContact.getId(), strongestFutureContact.getId());

            float alreadyGoneDistance = 0f;
            if (lastKnownPosition.getStart().getId() == lastId &&
                    lastKnownPosition.getDest().getId() == strongestFutureContact.getId()) {
                alreadyGoneDistance = lastKnownPosition.getPositionInBetween();
            }

            try {
                if (epoch.endPosition == null) throw new RuntimeException();

                distance = topologyAnalyzer.getTotalRoutePosition(epoch.endPosition, strongestLastContact,
                        strongestFutureContact).getPositionInBetween() - alreadyGoneDistance;
            } catch (RuntimeException e) {
                distance = totalDistance
                        - (alreadyGoneDistance + strongestFutureContact.getRadius());
            }

            startingPosition = new Position(strongestLastContact, strongestFutureContact,
                    alreadyGoneDistance, totalDistance);
        } else {
            // Epoch with Relay contact
            Relay strongestContact = topologyAnalyzer.getRelay(epoch.getRelayContact().getNodeId());
            distance = strongestContact.getRadius();

            // Find distance and starting position.
            Relay prevRelay = null;
            Relay nextRelay = null;
            try {
                prevRelay = topologyAnalyzer.getRelay(Epoch.getNeighbourRelay(epochs, i, true).getNodeId());
            } catch (NoSuchElementException e) {
                try {
                    if (s.getLastKnownPosition() != null) {
                        if (!s.getLastKnownPosition().getDest().equals(strongestContact)) {
                            prevRelay = (Relay) s.getLastKnownPosition().getDest();
                        } else {
                            prevRelay = (Relay) s.getLastKnownPosition().getStart();
                        }
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
                if (epoch.endPosition != null) {
                    if (epoch.endPosition.getStart().equals(strongestContact)) {
                        if (!recursed) {
                            epoch.setType(Epoch.EpochType.RELAY_WITHDRAWAL);
                            return calculateEpochPosition(s, i, true);
                        } else {
                            return null;
                        }
                    } else {
                        if (epoch.endPosition.getDest() instanceof Relay) {
                            prevRelay = (Relay)epoch.endPosition.getStart();
                        }
                    }
                }
                // If the sensor is approaching it may have entered on either side of the relay radius.
                if (prevRelay != null) {
                    float totalDistance = topologyAnalyzer.getDistance(prevRelay.getId(),
                            strongestContact.getId());
                    startingPosition = new Position(prevRelay, strongestContact,
                            totalDistance - strongestContact.getRadius(), totalDistance);
                } else if (nextRelay != null) {
                    // Can not determine last node, fallback for first iteration
                    //  If the previous package position is unknown, set it to new Position(null, node, INF, INF)
                    //  If it is known then process normally

                    Position position = new Position(strongestContact, nextRelay, 0,
                            topologyAnalyzer.getDistance(strongestContact.getId(), nextRelay.getId()));
                    epoch.endPosition = position;
                    for (Package pack : epoch.getPackages()) {
                        pack.setPosition(position);
                    }
                    return null;
                } else {
                    return new LinkedList<>();
                }
            } else {
                if (epoch.endPosition != null) {
                    if (epoch.endPosition.getDest().equals(strongestContact)) {
                        if (!recursed) {
                            epoch.setType(Epoch.EpochType.RELAY_APPROACH);
                            return calculateEpochPosition(s, i, true);
                        } else {
                            return null;
                        }
                    } else {
                        if (epoch.endPosition.getStart() instanceof Relay) {
                            nextRelay = (Relay)epoch.endPosition.getDest();
                        }
                    }
                }
                // If the sensor is withdrawing, the starting position is always assumed to be the center
                // The next or prev relay are used to determine which sign the direction has (abs = r)

                // Push out the already cleared epochs
                if (nextRelay == null) {
                    if (i > 0) {
                        return s.mergeAndClearEpochs(i);
                    }
                    return new LinkedList<>();
                }

                float totalDistance = topologyAnalyzer.getDistance(strongestContact.getId(), nextRelay.getId());
                startingPosition = new Position(strongestContact, nextRelay, 0, totalDistance);
                // Assume there was no change in directionality
                distance = strongestContact.getRadius();
            }
        }

        epoch.setPackagePositions(distance, startingPosition);
        return null;
    }

    /**
     * Sets the packages to determinable locations for a sensor and manages intra-sensor contacts on the way.
     * Callable if a new relay peak or directional change occurred.
     * @param s The sensor for which to do it
     * @return Position-assigned packages from s
     */    List<Package> clearSensorEpochs(Sensor s) {
        return clearSensorEpochs(s, Integer.MAX_VALUE);
    }

    /**
     * Sets the packages to determinable locations for a sensor and manages intra-sensor contacts on the way.
     * Callable if a new relay peak or directional change occurred.
     * @param s The sensor for which to do it
     * @param maxIndex Exclusive upper boundary for the epochs to be processed
     * @return Position-assigned packages from s
     */
    List<Package> clearSensorEpochs(Sensor s, int maxIndex) {
        List<Epoch> epochs = s.getMysteryEpochs();

        // Merge start epoch with withdrawal if applicable
        for (int i = 0; i < epochs.size() - 1; i++) {
            Epoch epoch = epochs.get(i);
            if (epoch.getType().equals(Epoch.EpochType.VOYAGE)) continue;
            Epoch secondEpoch = epochs.get(i + 1);
            if (epoch.getPackages().size() == 1 && epoch.getType().equals(Epoch.EpochType.RELAY_APPROACH)) {
                long relayId = epoch.getPackages().get(0).getStrongestRelay().getNodeId();
                if (secondEpoch.getType().equals(Epoch.EpochType.VOYAGE)) {
                    epoch.setType(Epoch.EpochType.RELAY_WITHDRAWAL);
                } else if (secondEpoch.hasContactToNode(topologyAnalyzer.getRelay(relayId))) {
                    secondEpoch.packages.add(0, epoch.getPackages().get(0));
                    epochs.remove(0);
                }
            }
            break;
        }

        for (int i = 0; i < epochs.size() && i < maxIndex; i++) {
            Epoch epoch = epochs.get(i);

            List<Package> result = calculateEpochPosition(s, i);
            if (result != null) return result;

            // Check if epochs have to be split and calculations redone because of contact to other sensors.
            if (!checkpoints && !pathRectification) continue;
            for (long k : epoch.getStrongestContact().keySet()) {
                Sensor contactedSensor = sensors.get(k);
                Package strongPackage = epoch.getStrongestContact().get(k);

                if (pathRectification && epoch.getType().equals(Epoch.EpochType.VOYAGE)) {
                    // Check for each contact if earliest possible confluence is greater than the calculated position
                    Long lastRelayId = contactedSensor.getLastRelayContactId(strongPackage.getTimestamp()
                            + TIME_TOLERANCE);
                    Node currentStart = strongPackage.position.getStart();
                    if (lastRelayId != null && currentStart != null) {
                        Relay lastRelay = topologyAnalyzer.getRelay(lastRelayId);

                        Node earliestConfluence = topologyAnalyzer.getEarliestSharedNode(
                                currentStart, lastRelay, strongPackage.position.getDest());

                        float minDistance = topologyAnalyzer.getDistance(
                                strongPackage.position.getStart().getId(), earliestConfluence.getId());

                        if (strongPackage.getPosition().getPositionInBetween() < minDistance) {
                            // Split epoch and recalculate (otherwise we'd end up with an impossible contact)
                            Epoch newEpoch = epoch.split(strongPackage, topologyAnalyzer.getGraphEdgePosition(
                                    new Position(strongPackage.position.getStart(),
                                    strongPackage.position.getDest(), minDistance,
                                    strongPackage.position.getTotalDistance())));
                            if (newEpoch != null) {
                                s.mysteryEpochs.add(i + 1, newEpoch);
                                maxIndex++;
                            }

                            calculateEpochPosition(s, i);
                        }
                    }
                }

                if (checkpoints && strongPackage.getPosition().getStart() != null
                        && strongPackage.getPosition().getDest() != null) {
                    // Do this once final positions are determined
                    Position pos = topologyAnalyzer.getGraphEdgePosition(strongPackage.getPosition());
                    Long lastId = contactedSensor.getLastRelayContactId();
                    if (lastId != null && topologyAnalyzer.contains(pos.getStart(),
                            pos.getDest(), new Position(topologyAnalyzer.getRelay(lastId), pos.getDest(), 0, Float.POSITIVE_INFINITY))) {
                        contactedSensor.addRendezVous(
                                new RendezVous(pos, s, strongPackage.getTimestamp()));
                    }
                }
            }
        }

        return s.mergeAndClearEpochs(Math.min(maxIndex, s.getMysteryEpochs().size()));
    }
}
