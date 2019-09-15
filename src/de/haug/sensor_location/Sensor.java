package de.haug.sensor_location;

import java.util.LinkedList;
import java.util.List;

/**
 * Class for the sensors that will move and create packages
 */
@SuppressWarnings("WeakerAccess")
public class Sensor extends Node {
    /**
     * Epochs that cannot yet be assigned trajectories
     */
    List<Epoch> mysteryEpochs;

    /**
     * Timestamp of the last epoch end
     */
    long lastEpochEnd = 0;

    /**
     * Is the sensor new?
     */
    private boolean pristine = true;

    /**
     * Last known position of the sensor
     */
    private Position lastKnownPosition = null;

    /**
     * Checkpoints at which positions shall be set accordingly
     */
    private List<RendezVous> checkpoints;

    /**
     * Las epoch purge time
     */
    private long lastPurge = Long.MIN_VALUE;

    /**
     * Constructs a new instance
     * @param id Unique sensor id in the id namespace
     */
    public Sensor(long id) {
        super(id);
        checkpoints = new LinkedList<>();
        mysteryEpochs = new LinkedList<>();
        if (!Node.isSensor(id)) {
            throw new RuntimeException("Id does not match Sensor status");
        }
    }

    /**
     * @return Return latest added Package or null if there is none
     */
    Package getLastPackage() {
        var latestEpoch = getLatestEpoch();
        if (latestEpoch == null) {
            return null;
        }
        try {
            return latestEpoch.getLatest();
        } catch (EpochException e) {
            return null;
        }
    }

    /**
     * @return Return latest epoch or null if there is none
     */
    Epoch getLatestEpoch() {
        if (mysteryEpochs.size() < 1) {
            return null;
        }
        return mysteryEpochs.get(mysteryEpochs.size() - 1);
    }

    /**
     * @return The epoch list
     */
    List<Epoch> getMysteryEpochs() {
        return mysteryEpochs;
    }

    /**
     * Adds a new epoch of a given type and sets lastEpochEnd accordingly
     * @param t The new Epoch's type
     * @param p The new package
     */
    void addEpoch(Epoch.EpochType t, Package p) {
        if (mysteryEpochs.size() > 0) {
            this.lastEpochEnd = getLatestEpoch().getEndTime();
        }
        if (!pristine) {
            mysteryEpochs.add(new Epoch(t, p, lastEpochEnd));
            return;
        }
        mysteryEpochs.add(new Epoch(t, p));
        pristine = false;
    }

    /**
     * Deletes a number of epochs and returns a list of their packages
     * @param count Number of epochs to delete, starting with the most recent
     * @return A list of the epoch's packages
     */
    List<Package> mergeAndClearEpochs(int count) {
        List<Package> result = new LinkedList<>();
        for (int i = count - 1; i >= 0; i--) {
            result.addAll(0, mysteryEpochs.get(i).getPackages());
            mysteryEpochs.remove(i);
        }
        this.lastEpochEnd = result.get(result.size() - 1).getTimestamp();
        this.lastKnownPosition = result.get(result.size() - 1).position;
        this.lastPurge = this.lastEpochEnd;
        return result;
    }

    /**
     * @return The last known sensor position
     */
    public Position getLastKnownPosition() {
        return lastKnownPosition;
    }

    /**
     * Adds a checkpoint to the sensor if fresh enough
     * @param rendezVous The checkpoint candidate
     */
    void addRendezVous(RendezVous rendezVous) {
        if (rendezVous.getTimestamp() > lastPurge) {
            checkpoints.add(rendezVous);
        }
    }

    RendezVous getCheckpoint(long start, long end) {
        RendezVous mostRecent = null;
        LinkedList<RendezVous> junk = new LinkedList<>();

        for (var rdv : checkpoints) {
            if (end > rdv.getTimestamp()) {
                if (start < rdv.getTimestamp()) {
                    if (mostRecent == null) {
                        mostRecent = rdv;
                    } else if (mostRecent.getTimestamp() > rdv.getTimestamp()) {
                        junk.add(rdv);
                    } else {
                        junk.add(mostRecent);
                        mostRecent = rdv;
                    }
                } else {
                    junk.add(rdv);
                }
            }
        }

        checkpoints.removeAll(junk);
        checkpoints.remove(mostRecent);

        return mostRecent;
    }

    /**
     * @param timestampBound Method returns null if the contact is newer than timestampBound
     * @return The last relay contact id number or null if there was no last relay
     */
    Long getLastRelayContactId(long timestampBound) {
        for (int i = getMysteryEpochs().size() - 1; i >= 0; i--) {
            var e = getMysteryEpochs().get(i);
            var latestPackage = e.getLatest();
            if (latestPackage.getTimestamp() > timestampBound) return null;

            var relayCandidate = latestPackage.getStrongestRelay();
            if (relayCandidate != null) {
                return relayCandidate.getNodeId();
            }
        }

        if (lastKnownPosition != null) {
            if (lastKnownPosition.getDest() != null) return lastKnownPosition.getDest().getId();
            if (lastKnownPosition.getStart() != null) return lastKnownPosition.getStart().getId();
        }

        return null;
    }

    Long getLastRelayContactId() {
        return getLastRelayContactId(Long.MAX_VALUE);
    }

    /**
     * @return A list of RendezVous as checkpoints
     */
    List<RendezVous> getCheckpoints() {
        return checkpoints;
    }

    /**
     * @param checkpoint Checkpoint that is consumed by location
     */
    void useCheckpoint(RendezVous checkpoint) {
        checkpoints.remove(checkpoint);
    }
}
