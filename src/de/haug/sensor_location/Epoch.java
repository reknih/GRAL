package de.haug.sensor_location;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class Epoch {
    List<Package> packages;
    EpochType type;
    float distance;
    Long startTime;

    public enum EpochType {
        RELAY_WITHDRAWAL,
        RELAY_APPROACH,
        VOYAGE
    }

    public Epoch(EpochType t) {
        this.type = t;
        this.packages = new LinkedList<>();
        this.distance = Float.NaN;
        this.startTime = null;
    }

    public Epoch(EpochType t, Package p) {
        this(t);
        this.packages.add(p);
    }

    public Epoch(EpochType t, Package p, long startTime) {
        this(t, p);
        this.startTime = startTime;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public void addPackage(Package p) {
        packages.add(p);
    }

    public EpochType getType() {
        return type;
    }

    public Package getLatest() throws EpochException {
        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(packages.size() - 1);
    }

    public long getStartTime() throws EpochException {
        if (startTime != null) {
            return startTime;
        }

        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(0).getTimestamp();
    }

    public long getEndTime() throws EpochException {
        if (packages.size() < 1) {
            throw new EpochException("Epoch has no packages");
        }

        return packages.get(packages.size() - 1).getTimestamp();
    }

    public long getDuration() throws EpochException {
        return getEndTime() - getStartTime();
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public double getAverageSpeed() throws EpochException {
        if (Float.isNaN(distance)) {
            throw new EpochException("Distance not set");
        }

        return distance / getDuration();
    }

    public void setPackagePositions(Position startingPosition) throws EpochException {
        for (var p : packages) {
            //if (p.getPosition() == null) {
                p.setPosition(new Position(startingPosition.getStart(), startingPosition.getDest(),
                        (float)(startingPosition.getPositionInBetween() + (p.getTimestamp() - getStartTime()) * getAverageSpeed()),
                        startingPosition.getTotalDistance()));
            //}
        }
    }

    public static EpochType typeFromDirection(Direction d) {
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
     * Returns the next relay contact from a adjacent epoch
     * @param epochs The epoch list in which to look
     * @param index The index of the current epoch
     * @param backwards Whether to go forwards or backwards
     * @return The relay that was next contacted
     */
    public static WirelessContact getNeighbourRelay(List<Epoch> epochs, int index, boolean backwards) throws EpochException, NoSuchElementException {
        if ((index < 1 && backwards) || (index > epochs.size() - 2 && !backwards))
            throw new NoSuchElementException("Index at border of list");

        for (int i = i = backwards ? index - 1 : index + 1; i < epochs.size() && i >= 0; i = backwards ? i - 1 : i + 1) {
            var epoch = epochs.get(i);
            if (!epoch.getType().equals(EpochType.VOYAGE)) {
                return epoch.getLatest().getStrongestRelay();
            }
        }
        throw new NoSuchElementException("No relay contact found in the surroundings");
    }
}
