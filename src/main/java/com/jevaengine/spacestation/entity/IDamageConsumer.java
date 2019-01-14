package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.DamageDescription;
import io.github.jevaengine.world.entity.IEntity;

public interface IDamageConsumer extends IEntity {
    void consume(DamageDescription damage);
}
