package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

public interface IGasSimulation {

    float getVolume(Vector2D location);

    GasSimulation.GasMetaData consume(Vector2D location, float mols);

    GasSimulation.GasMetaData sample(Vector2D location);

    void produce(Vector2D location, GasSimulation.GasMetaData gas);
}
