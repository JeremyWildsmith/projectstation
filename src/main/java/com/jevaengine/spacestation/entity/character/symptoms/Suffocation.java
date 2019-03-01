package com.jevaengine.spacestation.entity.character.symptoms;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;

import java.util.HashMap;
import java.util.Map;

public class Suffocation implements ISymptom {
    public static String NAME = "Suffocation";

    private final int INTERVAL = 2000;
    private final int PERIOD = 10000;

    private int last_impact = 0;
    private int affect_period = PERIOD;

    private final DamageSeverity severity;

    public Suffocation(DamageSeverity severity) {
        this.severity = severity;
    }

    public void elapseIneffective(int deltaTime) {
        if(affect_period <= 0)
            return;

        affect_period -= deltaTime;
        last_impact = (last_impact + deltaTime) % INTERVAL;
    }

    @Override
    public String getName() {
        return NAME;
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
        desc.put(DamageCategory.Suffocation, severity);

        return new DamageDescription(desc);
    }

    public boolean isGone() {
        return affect_period <= 0;
    }

    @Override
    public boolean isRecovery() {
        return false;
    }

    public boolean overrides(ISymptom symptom) {
        if(symptom instanceof RecoveringSuffocation)
            return true;
        if(!(symptom instanceof Suffocation))
            return false;

        return ((Suffocation)symptom).severity.compareTo(this.severity) <= 0;
    }

    public boolean tryConsume(ISymptom symptom) {
        if(!(symptom instanceof Suffocation))
            return false;

        Suffocation other = (Suffocation)symptom;

        if(other.severity == this.severity) {
            this.affect_period = PERIOD;
            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Suffocation";
    }

    @Override
    public DamageSeverity getSeverity() {
        return severity;
    }
}
