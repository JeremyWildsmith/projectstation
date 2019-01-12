package com.jevaengine.spacestation.item;

import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;

public enum StationItemFunctionFactory {
    Construction("Construction", new ConstructionItemFunctionFactory()),
    Tool("Tool", new ToolItemFunctionFactory()),
    ProjectileWeapon("ProjectileWeapon", new ProjectileWeaponItemFunctionFactory()),
    BodyPart("BodyPart", new BodyPartItemFunctionFactory());

    private String name;
    private IItemFunctionFactory factory;

    StationItemFunctionFactory(String name, IItemFunctionFactory factory) {
        this.name = name;
        this.factory = factory;
    }

    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters) {
        return this.factory.create(itemFactory, entityFactory, modelFactory, parameters);
    }

    public String getName() {
        return name;
    }
}
