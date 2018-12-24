package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.*;
import io.github.jevaengine.world.Direction;
import org.apache.commons.math3.linear.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GasSimulation implements IGasSimulation {

    public static final int NORMALIZE_INTERVAL = 100;

    private static final float ACTIVE_THRESHOLD = 0.001F;

    private static final float HEAT_CONDUCTIVITY_THRESHOLD = 0.01F;
    private static final float ACTIVE_GAS_GIVE_THRESHOLD = 0.00001F;
    private static final float ACTIVE_HEAT_NORMALIZE_THRESHOLD = 0.01F;

    private static final int ACTIVE_TIMEOUT = 10000;

    private static final float HEAT_FLOW_RATE = 0.4F;

    private static final float GAS_CONSTANT = 8.31F;

    private WorldMapReader world;

    private HashMap<Vector2D, GasMetaData> gasMappings = new HashMap<>();

    private int sinceLastTick = 0;

    private HashMap<Vector2D, Integer> activeLastPass = new HashMap<>();
    private Set<Vector2D> processedLastPass = new HashSet<>();

    private Set<Vector2D> queueDeactivate = new HashSet<>();
    private Set<Vector2D> queueActivate = new HashSet<>();
    private Set<Vector2D> queueAsyncActivate = new HashSet<>();

    private final float defaultTemperature;

    public GasSimulation(GasSimulation sim) {
        this.defaultTemperature = sim.defaultTemperature;
        this.gasMappings = new HashMap<>(sim.gasMappings);
        this.world = sim.world.duplicate();
    }

    public GasSimulation(WorldMapReader world, float defaultTemperature) {
        this.world = world;
        this.defaultTemperature = defaultTemperature;
    }

    public HashMap<Vector2D, GasMetaData> getGasMap() {
        return new HashMap<>(gasMappings);
    }

    public float getVolume(Vector2D location) {
        return world.getVolume(location);
    }

    public GasMetaData consume(Vector2D location, float mols) {
        if (Float.isNaN(mols))
            throw new IllegalArgumentException();

        activeLastPass.put(location, 0);

        if (!gasMappings.containsKey(location))
            return new GasMetaData(defaultTemperature);

        GasMetaData current = gasMappings.get(location);


        float total = current.getTotalMols();

        if (total == 0)
            return new GasMetaData(0);

        float amountConsume = Math.min(1.0F, mols / total);

        GasMetaData consumed = current.consume(amountConsume, 0.0F);
        GasMetaData remaining = current.consume(1 - amountConsume, 1.0F);

        gasMappings.put(location, remaining);

        return consumed;
    }

    public void produce(Vector2D location, GasMetaData gas) {
        gas.validate();

        if (!world.isAirTight(location))
            return;

        if (!gasMappings.containsKey(location))
            gasMappings.put(location, new GasMetaData(defaultTemperature));

        GasMetaData current = gasMappings.get(location);
        gasMappings.put(location, current.add(gas));
        gasMappings.get(location).validate();
        activeLastPass.put(location, 0);
    }

    public GasMetaData sample(Vector2D location) {
        if (!gasMappings.containsKey(location))
            return new GasMetaData(defaultTemperature);
        gasMappings.get(location).validate();

        return new GasMetaData(gasMappings.get(location));
    }

    public WorldMapReader getReader() {
        return world.duplicate();
    }

    private float normalizeGasses(Vector2D location, Set<Vector2D> ignore, Queue<Vector2D> toProcess, int deltaTime) {
        GasMetaData initialGas = gasMappings.containsKey(location) ? gasMappings.get(location) : new GasMetaData(defaultTemperature);

        Map<Direction, GasMetaData> canidateDirections = new HashMap<>();
        for (Direction d : Direction.ALL_DIRECTIONS) {
            Vector2D normDest = location.add(d.getDirectionVector());

            if (!world.isConnected(location, normDest))
                continue;

            GasMetaData cellValue = new GasMetaData(defaultTemperature);
            if (world.isAirTight(normDest)) {
                if (!gasMappings.containsKey(normDest))
                    gasMappings.put(normDest, new GasMetaData(defaultTemperature));

                cellValue = gasMappings.get(normDest);
            } else {
                gasMappings.remove(normDest);
            }

            if (cellValue.calculatePressure(world.getVolume(normDest)) > initialGas.calculatePressure(world.getVolume(location))) {
                if(cellValue.getTotalMols() > ACTIVE_THRESHOLD)
                    toProcess.add(normDest);

                continue;
            }

            canidateDirections.put(d, cellValue);
        }

        if (canidateDirections.isEmpty() || initialGas.getTotalMols() < ACTIVE_THRESHOLD)
            return 0;

        GasMetaData totalGas = new GasMetaData(0);

        canidateDirections.put(Direction.Zero, initialGas);
        List<Map.Entry<Direction, GasMetaData>> entries = new ArrayList<>();
        for (Map.Entry<Direction, GasMetaData> e : canidateDirections.entrySet()) {
            totalGas = totalGas.add(e.getValue());
            entries.add(e);
        }

        //Index as y,x
        final int dim = entries.size();
        double[][] coefficientMatrix = new double[entries.size()][entries.size()];
        double[] answerMatrix = new double[entries.size()];
        for (int i = 0; i < coefficientMatrix.length; i++) {
            coefficientMatrix[0][i] = 1;
        }
        answerMatrix[0] = totalGas.getTotalMols();

        //We build the remaining of the matrix here...
        int row = 1;
        int col = 0;
        Iterator<Map.Entry<Direction, GasMetaData>> it = entries.iterator();
        Map.Entry<Direction, GasMetaData> last = null;
        while (it.hasNext()) {
            Map.Entry<Direction, GasMetaData> current = (last == null ? it.next() : last);

            if (!it.hasNext())
                break;

            Map.Entry<Direction, GasMetaData> next = it.next();
            last = next;

            Vector2D currentNormDest = location.add(current.getKey().getDirectionVector());
            float currentVolume = world.getVolume(currentNormDest);

            Vector2D nextNormDest = location.add(next.getKey().getDirectionVector());
            float nextVolume = world.getVolume(nextNormDest);

            if (currentVolume <= 0 || nextVolume <= 0)
                throw new IllegalArgumentException();

            float currentTemperatureVolumeRation = GAS_CONSTANT * current.getValue().temperature / currentVolume;
            float nextTemperatureVolumeRation = GAS_CONSTANT * next.getValue().temperature / nextVolume;
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
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Direction, GasMetaData> e = entries.get(i);
            Vector2D distLocation = location.add(e.getKey().getDirectionVector());

            if (e.getKey() != Direction.Zero)
                toProcess.add(distLocation);

            float amountNeeded = (float) solution.getEntry(i);

            molsDistributed += Math.abs(e.getValue().getTotalMols() - amountNeeded);
            GasMetaData result = e.getValue().consume(0, 1);
            result = result.add(totalGas.consume(amountNeeded / totalGas.getTotalMols(), 0));
            gasMappings.put(distLocation, result);


        }

        return molsDistributed;
    }

    private float calculateGive(GasMetaData source, float sourceVolume, GasMetaData dest, float destVolume) {

        float totalMols = source.getTotalMols() + dest.getTotalMols();

        if(totalMols <= 0)
            return 0;

        //Index as y,x
        double[][] coefficientMatrix = new double[2][2];
        double[] answerMatrix = new double[2];

        for (int i = 0; i < coefficientMatrix.length; i++) {
            coefficientMatrix[0][i] = 1;
        }

        answerMatrix[0] = totalMols;

        float sourceTemperatureVolumeRation = GAS_CONSTANT * source.temperature / sourceVolume;
        float destTemperatureVolumeRation = GAS_CONSTANT * dest.temperature / destVolume;

        if(sourceTemperatureVolumeRation == 0 || destTemperatureVolumeRation == 0)
            return 0;

        coefficientMatrix[1][0] = sourceTemperatureVolumeRation;
        coefficientMatrix[1][1] = -destTemperatureVolumeRation;

        RealMatrix coefficients = new Array2DRowRealMatrix(coefficientMatrix);
        RealVector constants = new ArrayRealVector(answerMatrix);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector solution = solver.solve(constants);

        float giveRatio = (float)solution.getEntry(1) / totalMols;
        return giveRatio;
    }


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
    }

    private int normalize(Vector2D origin, int deltaTime) {
        Queue<Vector2D> toProcess = new LinkedList<>();
        HashSet<Vector2D> processed = new HashSet<>();
        toProcess.add(origin);

        int cycles = 0;
        while (!toProcess.isEmpty()) {
            Vector2D location = toProcess.remove();

            if (processed.contains(location))
                continue;

            processed.add(location);

            if (processedLastPass.contains(location))
                continue;

            cycles++;

            float totalDistMols = normalizeGasses(location, processed, toProcess, deltaTime);

            normalizeHeat(location, processed, toProcess, deltaTime);

            if(totalDistMols > ACTIVE_THRESHOLD)
                queueActivate.add(location);
            else
                queueDeactivate.add(location);
        }

        processedLastPass.addAll(processed);
        return cycles;
    }

    public void syncGameLoop(Map<GasSimulationNetwork, GasSimulation> simulations) {
        Set<Vector2D> modifiedLocations = world.syncWithWorld(simulations);

        if(modifiedLocations.isEmpty())
            return;

        synchronized (queueAsyncActivate) {
            queueAsyncActivate.addAll(modifiedLocations);
        }
    }

    public int update(int delta) {
        synchronized (queueAsyncActivate) {
            for(Vector2D v : queueAsyncActivate)
                activeLastPass.put(v, 0);

            queueAsyncActivate.clear();
        }

        //Before we update, we copy over the shared simulation mappings:
        for (Map.Entry<GasSimulationNetwork.ConnectedLinkPair, GasSimulation> v : world.getLinks().entrySet()) {
            GasMetaData sourceGas = v.getValue().sample(v.getKey().source);
            GasMetaData destGas = sample(v.getKey().dest);

            GasMetaData total = sourceGas.add(destGas);

            float sourceVolume = v.getValue().world.getVolume(v.getKey().source);
            float destVolume = world.getVolume(v.getKey().dest);

            float destGiveRatio = calculateGive(sourceGas, sourceVolume, destGas, destVolume);
            float sourceGiveRatio = 1.0f - destGiveRatio;

            v.getValue().gasMappings.put(v.getKey().source, total.consume(sourceGiveRatio, 0.5f));
            v.getValue().activeLastPass.put(v.getKey().source, 0);

            gasMappings.put(v.getKey().dest, total.consume(destGiveRatio, 0.5f));
            activeLastPass.put(v.getKey().dest, 0);
        }

        int cycles = 0;

        sinceLastTick += delta;
        for (; sinceLastTick > NORMALIZE_INTERVAL; sinceLastTick -= NORMALIZE_INTERVAL) {
            processedLastPass.clear();
            List<Vector2D> remove = new ArrayList<>();
            List<Vector2D> stillAlive = new ArrayList<>();
            for (Map.Entry<Vector2D, Integer> e : activeLastPass.entrySet()) {
                if (!processedLastPass.contains(e.getKey()))
                    cycles += normalize(e.getKey(), NORMALIZE_INTERVAL);

                int newLife = e.getValue() + NORMALIZE_INTERVAL;
                if (newLife > ACTIVE_TIMEOUT)
                    remove.add(e.getKey());
                else
                    stillAlive.add(e.getKey());
            }

            for (Vector2D v : stillAlive) {
                activeLastPass.put(v, activeLastPass.get(v) + NORMALIZE_INTERVAL);
            }

            for (Vector2D v : queueActivate)
                activeLastPass.put(v, 0);

            queueActivate.clear();

            for (Vector2D v : remove)
                activeLastPass.remove(v);

            for (Vector2D v : queueDeactivate)
                activeLastPass.remove(v);

            queueDeactivate.clear();

        }

        return cycles;
    }

    public static class GasMetaData {
        public final Map<GasType, Float> amount;
        public final float temperature;

        public GasMetaData() {
            this(0);
        }

        public GasMetaData(float temperature) {
            this(new HashMap<>(), temperature);
        }

        public GasMetaData(Map<GasType, Float> amount, float temperature) {
            if (temperature < 0)
                throw new IllegalArgumentException();

            this.amount = new HashMap<>(amount);

            for (Float f : this.amount.values()) {
                if (f < 0)
                    throw new IllegalArgumentException();
            }

            this.temperature = temperature;
        }

        public void validate() {
            for (Float f : amount.values()) {
                if (f < 0 || Float.isNaN(f))
                    throw new RuntimeException("Gas Metadata validation failed.");
            }

            if (temperature < 0 || Float.isNaN(temperature))
                throw new RuntimeException("Gas Metadata validation failed.");
        }

        public GasMetaData(GasMetaData gas) {
            this.amount = new HashMap<>(gas.amount);
            this.temperature = gas.temperature;
        }

        public GasMetaData add(GasMetaData g) {
            HashMap<GasType, Float> sum = new HashMap<GasType, Float>();

            for (Map.Entry<GasType, Float> gas : g.amount.entrySet()) {
                sum.put(gas.getKey(), gas.getValue());
            }

            for (Map.Entry<GasType, Float> gas : this.amount.entrySet()) {
                float current = sum.containsKey(gas.getKey()) ? sum.get(gas.getKey()) : 0;
                sum.put(gas.getKey(), current + gas.getValue());
            }

            return new GasMetaData(sum, g.temperature + this.temperature);
        }

        public GasMetaData add(GasType type, Float mols) {
            GasMetaData result = new GasMetaData(this);
            float current = result.amount.containsKey(type) ? result.amount.get(type) : 0;

            result.amount.put(type, current + mols);

            return result;
        }

        public GasMetaData consume(float contentsFraction) {
            return consume(contentsFraction, contentsFraction);
        }

        public GasMetaData consume(float contentsFraction, float temperatureFraction) {
            HashMap<GasType, Float> sum = new HashMap<GasType, Float>();

            for (Map.Entry<GasType, Float> gas : amount.entrySet()) {
                sum.put(gas.getKey(), gas.getValue() * contentsFraction);
            }

            return new GasMetaData(sum, temperature * temperatureFraction);
        }

        public float getTotalMols() {
            float total = 0;

            for (Float f : amount.values())
                total += f;

            return total;
        }

        public float getFlowRate() {
            if (amount.isEmpty())
                return 0;

            float totalQuantity = getTotalMols();

            float flowRate = 0;

            for (Map.Entry<GasType, Float> e : amount.entrySet()) {
                flowRate += e.getKey().getFlowRatio() * (e.getValue() / totalQuantity);
            }

            return flowRate;
        }

        public float calculatePressure(float volume) {
            if (volume <= 0.0001f)
                throw new IllegalArgumentException();

            float pressure = getTotalMols() * GAS_CONSTANT * temperature / volume;
            return pressure;
        }

        public float getPercentContent(GasType g) {
            float quantity = amount.containsKey(g) ? amount.get(g) : 0;
            float totalMols = getTotalMols();

            if (totalMols <= 0)
                return 0;

            return quantity / totalMols;
        }

        public float getPercentContent(GasType g, boolean airBorne, float volume) {
            float quantity = amount.containsKey(g) ? amount.get(g) : 0;
            float totalMols = 0;

            float pressure = calculatePressure(volume);

            for(Map.Entry<GasType, Float> a : amount.entrySet())
            {
                if(a.getKey().isAirborne(pressure) == airBorne)
                    totalMols += a.getValue();
            }

            if (totalMols <= 0)
                return 0;

            return quantity / totalMols;
        }

        public float getPercentColour(float volume) {
            float totalMols = getTotalMols();
            float coloured = 0;

            if(totalMols <= 0)
                return 0;

            float pressure = calculatePressure(volume);
            for(Map.Entry<GasType, Float> a : amount.entrySet())
            {
                if(a.getKey().getColor(a.getValue(), pressure) != null)
                    coloured += a.getValue();
            }

            return coloured / totalMols;
        }

        public float getPercentAirborne(boolean isAirborne, float volume) {
            float totalMols = getTotalMols();
            float airBorne = 0;

            if (totalMols <= 0)
                return 0;

            float pressure = calculatePressure(volume);
            for(Map.Entry<GasType, Float> g : amount.entrySet())
            {
                if(g.getKey().isAirborne(pressure))
                    airBorne += g.getValue();
            }

            if(isAirborne)
                return airBorne / totalMols;
            else
                return (1f - airBorne / totalMols);
        }

        public Color getColor(float volume) {
            if (amount.isEmpty())
                return null;

            float pressure = calculatePressure(volume);

            float totalQuantity = getTotalMols();

            int r = 0;
            int g = 0;
            int b = 0;
            int a = 0;
            boolean coloured = false;
            for (Map.Entry<GasType, Float> e : amount.entrySet()) {
                float ratio = (e.getValue() / totalQuantity);

                Color c = e.getKey().getColor(e.getValue(), pressure);
                if(c != null)
                {
                    coloured = true;
                    r += c.getRed() * ratio;
                    g += c.getGreen() * ratio;
                    b += c.getBlue() * ratio;
                    a += c.getAlpha() * ratio;
                }
            }

            if(!coloured)
                return null;

            return new Color(r, g, b, a);
        }
    }

    public void removedEntity(Vector3F loc) {
        Vector2D location = loc.getXy().round();

        if (world.isAirTight(location))
            return;

        for (Direction d : Direction.ALL_DIRECTIONS) {
            Vector2D v = location.add(d.getDirectionVector());

            if (world.isConnected(location, v) && world.isAirTight(v)) {
                if (!gasMappings.containsKey(location))
                    gasMappings.put(location, new GasMetaData(defaultTemperature));

                activeLastPass.put(location, 0);

                return;
            }
        }
    }

    public interface WorldMapReader {
        float getHeatConductivity(Vector2D location);

        boolean isConnected(Vector2D locationA, Vector2D locationB);

        boolean isAirTight(Vector2D location);

        Set<Vector2D> syncWithWorld(Map<GasSimulationNetwork, GasSimulation> simulations);

        float getVolume(Vector2D location);

        WorldMapReader duplicate();

        Map<GasSimulationNetwork.ConnectedLinkPair, GasSimulation> getLinks();
    }
}
