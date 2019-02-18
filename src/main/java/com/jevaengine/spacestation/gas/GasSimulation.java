package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import org.apache.commons.math3.linear.*;

import java.util.*;
import java.util.List;
import java.util.Queue;

public class GasSimulation implements IGasSimulation {

    private static final float ACTIVE_THRESHOLD = 0.001F;

    private static final float HEAT_CONDUCTIVITY_THRESHOLD = 0.01F;
    private static final float ACTIVE_GAS_GIVE_THRESHOLD = 0.00001F;
    private static final float ACTIVE_HEAT_NORMALIZE_THRESHOLD = 0.01F;

    private static final int ACTIVE_TIMEOUT = 10000;

    private static final float HEAT_FLOW_RATE = 0.4F;

    private static final float GAS_CONSTANT = 8.31F;

    private GasSimulationWorldMapReader world;

    private HashMap<Vector2D, IGasSimulationCluster> gasMappings = new HashMap<>();

    private HashMap<IGasSimulationCluster, Integer> activeLastPass = new HashMap<>();
    private Set<IGasSimulationCluster> processedLastPass = new HashSet<>();

    private Set<IGasSimulationCluster> queueDeactivate = new HashSet<>();
    private Set<IGasSimulationCluster> queueActivate = new HashSet<>();
    private Set<Vector2D> queueAsyncActivate = new HashSet<>();

    private final VoidGasSimulationCluster voidCluster;

    private final float defaultTemperature;

    public GasSimulation(GasSimulation sim) {
        this.defaultTemperature = sim.defaultTemperature;
        this.world = sim.world.duplicate();
        voidCluster = new VoidGasSimulationCluster(sim.world);

        //We need to duplicate gas mappings via deep copy, to prevent threading issues.
        HashMap<IGasSimulationCluster, IGasSimulationCluster> translation = new HashMap<>();

        for(IGasSimulationCluster c : sim.gasMappings.values()) {
            IGasSimulationCluster duplicate = c.duplicate(this.world, false);
            translation.put(c, duplicate);
        }

        for(Map.Entry<IGasSimulationCluster, IGasSimulationCluster> e : translation.entrySet()) {
            for(IGasSimulationCluster connection : e.getKey().getConnections()) {
                IGasSimulationCluster translated = translation.get(connection);

                if(translated != null)
                    e.getValue().connect(translated);
                else
                    throw new RuntimeException("Unaccounted for cluster. Error in simulation detected.");
            }
        }

        for(Map.Entry<Vector2D, IGasSimulationCluster> e : sim.gasMappings.entrySet()) {
            this.gasMappings.put(new Vector2D(e.getKey()), translation.get(e.getValue()));
        }

    }

    public GasSimulation(GasSimulationWorldMapReader world, float defaultTemperature) {
        this.world = world;
        this.defaultTemperature = defaultTemperature;
        voidCluster = new VoidGasSimulationCluster(world);
    }

    public HashMap<Vector2D, IGasSimulationCluster> getGasMap() {

        return new HashMap<>(gasMappings);
    }

    private IGasSimulationCluster updateSimulationCluster(Vector2D location, HashSet<IGasSimulationCluster> rebuilt) {
        if(gasMappings.containsKey(location)) {
            IGasSimulationCluster c = gasMappings.get(location);
            if(rebuilt.contains(c))
                return c;

            Map<Vector2D, GasMetaData> components = c.explode();

            for(Vector2D v : components.keySet()) {
                gasMappings.get(v).disconnect();
                gasMappings.remove(v);
            }
            c.disconnect();

            for(Map.Entry<Vector2D, GasMetaData> e : components.entrySet()) {
                produce(e.getKey(), e.getValue());
            }
        }

        return createSimulationCluster(location);
    }

