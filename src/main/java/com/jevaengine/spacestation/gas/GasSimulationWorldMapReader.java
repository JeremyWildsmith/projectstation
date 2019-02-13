package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

import java.util.Map;
import java.util.Set;

public interface GasSimulationWorldMapReader {
    float getHeatConductivity(Vector2D location);

    boolean isConnected(Vector2D locationA, Vector2D locationB);

    boolean isAirTight(Vector2D location);

    Set<Vector2D> syncWithWorld(Map<GasSimulationNetwork, GasSimulation> simulations);

    float getVolume(Vector2D location);

    GasSimulationWorldMapReader duplicate();

    Map<GasSimulationNetwork.ConnectedLinkPair, GasSimulation> getLinks();

    float getTemperature(Vector2D location);
}
