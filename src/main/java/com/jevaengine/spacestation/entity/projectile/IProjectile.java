package com.jevaengine.spacestation.entity.projectile;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.entity.IEntity;

public interface IProjectile extends IEntity {
    void setIgnore(IEntity ignore);
    IEntity getIgnore();
    void setTravelDirection(Vector3F direction);
    Vector3F getTravelDirection();
    float getSpeed();


    interface IProjectileObserver {
        void changedDirection();
        void changedIgnore();
    }
}