    private IGasSimulationCluster createSimulationCluster(Vector2D location) {
        if(gasMappings.containsKey(location))
            return gasMappings.get(location);

        Set<IGasSimulationCluster> connections = new HashSet<>();
        Set<Vector2D> missingConnections = new HashSet<>();
        for (Direction d : Direction.HV_DIRECTIONS) {
            Vector2D v = location.add(d.getDirectionVector());
            if(world.isConnected(v, location)) {
                if (gasMappings.containsKey(v))
                    connections.add(gasMappings.get(v));
                else {
                    missingConnections.add(v);
                }
            }
        }

        IGasSimulationCluster ownerCluster = null;
        if(!world.isAirTight(location)) {
            voidCluster.addLocation(location);
            gasMappings.put(location, voidCluster);
            ownerCluster = voidCluster;
        } else {
            boolean joinedConnection = false;
            for(IGasSimulationCluster c : connections) {
                if(c.addLocation(location)) {
                    joinedConnection = true;
                    gasMappings.put(location, c);
                    ownerCluster = c;
                    c.produce(new GasMetaData());
                    break;
                }
            }

            if(!joinedConnection) {
                GasSimulationCluster c = new GasSimulationCluster(world, location, new GasMetaData());
                gasMappings.put(location, c);
                ownerCluster = c;
            }
        }

        for(IGasSimulationCluster c : connections) {
            if(c != ownerCluster) {
                c.connect(ownerCluster);
            }
        }

        for(Vector2D v : missingConnections) {
            if(!ownerCluster.isAirTight() && !world.isAirTight(v))
                continue;

            createSimulationCluster(v);
        }



        return ownerCluster;
    }

    public float getVolume(Vector2D location) {
        return world.getVolume(location);
    }

    public float getTemperature(Vector2D location) {
        return world.getTemperature(location);
    }

    public GasMetaData consume(Vector2D location, float mols) {
        if (Float.isNaN(mols))
            throw new IllegalArgumentException();

        if (!gasMappings.containsKey(location))
            return new GasMetaData();

        IGasSimulationCluster c = gasMappings.get(location);
        activeLastPass.put(c, 0);

        return c.consume(mols);
    }

    public void produce(Vector2D location, GasMetaData gas) {
        gas.validate();

        if (!world.isAirTight(location))
            return;

        if (!gasMappings.containsKey(location))
            createSimulationCluster(location);

        IGasSimulationCluster current = gasMappings.get(location);
        current.produce(gas);
        activeLastPass.put(current, 0);
    }

    public GasMetaData sample(Vector2D location) {
        if (!gasMappings.containsKey(location))
            return new GasMetaData();

        IGasSimulationCluster cluster = gasMappings.get(location);

        return cluster.getGas(location);
    }

    public GasSimulationWorldMapReader getReader() {
        return world.duplicate();
    }

    private float normalizeGasses(IGasSimulationCluster cluster, Set<IGasSimulationCluster> ignore, Queue<IGasSimulationCluster> toProcess, int deltaTime) {
        return cluster.normalizeGasses(ignore, toProcess, deltaTime);
    }

    private float calculateGive(IGasSimulationCluster source, IGasSimulationCluster dest) {

        float totalMols = source.getTotalGas().getTotalMols() + dest.getTotalGas().getTotalMols();

        if (totalMols <= 0)
            return 0;

        //Index as y,x
        double[][] coefficientMatrix = new double[2][2];
        double[] answerMatrix = new double[2];

        for (int i = 0; i < coefficientMatrix.length; i++) {
            coefficientMatrix[0][i] = 1;
        }

        answerMatrix[0] = totalMols;

        float sourceTemperatureVolumeRation = GAS_CONSTANT * source.getAverageTemperature() / source.getVolume();
        float destTemperatureVolumeRation = GAS_CONSTANT * dest.getAverageTemperature() / dest.getVolume();

        if (sourceTemperatureVolumeRation == 0 || destTemperatureVolumeRation == 0)
            return 0;

        coefficientMatrix[1][0] = sourceTemperatureVolumeRation;
        coefficientMatrix[1][1] = -destTemperatureVolumeRation;

        RealMatrix coefficients = new Array2DRowRealMatrix(coefficientMatrix);
        RealVector constants = new ArrayRealVector(answerMatrix);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector solution = solver.solve(constants);
        float giveRatio = (float) solution.getEntry(1) / totalMols;

        if(Float.isNaN(giveRatio))
            throw new RuntimeException("Give ratio is NaN");

        return giveRatio;
    }



