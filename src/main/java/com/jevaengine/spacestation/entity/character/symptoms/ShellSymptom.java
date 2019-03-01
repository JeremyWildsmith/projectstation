package com.jevaengine.spacestation.entity.character.symptoms;

import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;

public class ShellSymptom implements ISymptom {
    private final String name;
    private final String description;

    public ShellSymptom(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public void elapseIneffective(int deltaTime) {

    }

    @Override
    public DamageDescription getImpact(int deltaTime) {
        return new DamageDescription();
    }

    @Override
    public boolean overrides(ISymptom symptom) {
        return false;
    }

    @Override
    public boolean tryConsume(ISymptom symptom) {
        return false;
    }

    @Override
    public boolean isGone() {
        return false;
    }

    @Override
    public boolean isRecovery() {
        return false;
    }

    @Override
    public DamageSeverity getSeverity() {
        return DamageSeverity.None;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
