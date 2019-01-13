package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.DamageDescription;

public interface IDamageConsumer {
    void consume(DamageDescription damage);
}
