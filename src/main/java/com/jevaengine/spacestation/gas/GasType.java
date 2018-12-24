package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.util.Nullable;

import java.awt.*;
import java.util.NoSuchElementException;

public enum GasType {
    Oxygen("O", 1000F, 15.999F, true),
    Nitrogen("N", 1000F, 15.999F, true),
    CarbonDioxide("CO2", 0.7F, 44.09F, true),
    Water("H2O", 0.7F, 18.02F, false, new Color(44,145,195), 1, 30, 20000, 0.1f);

    private final String name;
    private final float flowRatio;
    private final float atomicMass;

    private Color baseColor = null;
    private float minColourMols = 0;
    private float maxColourMols = 0;
    private float opacityVariance = 0;
    private float liquidPressure = 0;
    private final boolean isAirborne;

    GasType(String name, float flowRatio, float atomicMass, boolean isAirborne) {
        this.name = name;
        this.flowRatio = flowRatio;
        this.atomicMass = atomicMass;
        this.isAirborne = isAirborne;
    }

    GasType(String name, float flowRatio, float atomicMass, boolean isAirborne, Color color, float minColourMols, float maxColourMols, float liquidPressure, float opacityVariance) {
        this.name = name;
        this.flowRatio = flowRatio;
        this.atomicMass = atomicMass;
        this.baseColor = color;
        this.minColourMols = minColourMols;
        this.maxColourMols = maxColourMols;
        this.opacityVariance = opacityVariance;
        this.isAirborne = isAirborne;
        this.liquidPressure = liquidPressure;
    }

    public boolean isAirborne(float pressure) {
        if(liquidPressure != 0 && pressure < liquidPressure)
            return true;

        return isAirborne;
    }

    @Nullable
    public Color getColor(float mols, float pressure) {
        if(baseColor == null || mols < minColourMols || pressure <= liquidPressure)
            return null;

        float colourRatio = Math.min(1.0f, (mols - minColourMols) / (maxColourMols - minColourMols));

        int opacity = Math.round(colourRatio * 200);
        opacity = (int)Math.round(opacity * (1 - opacityVariance) + (Math.random() * opacity * opacityVariance));
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);
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