    /*
    private Map<Direction, Float> calculateHeatDistRatios(Vector2D location, Set<Vector2D> ignore, int deltaTime) {
        GasMetaData initialGas = gasMappings.containsKey(location) ? gasMappings.get(location) : new GasMetaData(defaultTemperature);

        Map<Direction, GasMetaData> canidateDirections = new HashMap<>();
        for (Direction d : Direction.HV_DIRECTIONS) {
            Vector2D normDest = location.add(d.getDirectionVector());

            float conductivity = world.getHeatConductivity(normDest);

            if (conductivity < HEAT_CONDUCTIVITY_THRESHOLD)
                continue;

            GasMetaData cellValue = new GasMetaData(defaultTemperature);
            if (world.isAirTight(normDest)) {
                if (!gasMappings.containsKey(normDest))
                    gasMappings.put(normDest, new GasMetaData(defaultTemperature));

                cellValue = gasMappings.get(normDest);
            } else {
                gasMappings.remove(normDest);
            }

            if (cellValue.temperature > initialGas.temperature)
                continue;

            canidateDirections.put(d, cellValue);
        }

        float netTemperature = initialGas.temperature;

        if (netTemperature < ACTIVE_THRESHOLD)
            return new HashMap<>();

        Map<Direction, Float> distRatios = new HashMap<>();

        for (Map.Entry<Direction, GasMetaData> e : canidateDirections.entrySet()) {
            Vector2D normDest = location.add(e.getKey().getDirectionVector());
            float conductivity = world.getHeatConductivity(normDest);

            netTemperature += conductivity * (initialGas.temperature - e.getValue().temperature);
        }

        for (Map.Entry<Direction, GasMetaData> e : canidateDirections.entrySet()) {
            Vector2D normDest = location.add(e.getKey().getDirectionVector());
            float conductivity = world.getHeatConductivity(normDest);

            float tempDifference = conductivity * (initialGas.temperature - e.getValue().temperature);
            distRatios.put(e.getKey(), tempDifference / netTemperature);
        }

        return distRatios;

    }

    private float normalizeHeat(Vector2D location, Set<Vector2D> ignore, Queue<Vector2D> toProcess, int deltaTime) {
        GasMetaData current = this.gasMappings.get(location);

        if(current == null)
            return 0.0f;

        Map<Direction, Float> giveMappings = calculateHeatDistRatios(location, ignore, deltaTime);

        float totalGiveHeat = 0;
        float distributableHeatScaler = Math.min(1.0f, HEAT_FLOW_RATE * deltaTime / 1000.0f);

        for (Map.Entry<Direction, Float> e : giveMappings.entrySet()) {
            float giveHeatRatio = e.getValue() * distributableHeatScaler;

            totalGiveHeat += giveHeatRatio;

            Vector2D normDest = location.add(e.getKey().getDirectionVector());

            boolean destIsAirTight = world.isAirTight(normDest);

            if (destIsAirTight && !ignore.contains(normDest) && !toProcess.contains(normDest))
                toProcess.add(normDest);

            if (destIsAirTight) {
                GasMetaData destValue = gasMappings.containsKey(normDest) ? gasMappings.get(normDest) : new GasMetaData(defaultTemperature);
                destValue = destValue.add(current.consume(0, giveHeatRatio));
                gasMappings.put(normDest, destValue);
            }
        }

        gasMappings.put(location, current.consume(1, 1 - totalGiveHeat));

        return current.consume(0, totalGiveHeat).temperature;
    }*/

