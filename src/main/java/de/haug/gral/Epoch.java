package de.haug.gral;

import java.util.*;

/**
 * Class that holds a bundle of packages with the same heading.
 */
class Epoch {
    /**
     * List of packages in the epoch, contains at least one element and ordered by timestamp.
     */
    List<Package> packages;
    private EpochType type;
    private float distance;
    private Long startTime;
    private WirelessContact relayContact;
    private Map<Long, Package> strongestContact;

    /**
     * Can be set to end the epoch at a known location, may be null even for processed epochs.
     */
    Position endPosition;

    /**
     * Epoch type enum mirroring the Direction enum.
     */
    enum EpochType {
        RELAY_WITHDRAWAL,
        RELAY_APPROACH,
        VOYAGE
    }

    /**
     * Constructs a new Epoch instance without packages using a given type.
     * A package has to be added before performing any other operation.
     * @param t The epoch type
     */
    private Epoch(EpochType t) {
        this.type = t;
        this.packages = new LinkedList<>();
        this.distance = Float.NaN;
        this.startTime = null;
        this.strongestContact = new HashMap<>();
    }

    /**
     * Constructs a new Epoch instance with a first package using a given type.
     * @param t The epoch type
     * @param p The epoch's first package
     */
    Epoch(EpochType t, Package p) {
        this(t);
        addPackage(p);
    }

    /**
     * Constructs a new Epoch instance starting at a given time with a first package using a given type.
     * @param t The epoch type
     * @param p The epoch's first package
     * @param startTime The time at which to set duration = 0.
     *                  If not set, the first package's timestamp will be used
     */
    Epoch(EpochType t, Package p, long startTime) {
        this(t, p);
        this.startTime = startTime;
    }

    /**
     * Getter for the packages
     * @return The package list
     */
    List<Package> getPackages() {
        return packages;
    }

    /**
     * Adds a package to the epoch and sets this.strongestContact for each registered Sensor to
     * the maximum of the previous strength value and this package's reading.
     * @param p The package to add
     */
    void addPackage(Package p) {
        packages.add(p);

        setStrongestContact(p);
    }

    /**
     * Changes the strongestContact property if package p mandates it
     * @param p The new package
     */
    void setStrongestContact(Package p) {
        LinkedList<WirelessContact> detectedSensors = new LinkedList<>(p.contacts);
        detectedSensors.removeIf(c -> !Node.isSensor(c.getNodeId()));

        for (WirelessContact wc : detectedSensors) {
            long id = wc.getNodeId();
            if (strongestContact.get(id) == null || strongestContact.get(id).getContactToNode(id) == null
                    || strongestContact.get(id).getContactToNode(id).getStrength() <= wc.getStrength()) {
                strongestContact.put(id, p);
            }
        }
    }


    /**
     * Sets the new strongest package.
     * Call after packages have been removed from the epoch
     */
    void renewStrongestContactInfo () {
        strongestContact.clear();
        for (Package p : packages) {
            setStrongestContact(p);
        }
    }

    /**
     * Getter for the Epoch's type
     * @return The Epoch's type
     */
    EpochType getType() {
        return type;
    }

    /**
     * @return A map of Sensor ids to packages for the strongest contact to each sensor
     */
    public Map<Long, Package> getStrongestContact() {
        return strongestContact;
    }

    /**
     * Gets the latest package of the epoch
     * @return The latest package of the epoch
     * @throws EpochException Thrown if the epoch has no packages
     */
    Package getLatest() throws EpochException {
        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(packages.size() - 1);
    }

    /**
     * Gets the time at which the sensor motion in this package starts
     * @return this.startTime if it is set, otherwise the timestamp of the first package
     * @throws EpochException Thrown if the epoch has no packages
     */
    long getStartTime() throws EpochException {
        if (startTime != null) {
            return startTime;
        }

        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(0).getTimestamp();
    }

