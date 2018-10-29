package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.util.Nullable;

import java.awt.*;
import java.util.NoSuchElementException;

public enum GasType {
    Oxygen("O", 1000F, 15.999F),
    Nitrogen("N", 1000F, 15.999F),
    CarbonDioxide("CO2", 0.7F, 44.09F),
    Water("H2O", 0.7F, 18.02F, new Color(44,145,195), 1, 30, 0.1f);

    private final String name;
    private final float flowRatio;
    private final float atomicMass;

    private Color baseColor = null;
    private float minColourMols = 0;
    private float maxColourMols = 0;
    private float opacityVariance = 0;

    GasType(String name, float flowRatio, float atomicMass) {
        this.name = name;
        this.flowRatio = flowRatio;
        this.atomicMass = atomicMass;
    }

    GasType(String name, float flowRatio, float atomicMass, Color color, float minColourMols, float maxColourMols, float opacityVariance) {
        this.name = name;
        this.flowRatio = flowRatio;
        this.atomicMass = atomicMass;
        this.baseColor = color;
        this.minColourMols = minColourMols;
        this.maxColourMols = maxColourMols;
        this.opacityVariance = opacityVariance;
    }

    @Nullable
    public Color getColor(float mols) {
        if(baseColor == null || mols < minColourMols)
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
