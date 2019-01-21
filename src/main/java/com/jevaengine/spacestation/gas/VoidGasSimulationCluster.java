package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

import java.util.*;

public class VoidGasSimulationCluster implements IGasSimulationCluster {
    public static final float VOID_TEMPERATURE = 150;

    private final GasSimulationWorldMapReader world;

    private final HashSet<IGasSimulationCluster> connections = new HashSet<>();

    private final HashSet<Vector2D> locations = new HashSet<>();

    public VoidGasSimulationCluster(GasSimulationWorldMapReader world) {
        this.world = world;
    }

    @Override
    public void produce(GasMetaData gas) {

    }

    @Override
    public GasMetaData consume(float mols) {
        return new GasMetaData();
    }

    @Override
    public boolean addLocation(Vector2D location) {
        locations.add(location);
        return true;
    }

    @Override
    public void removeLocation(Vector2D location) {
        locations.remove(location);
    }

    @Override
    public GasMetaData getTotalGas() {
        return new GasMetaData(VOID_TEMPERATURE);
    }

    @Override
    public void setTotalGas(GasMetaData gas) { }

    @Override
    public boolean isAirTight() {
        return false;
    }

    @Override
    public float calculatePressure() {
        return 0;
    }

    @Override
    public float getVolume() {
        return 100000000;
    }

    @Override
    public void connect(IGasSimulationCluster cluster) {
        connections.add(cluster);
    }

    @Override
    public void disconnect(IGasSimulationCluster cluster) {
        connections.remove(cluster);
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
    public Map<Vector2D, GasMetaData> explode() {
        HashMap<Vector2D, GasMetaData> g = new HashMap<>();

        for(Vector2D v : locations) {
            g.put(v, new GasMetaData(VOID_TEMPERATURE));
        }

        return g;
    }

    @Override
    public void locationChanged(Vector2D location) {
    }

    @Override
    public float normalizeGasses(Set<IGasSimulationCluster> ignore, Queue<IGasSimulationCluster> toProcess, int deltaTime) {
        return 0;
    }

    @Override
    public IGasSimulationCluster[] getConnections() {
        return new IGasSimulationCluster[0];
    }

    @Override
    public Vector2D[] getLocations() {
        return new Vector2D[0];
    }

    @Override
    public GasSimulationWorldMapReader getWorld() {
        return world;
    }

    @Override
    public GasMetaData getGas(Vector2D location) {
        return getTotalGas();
    }

    @Override
    public float getAverageTemperature() {
        return VOID_TEMPERATURE;
    }
}
