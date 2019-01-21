package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.world.Direction;
import org.apache.commons.math3.linear.*;

import java.util.*;

public class GasSimulationCluster implements IGasSimulationCluster {
    private static final float ACTIVE_THRESHOLD = 0.001F;
    private static final float GAS_CONSTANT = 8.31F;
    private final Set<Vector2D> locations = new HashSet<>();
    private final Set<IGasSimulationCluster> connections = new HashSet<>();
    private static final int MAX_CLUSTER_SIZE = 5;

    private final GasSimulationWorldMapReader world;
    private GasMetaData gas;
    private float volume = 0;

    private boolean isAirTight = false;

    public GasSimulationCluster(GasSimulationWorldMapReader world, Vector2D location, GasMetaData gas) {
        this.world = world;
        locations.add(location);
        this.gas = new GasMetaData(gas);
        this.gas.validate();
        recalculateProperties();
    }

    private GasSimulationCluster(GasSimulationWorldMapReader world) {
        this.world = world;
    }

    @Override
    public float getAverageTemperature() {
        if(locations.isEmpty())
            return 0;

        return gas.temperature * 1.0f / locations.size();
    }

    private void recalculateProperties() {
        isAirTight = true;
        for(Vector2D v : locations) {
            volume += world.getVolume(v);

            if(!world.isAirTight(v))
                isAirTight = false;
        }
    }

    public boolean isAirTight() {
        return isAirTight;
    }

    public float calculatePressure() {
        if(volume <= 0)
            return 0;

        return gas.calculatePressure(volume);
    }

    @Override
    public boolean addLocation(Vector2D location) {
        if(locations.size() >= MAX_CLUSTER_SIZE)
            return false;

        locations.add(location);
        recalculateProperties();
        return true;
    }

    @Override
    public void removeLocation(Vector2D location) {
        locations.remove(location);
        recalculateProperties();
    }

    public float getVolume() {
        return volume;
    }

    public void locationChanged(Vector2D location) {
        if(locations.contains(location)) {
            recalculateProperties();
        }
    }


    public GasMetaData getTotalGas() {
        return new GasMetaData(gas);
    }

    public void setTotalGas(GasMetaData gas) {
        this.gas = new GasMetaData(gas);
    }

    public GasMetaData getGas(Vector2D location) {
        if(!locations.contains(location))
            throw new RuntimeException("Location does not exist in this cluster.");

        return gas.consume(1.0f / locations.size(), 1.0f / locations.size());
    }

    @Override
    public Map<Vector2D, GasMetaData> explode() {
        Map<Vector2D, GasMetaData> c = new HashMap<>();

        if(locations.isEmpty())
            return new HashMap<>();

        float portion = 1.0f / locations.size();

        for(Vector2D l : locations) {
            c.put(l, gas.consume(portion));
        }

        return c;
    }

    @Override
    public void disconnect() {
        Set<IGasSimulationCluster> oldConnections = new HashSet<>(connections);
        for(IGasSimulationCluster c : oldConnections) {
            c.disconnect(this);
        }

        connections.clear();
    }

    @Override
    public void connect(IGasSimulationCluster cluster) {
        if(cluster == this || connections.contains(cluster))
            return;

        connections.add(cluster);
        cluster.connect(this);
    }

    @Override
    public void disconnect(IGasSimulationCluster cluster) {
        if(!connections.contains(cluster))
            return;

        connections.remove(cluster);
        cluster.disconnect(this);
    }

    @Override
    public IGasSimulationCluster[] getConnections() {
        return connections.toArray(new IGasSimulationCluster[connections.size()]);
    }

    @Override
    public Vector2D[] getLocations() {
        return locations.toArray(new Vector2D[locations.size()]);
    }

    @Override
    public GasSimulationWorldMapReader getWorld() {
        return world;
    }

    public float normalizeGasses(Set<IGasSimulationCluster> ignore, Queue<IGasSimulationCluster> toProcess, int deltaTime) {
        GasMetaData initialGas = gas;

        List<IGasSimulationCluster> canidateClusters = new ArrayList<>();
        for (IGasSimulationCluster c : connections) {
            if (c.calculatePressure() > this.calculatePressure()) {
                if (c.getTotalGas().getTotalMols() > ACTIVE_THRESHOLD)
                    toProcess.add(c);
            } else
                canidateClusters.add(c);
        }

        if (canidateClusters.isEmpty() || initialGas.getTotalMols() < ACTIVE_THRESHOLD)
            return 0;

        GasMetaData totalGas = new GasMetaData(0);

        canidateClusters.add(this);
        for (IGasSimulationCluster e : canidateClusters) {
            totalGas = totalGas.add(e.getTotalGas());
        }

        //Index as y,x
        double[][] coefficientMatrix = new double[canidateClusters.size()][canidateClusters.size()];
        double[] answerMatrix = new double[canidateClusters.size()];
        for (int i = 0; i < coefficientMatrix.length; i++) {
            coefficientMatrix[0][i] = 1;
        }
        answerMatrix[0] = totalGas.getTotalMols();

        //We build the remaining of the matrix here...
        int row = 1;
        int col = 0;
        Iterator<IGasSimulationCluster> it = canidateClusters.iterator();
        IGasSimulationCluster last = null;
        while (it.hasNext()) {
            IGasSimulationCluster current = (last == null ? it.next() : last);

            if (!it.hasNext())
                break;

            IGasSimulationCluster next = it.next();
            last = next;

            float currentVolume = current.getVolume();
            float nextVolume = next.getVolume();

            if (currentVolume <= 0 || nextVolume <= 0)
                throw new IllegalArgumentException();

            float currentTemperatureVolumeRation = GAS_CONSTANT * current.getTotalGas().temperature / currentVolume;
            float nextTemperatureVolumeRation = GAS_CONSTANT * next.getTotalGas().temperature / nextVolume;
            coefficientMatrix[row][col] = currentTemperatureVolumeRation;
            coefficientMatrix[row][col + 1] = -nextTemperatureVolumeRation;
            col++;
            row++;
        }

        RealMatrix coefficients = new Array2DRowRealMatrix(coefficientMatrix);
        RealVector constants = new ArrayRealVector(answerMatrix);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector solution = solver.solve(constants);

        float molsDistributed = 0;
        for (int i = 0; i < canidateClusters.size(); i++) {
            IGasSimulationCluster e = canidateClusters.get(i);

            if (e != this)
                toProcess.add(e);

            float amountNeeded = (float) solution.getEntry(i);

            molsDistributed += Math.abs(e.getTotalGas().getTotalMols() - amountNeeded);

            GasMetaData result = e.getTotalGas().consume(0, 1);
            result = result.add(totalGas.consume(amountNeeded / totalGas.getTotalMols(), 0));
            e.setTotalGas(result);
        }

        return molsDistributed;
    }

    public GasMetaData consume(float mols) {
        GasMetaData current = gas;


        float total = current.getTotalMols();

        if (total == 0)
            return new GasMetaData();

        float amountConsume = Math.min(1.0F, mols / total);

        GasMetaData consumed = current.consume(amountConsume, 1.0F);
        gas = current.consume(1 - amountConsume, 1.0F);

        gas.validate();

        return consumed;
    }

    public void produce(GasMetaData gas) {
        gas.validate();

        this.gas = this.gas.add(gas);
        this.gas.validate();
    }
}
