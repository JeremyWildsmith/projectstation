package com.jevaengine.spacestation.item;

import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.entity.IEntityFactory;

public enum StationItemFunctionFactory {
    Construction("Construction", new ConstructionItemFunctionFactory()),
    Tool("Tool", new ToolItemFunctionFactory());

    private String name;
    private IItemFunctionFactory factory;

    StationItemFunctionFactory(String name, IItemFunctionFactory factory) {
        this.name = name;
        this.factory = factory;
    }

    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, IImmutableVariable parameters) {
        return this.factory.create(itemFactory, entityFactory, parameters);
    }

    public String getName() {
        return name;
    }
}
