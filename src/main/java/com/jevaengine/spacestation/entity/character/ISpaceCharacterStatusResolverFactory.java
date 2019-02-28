package com.jevaengine.spacestation.entity.character;

import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IStatusResolverFactory;
import io.github.jevaengine.world.scene.model.IActionSceneModel;

public interface ISpaceCharacterStatusResolverFactory extends IStatusResolverFactory {
    ISpaceCharacterStatusResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model);
}
