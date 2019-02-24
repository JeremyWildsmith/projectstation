package com.jevaengine.spacestation.entity.character.symptoms;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;

import java.util.Map;

public interface ISymptom extends ISymptomDetails {
    void elapseIneffective(int deltaTime);
    DamageDescription getImpact(int deltaTime);
    boolean overrides(ISymptom symptom);
    boolean tryConsume(ISymptom symptom);
    boolean isGone();
    boolean isRecovery();
}
