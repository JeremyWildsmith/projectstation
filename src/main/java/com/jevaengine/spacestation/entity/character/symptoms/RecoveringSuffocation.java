package com.jevaengine.spacestation.entity.character.symptoms;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;

import java.util.HashMap;
import java.util.Map;

public class RecoveringSuffocation implements ISymptom {
    private final int INTERVAL = 2000;
    private final int PERIOD = 10000;

    private int last_impact = 0;
    private int affect_period = PERIOD;

    public void elapseIneffective(int deltaTime) {
        if(affect_period <= 0)
            return;

        affect_period -= deltaTime;
        last_impact = (last_impact + deltaTime) % INTERVAL;
    }

    @Override
    public String getName() {
        return "Recovering Suffocation";
    }

    @Override
    public DamageDescription getImpact(int deltaTime) {
        if(affect_period <= 0)
            return new DamageDescription();

        last_impact += deltaTime;
        affect_period -= deltaTime;

        if(last_impact < INTERVAL)
            return new DamageDescription();

        last_impact -= INTERVAL;

        Map<DamageCategory, DamageSeverity> desc = new HashMap<>();
        desc.put(DamageCategory.Suffocation, DamageSeverity.Minor);

        return new DamageDescription(desc);
    }

    public boolean isGone() {
        return affect_period <= 0;
    }

    @Override
    public boolean isRecovery() {
        return true;
    }

    public boolean overrides(ISymptom symptom) {
        return false;
    }

    public boolean tryConsume(ISymptom symptom) {
        if(!(symptom instanceof RecoveringSuffocation))
            return false;

        this.affect_period = PERIOD;
        return true;
    }

    @Override
    public String getDescription() {
        return "Recovering Suffocation";
    }

    @Override
    public DamageSeverity getSeverity() {
        return DamageSeverity.Minor;
    }
}
