package com.jevaengine.spacestation.entity.projectile;

import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.*;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LaserProjectile implements IProjectile {
    private static final AtomicInteger m_unnamedCount = new AtomicInteger(0);

    private final int m_maxLife;

    private int m_life = 0;

    private final String m_name;
    @Nullable
    private final PhysicsBodyDescription m_physicsBodyDescription;
    private final Observers m_observers = new Observers();
    private final EntityBridge m_bridge;

    @Nullable
    private final ISceneModel m_model;
    private IPhysicsBody m_body = new NullPhysicsBody();
    @Nullable
    private World m_world;

    private HashMap<String, Integer> m_flags = new HashMap<>();

    private final float m_speed;
    private Vector3F m_direction;

    private IEntity m_ignore = null;

    public LaserProjectile(ISceneModel model, float speed, int maxLife) {
        m_name = this.getClass().getName() + m_unnamedCount.getAndIncrement();
        m_model = model;
        m_maxLife = maxLife;
        m_speed = speed;
        m_direction = new Vector3F();
        m_physicsBodyDescription = new PhysicsBodyDescription(PhysicsBodyDescription.PhysicsBodyType.Dynamic, model.getBodyShape(), 1.0F, true, true, 1.0F);
        m_physicsBodyDescription.collisionExceptions = new Class[] {
                LaserProjectile.class
        };

        m_bridge = new EntityBridge(this);
    }

    public void setIgnore(IEntity ignore) {
        m_ignore = ignore;
        m_observers.raise(IProjectileObserver.class).changedIgnore();
    }

    public IEntity getIgnore() {
        return m_ignore;
    }

    public float getSpeed() {
        return m_speed;
    }

    public void setTravelDirection(Vector3F direction) {
        if(!direction.isZero()) {
            m_direction = direction.normalize();
        } else
            m_direction = new Vector3F();

        m_observers.raise(IProjectileObserver.class).changedDirection();
    }

    public Vector3F getTravelDirection() {
        return m_direction;
    }

    @Override
    public void dispose() {
        if (m_world != null)
            m_world.removeEntity(this);

        m_observers.clear();
    }

    @Override
    public String getInstanceName() {
        return m_name;
    }

    @Override
    public final World getWorld() {
        return m_world;
    }

    @Override
    public final void associate(World world) {
        if (m_world != null)
            throw new WorldAssociationException("Already associated with world");

        m_world = world;

        constructPhysicsBody();
        m_observers.raise(IEntityWorldObserver.class).enterWorld();
    }

    @Override
    public final void disassociate() {
        if (m_world == null)
            throw new WorldAssociationException("Not associated with world");

        m_observers.raise(IEntityWorldObserver.class).leaveWorld();

        destroyPhysicsBody();

        m_world = null;
    }

    private void constructPhysicsBody() {
        Direction dir = m_body.getDirection();
        if (m_physicsBodyDescription == null)
            m_body = new NonparticipantPhysicsBody(this, m_model.getAABB());
        else {
            m_body = m_world.getPhysicsWorld().createBody(this, m_physicsBodyDescription);
            m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
        }

        m_body.getObservers().add(new ContactObserver());

        m_body.setDirection(dir);
    }

    private void destroyPhysicsBody() {
        Direction dir = m_body.getDirection();

        m_body.destory();
        m_body = new NullPhysicsBody();
        m_body.setDirection(dir);
        m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public final IPhysicsBody getBody() {
        return m_body;
    }

    @Override
    public void update(int deltaTime) {
        m_life += deltaTime;

        if (m_life < m_maxLife) {
            m_model.update(deltaTime);

            if(!m_direction.isZero())
                m_body.setLinearVelocity(m_direction.multiply(m_speed));
        } else
            m_world.removeEntity(this);
    }

    @Override
    @Nullable
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    @Override
    public Map<String, Integer> getFlags() {
        return m_flags;
    }

    @Override
    public IObserverRegistry getObservers() {
        return m_observers;
    }

    @Override
    public EntityBridge getBridge() {
        return m_bridge;
    }

    @Override
    public IEntityTaskModel getTaskModel() {
        return new NullEntityTaskModel();
    }

    private class ContactObserver implements IPhysicsBodyContactObserver {
        @Override
        public void onBeginContact(IImmutablePhysicsBody other) {
            if(!other.isSensor() && other.getOwner() != m_ignore && other.isCollidable()) {
                m_world.removeEntity(LaserProjectile.this);
            }
        }

        @Override
        public void onEndContact(IImmutablePhysicsBody other) {

        }
    }

}
