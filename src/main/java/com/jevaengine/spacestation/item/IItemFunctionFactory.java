package com.jevaengine.spacestation.item;

import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;

public interface IItemFunctionFactory {
    IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters);
}
