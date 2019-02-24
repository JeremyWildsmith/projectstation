package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;

import java.util.List;

public interface IImmutableSymptomBody {
    List<ISymptomDetails> getSymptoms();
}
