package de.haug.sensor_location;

import java.util.LinkedList;
import java.util.List;

public class Sensor extends Node {
    public List<Epoch> mysteryEpochs;
    public long lastEpochEnd = 0;
    private boolean pristine = true;
    private Position lastKnownPosition = null;
    private List<RendezVous> checkpoints;
    private long lastPurge = Long.MIN_VALUE;

    public Sensor(long id) throws Exception {
        super(id);
        checkpoints = new LinkedList<>();
        mysteryEpochs = new LinkedList<>();
        if (!Node.isSensor(id)) {
            throw new Exception("Id does not match Sensor status");
        }
    }

    public Package getLastPackage() {
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

    public Epoch getLatestEpoch() {
        if (mysteryEpochs.size() < 1) {
            return null;
        }
        return mysteryEpochs.get(mysteryEpochs.size() - 1);
    }

    public List<Epoch> getMysteryEpochs() {
        return mysteryEpochs;
    }

    public void addEpoch(Epoch.EpochType t, Package p) throws EpochException {
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

    public List<Package> mergeAndClearEpochs(int count) {
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

    public Position getLastKnownPosition() {
        return lastKnownPosition;
    }

    public void addRendezVous(RendezVous rendezVous) {
        if (rendezVous.getTimestamp() > lastPurge) {
            checkpoints.add(rendezVous);
        }
    }

    public List<RendezVous> getCheckpoints() {
        return checkpoints;
    }

    public void useCheckpoint(RendezVous checkpoint) {
        checkpoints.remove(checkpoint);
    }
}
