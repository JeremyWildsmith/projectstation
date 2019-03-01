package com.jevaengine.spacestation.entity.character.symptoms;

import com.jevaengine.spacestation.DamageSeverity;

public interface ISymptomDetails {
    DamageSeverity getSeverity();
    String getName();
    String getDescription();
}