    /**
     * Tells whether a certain node has been encountered during the epoch
     * @param n Which node to look for
     * @return Whether the node had contact to the node specified in the arguments
     */
    boolean hasContactToNode(Node n) {
        for (Package p : packages) {
            if (p.getContactToNode(n.id) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the time at which the sensor motion in this package stops
     * @return The timestamp of the last package
     * @throws EpochException Thrown if the epoch has no packages
     */
    long getEndTime() throws EpochException {
        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(packages.size() - 1).getTimestamp();
    }

    /**
     * Calculates the difference between start time and end time.
     * @return The duration of the sensor motion in this epoch
     * @throws EpochException Thrown if the epoch has no packages
     */
    long getDuration() throws EpochException {
        return getEndTime() - getStartTime();
    }

    /**
     * Calculates the average distance travelled during a tick.
     * @return The average distance travelled during a tick
     * @throws EpochException Thrown if distance has not been set or if the epoch contains no packages.
     */
    private double getAverageSpeed() throws EpochException {
        if (Float.isNaN(distance)) {
            throw new EpochException("Distance not set");
        }

        long duration = getDuration();

        if (duration == 0) return 1;

        return distance / duration;
    }

    /**
     * Sets the positions of the packages relative to startingPosition and according to distance.
     * @param distance The distance the sensor travelled during the epoch
     * @param startingPosition Position at which the motion of this Epoch starts
     * @throws EpochException Thrown if distance has not been set or if the epoch contains no packages.
     */
    void setPackagePositions(float distance, Position startingPosition) throws EpochException {
        this.distance = distance;

        for (Package p : packages) {
            p.setPosition(new Position(startingPosition.getStart(), startingPosition.getDest(),
                    (float)(startingPosition.getPositionInBetween() + (p.getTimestamp() - getStartTime())
                            * getAverageSpeed()),
                    startingPosition.getTotalDistance()));
        }

        if (endPosition == null) {
            endPosition = this.getLatest().position;
        }
    }

    /**
     * Splits up an Epoch into two: All packages after splitPackage are removed from this Epoch and moved to a
     * newly constructed instance of the same type which will be returned
     * @param splitPackage The last package of this epoch
     * @param endPosition The physical Position at which this Epoch ends. Required for Localization since
     *                    there would be no other way to deal with two subsequent epochs of the same type.
     * @return The newly instantiated split-off Epoch
     */
    Epoch split(Package splitPackage, Position endPosition) {
        int splitIndex = packages.indexOf(splitPackage);

        // Return nothing if this was the last package
        if (splitIndex >= packages.size()) return null;

        List<Package> subList = packages.subList(splitIndex + 1, packages.size());

        Epoch returnEpoch = null;
        if (subList.size() > 0) {
            this.endPosition = endPosition;
            returnEpoch = new Epoch(getType());
            returnEpoch.startTime = splitPackage.getTimestamp();
            returnEpoch.packages.addAll(subList);

            subList.clear();
            renewStrongestContactInfo();
        }

        return returnEpoch;
    }

    /**
     * @param d The direction for which the matching EpochType should be determined
     * @return Returns the matching EpochType for d.
     */
    static EpochType typeFromDirection(Direction d) {
        switch (d) {
            case APPROACH:
                return EpochType.RELAY_APPROACH;
            case WITHDRAWAL:
                return EpochType.RELAY_WITHDRAWAL;
            case UNKNOWN:
                return EpochType.VOYAGE;
        }
        return EpochType.VOYAGE;
    }

    /**
     * Setter for the EpochType
     * @param type The new EpochType
     */
    void setType(EpochType type) {
        this.type = type;
    }

    /**
     * Setter for the relay contact in this epoch. Use if this epoch was result of merge with another one
     * @param r A contact to the strongest relay in this epoch
     */
    void setRelayContact(WirelessContact r) {
        this.relayContact = r;
    }

    /**
     * @return A contact to the strongest relay in this epoch
     */
    WirelessContact getRelayContact() {
        return getLatest().getStrongestRelay() == null ?
                relayContact : getLatest().getStrongestRelay();
    }

    @Override
    public String toString() {
        return type.name() + " epoch with " + packages.size() + " packages";
    }

    /**
     * Returns the next relay contact from a adjacent epoch.
     * @param epochs The epoch list in which to look
     * @param index The index of the current epoch
     * @param backwards Whether to go forwards or backwards
     * @return The relay that was the next to be contacted
     * @throws NoSuchElementException Thrown if index at border of list or no match found
     */
    static WirelessContact getNeighbourRelay(List<Epoch> epochs, int index, boolean backwards)
            throws NoSuchElementException {
        return getLastNonVoyageEpoch(epochs, index, backwards)
                .getRelayContact();
    }

    /**
     * Returns the next relay-contacting Epoch from epochs.
     * @param epochs List of epochs
     * @param index Index of reference element
     * @param backwards Set to true for search in ]i;0], false for ]i;epochs.size[
     * @return The next relay-contacting Epoch from epochs
     * @throws NoSuchElementException Thrown if index at border of list or no match found
     */
    static Epoch getLastNonVoyageEpoch(List<Epoch> epochs, int index, boolean backwards)
            throws NoSuchElementException {
        if ((index < 1 && backwards) || (index > epochs.size() - 2 && !backwards))
            throw new NoSuchElementException("Index at border of list");

        boolean skip = true;
        for (int i = backwards ? index - 1 : index + 1; i < epochs.size() && i >= 0; i = backwards ? i - 1 : i + 1) {
            Epoch epoch = epochs.get(i);
            if (skip && (backwards ? i + 1 : i - 1) < epochs.size() && (backwards ? i + 1 : i - 1) > 0) {
                Epoch nextEpoch = epochs.get(backwards ? i + 1 : i - 1);
                if (nextEpoch.getType().equals(epoch.getType())) {
                    if (nextEpoch.type.equals(EpochType.VOYAGE)) continue;
                    if (nextEpoch.getRelayContact().getNodeId() == epoch.getRelayContact().getNodeId()) continue;
                }
            }
            skip = false;
            if (!epoch.getType().equals(EpochType.VOYAGE)) {
                return epoch;
            }
        }
        throw new NoSuchElementException("No relay contact found in the surroundings");
    }
}
