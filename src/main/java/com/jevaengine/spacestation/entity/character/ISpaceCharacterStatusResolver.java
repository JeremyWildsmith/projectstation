package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import io.github.jevaengine.rpg.entity.character.IStatusResolver;

public interface ISpaceCharacterStatusResolver extends IStatusResolver, IImmutableSymptomBody {
    void addSymptom(ISymptom symtom);
    void removeSymptom(String name);
}
