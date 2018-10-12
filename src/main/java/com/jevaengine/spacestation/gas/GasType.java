package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2F;

import java.util.NoSuchElementException;

public enum GasType {
    Oxygen("O", 0.7F, 15.999F),
    Heat("Heat", 0.3F, 0),
    CarbonDioxide("CO2", 0.7F, 44.09F);

    private final String name;
    private final float flowRatio;
    private final float atomicMass;

    GasType(String name, float flowRatio, float atomicMass) {
        this.name = name;
        this.flowRatio = flowRatio;
        this.atomicMass = atomicMass;
    }

    public String getName() {
        return name;
    }

    public float getFlowRatio() {
        return flowRatio;
    }

    public float getAtomicMass() {
        return atomicMass;
    }

    public boolean isFakeGas() {
        return atomicMass < Vector2F.TOLERANCE;
    }

    public static GasType fromName(String name) {
        for(GasType t : GasType.values()) {
            if(t.getName().compareTo(name) == 0)
                return t;
        }

        throw new NoSuchElementException();
    }
}
