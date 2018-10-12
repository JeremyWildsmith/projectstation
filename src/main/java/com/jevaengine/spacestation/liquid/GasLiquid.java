package com.jevaengine.spacestation.liquid;

import com.jevaengine.spacestation.gas.GasType;

import java.util.Objects;

public class GasLiquid implements ILiquid {
    private GasType gasType;

    public GasLiquid(GasType type) {
        this.gasType = type;
    }

    public GasType getGasType() {
        return gasType;
    }

    @Override
    public float getRateOfFlow() {
        return gasType.getFlowRatio();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GasLiquid gasLiquid = (GasLiquid) o;
        return gasType == gasLiquid.gasType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gasType);
    }
}
