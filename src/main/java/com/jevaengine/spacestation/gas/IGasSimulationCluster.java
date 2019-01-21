package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

public interface IGasSimulationCluster {
    GasMetaData getGas(Vector2D location);

    boolean isAirTight();

    float calculatePressure();
    float getVolume();
    float getAverageTemperature();



    GasMetaData getTotalGas();
    void setTotalGas(GasMetaData gas);

    void connect(IGasSimulationCluster cluster);
    void disconnect(IGasSimulationCluster cluster);
    void disconnect();

    Map<Vector2D, GasMetaData> explode();

    void produce(GasMetaData gas);
    GasMetaData consume(float mols);

    boolean addLocation(Vector2D location);
    void removeLocation(Vector2D location);

    void locationChanged(Vector2D location);

    float normalizeGasses(Set<IGasSimulationCluster> ignore, Queue<IGasSimulationCluster> toProcess, int deltaTime);

    IGasSimulationCluster[] getConnections();
    Vector2D[] getLocations();

    GasSimulationWorldMapReader getWorld();


}