    private int normalize(IGasSimulationCluster origin, int deltaTime) {
        Queue<IGasSimulationCluster> toProcess = new LinkedList<>();
        HashSet<IGasSimulationCluster> processed = new HashSet<>();
        toProcess.add(origin);

        int cycles = 0;
        while (!toProcess.isEmpty()) {
            IGasSimulationCluster location = toProcess.remove();

            if (processed.contains(location))
                continue;

            processed.add(location);

            if (processedLastPass.contains(location))
                continue;

            cycles++;

            float totalDistMols = normalizeGasses(location, processed, toProcess, deltaTime);

            //normalizeHeat(location, processed, toProcess, deltaTime);

            if (totalDistMols > ACTIVE_THRESHOLD)
                queueActivate.add(location);
            else
                queueDeactivate.add(location);
        }

        processedLastPass.addAll(processed);
        return cycles;
    }

    public void syncGameLoop(Map<GasSimulationNetwork, GasSimulation> simulations) {
        Set<Vector2D> modifiedLocations = world.syncWithWorld(simulations);

        if (modifiedLocations.isEmpty())
            return;

        synchronized (queueAsyncActivate) {
            queueAsyncActivate.addAll(modifiedLocations);
        }
    }

    public int update(int delta) {
        synchronized (queueAsyncActivate) {

            HashSet<IGasSimulationCluster> rebuilt = new HashSet<>();
            for (Vector2D v : queueAsyncActivate) {
                IGasSimulationCluster rebuiltCluster = updateSimulationCluster(v, rebuilt);
                rebuilt.add(rebuiltCluster);

                for (IGasSimulationCluster s : gasMappings.values()) {
                    s.locationChanged(v);
                }

                if(gasMappings.containsKey(v))
                    activeLastPass.put(gasMappings.get(v), 0);
            }

            queueAsyncActivate.clear();
        }

        int cycles = 0;

        for (Map.Entry<GasSimulationNetwork.ConnectedLinkPair, GasSimulation> v : world.getLinks().entrySet()) {
            IGasSimulationCluster sourceCluster = v.getValue().createSimulationCluster(v.getKey().source);
            IGasSimulationCluster destCluster = createSimulationCluster(v.getKey().dest);

            GasMetaData total = sourceCluster.getTotalGas().add(destCluster.getTotalGas());

            float destGiveRatio = calculateGive(sourceCluster, destCluster);
            float sourceGiveRatio = 1.0f - destGiveRatio;

            sourceCluster.setTotalGas(total.consume(sourceGiveRatio));
            //v.getValue().gasMappings.put(v.getKey().source, total.consume(sourceGiveRatio, 0.5f));
            v.getValue().activeLastPass.put(sourceCluster, 0);

            destCluster.setTotalGas(total.consume(destGiveRatio));
            //gasMappings.put(v.getKey().dest, total.consume(destGiveRatio, 0.5f));
            activeLastPass.put(destCluster, 0);
        }

        processedLastPass.clear();
        List<IGasSimulationCluster> remove = new ArrayList<>();
        List<IGasSimulationCluster> stillAlive = new ArrayList<>();
        for (Map.Entry<IGasSimulationCluster, Integer> e : activeLastPass.entrySet()) {
            if (!processedLastPass.contains(e.getKey()))
                cycles += normalize(e.getKey(), delta);

            int newLife = e.getValue() + delta;
            if (newLife > ACTIVE_TIMEOUT)
                remove.add(e.getKey());
            else
                stillAlive.add(e.getKey());
        }

        for (IGasSimulationCluster v : stillAlive) {
            activeLastPass.put(v, activeLastPass.get(v) + delta);
        }

        for (IGasSimulationCluster v : queueActivate)
            activeLastPass.put(v, 0);

        queueActivate.clear();

        for (IGasSimulationCluster v : remove)
            activeLastPass.remove(v);

        for (IGasSimulationCluster v : queueDeactivate)
            activeLastPass.remove(v);

        queueDeactivate.clear();

        return cycles;
    }

    public void removedEntity(Vector3F loc) {
        Vector2D location = loc.getXy().round();

        //if (world.isAirTight(location))
        //    return;

        updateSimulationCluster(location, new HashSet<>());
        for(IGasSimulationCluster s : gasMappings.values()) {
            s.locationChanged(location);
        }
    }

    public Collection<IGasSimulationCluster> getClusters() {
        return gasMappings.values();
    }
}
