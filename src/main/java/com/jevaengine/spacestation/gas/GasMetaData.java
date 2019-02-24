package com.jevaengine.spacestation.gas;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GasMetaData {
    private static final float GAS_CONSTANT = 8.31F;

    public final Map<GasType, Float> amount;

    public GasMetaData() {
        this(new HashMap<>());
    }

    public GasMetaData(Map<GasType, Float> amount) {
        this.amount = new HashMap<>(amount);

        for (Float f : this.amount.values()) {
            if (f < 0)
                throw new IllegalArgumentException();
        }
    }

    public void validate() {
        for (Float f : amount.values()) {
            if (f < 0 || Float.isNaN(f) || Float.isInfinite(f))
                throw new RuntimeException("Gas Metadata validation failed.");
        }
    }

    public GasMetaData(GasMetaData gas) {
        this.amount = new HashMap<>(gas.amount);
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

        return new GasMetaData(sum);
    }

    public GasMetaData add(GasType type, Float mols) {
        GasMetaData result = new GasMetaData(this);
        float current = result.amount.containsKey(type) ? result.amount.get(type) : 0;

        result.amount.put(type, current + mols);

        return result;
    }

    public GasMetaData consume(float contentsFraction) {
        HashMap<GasType, Float> sum = new HashMap<GasType, Float>();

        for (Map.Entry<GasType, Float> gas : amount.entrySet()) {
            sum.put(gas.getKey(), gas.getValue() * contentsFraction);
        }

        return new GasMetaData(sum);
    }

    public float getDensity() {
        float density = 0;

        for(Map.Entry<GasType, Float> e : this.amount.entrySet()) {
            density += (e.getValue() / getTotalMols()) * e.getKey().getDensity();
        }

        return density;
    }


    public float getMolarMass() {
        float mass = 0;

        for(Map.Entry<GasType, Float> e : this.amount.entrySet()) {
            mass += (e.getValue() / getTotalMols()) * e.getKey().getAtomicMass();
        }

        return mass;
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

    public float calculatePressure(float volume, float temperature) {
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
/*
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
    }*/
}