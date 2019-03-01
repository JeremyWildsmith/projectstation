package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import io.github.jevaengine.rpg.entity.character.IStatusResolver;

public interface ISpaceCharacterStatusObserver extends IStatusResolver.IStatusResolverObserver {
    void affectedBySymptom(ISymptomDetails symptom);
    void lostSymptom(ISymptomDetails symptom);
    void effectiveHitpointsChanged(float newHitpoints);
}
