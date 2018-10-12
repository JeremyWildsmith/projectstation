package com.jevaengine.spacestation.gas;

import com.jevaengine.spacestation.entity.GasVent;
import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.scene.model.DecoratedSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;
import org.apache.commons.lang.time.StopWatch;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class GasSimulationEntity implements IEntity, GasSimulation.WorldMapReader {
    public static String INSTANCE_NAME = "GAS_SIMULATION";

    private World m_world;

    private final String m_instanceName;

    private HashMap<String, Integer> m_flags = new HashMap<>();
    private HashMap<Vector2D, Boolean> isAirTightMap = new HashMap<>();
    private HashMap<Vector2D, Boolean> isBlockingMap = new HashMap<>();

    private WorldObserver m_worldObserver = new WorldObserver();

    private Map<GasType, GasSimulation> simulations = new HashMap<>();

    public boolean connectionMapDirty = true;

    public GasSimulationEntity() {
        this.m_instanceName = INSTANCE_NAME;
    }

    @Override
    public boolean isAirTight(Vector2D location) {
        if(isAirTightMap.containsKey(location))
            return isAirTightMap.get(location);

        Infrastructure[] entities = m_world.getEntities().search(Infrastructure.class, new RadialSearchFilter<Infrastructure>(new Vector2F(location), 0.4F));

        boolean airTight = false;
        for(Infrastructure e : entities) {
            if (e.isAirTight()) {
                airTight = true;
                break;
            }
        }

        isAirTightMap.put(location, airTight);

        return airTight;
    }

    @Override
    public boolean isBlocking(Vector2D location) {
        if(isBlockingMap.containsKey(location))
            return isBlockingMap.get(location);

        IEntity[] entities = m_world.getEntities().search(IEntity.class, new RadialSearchFilter<>(new Vector2F(location), 0.4F));

        boolean blocking = false;
        for(IEntity e : entities) {

            if(e instanceof Door) {
                blocking = e.getBody().isCollidable();
                break;
            } else if(e instanceof Infrastructure && e.getBody().isCollidable() && ((Infrastructure) e).isAirTight()) {
                blocking = true;
                break;
            }
        }

        isBlockingMap.put(location, blocking);

        return blocking;
    }

    public Map<GasType, Float> consume(Vector2D location, float volume) {
        if(volume < 0 || Float.isNaN(volume))
            throw new IllegalArgumentException();

        Map<GasType, Float> getRatio = new HashMap<>();
        float total = 0;

        Map<GasType, Float> quantity = getQuantity(location);
        for(Map.Entry<GasType, Float> e : quantity.entrySet()) {
            total += e.getValue();
        }

        if(total <= 0)
            return getRatio;

        for(GasType g : quantity.keySet()) {
            getRatio.put(g, simulations.get(g).consume(location, volume * quantity.get(g) / total));
        }

        return getRatio;
    }

    public void produce(Vector2D location, GasType type, float volume) {
        if(volume < 0 || Float.isNaN(volume))
            throw new IllegalArgumentException();

        if(!isAirTight(location))
            return;

        if(!simulations.containsKey(type))
            simulations.put(type, new GasSimulation(this, type.getFlowRatio()));

        simulations.get(type).produce(location, volume);
    }

    public void set(Vector2D location, GasType type, float volume) {
        if(volume < 0 || Float.isNaN(volume))
            throw new IllegalArgumentException();

        if(!isAirTight(location))
            return;

        if(!simulations.containsKey(type))
            simulations.put(type, new GasSimulation(this, type.getFlowRatio()));

        simulations.get(type).set(location, volume);

    }

    public Map<GasType, Float> getQuantity(Vector2D location) {
        Map<GasType, Float> quantity = new HashMap<>();

        for(Map.Entry<GasType,GasSimulation> e : simulations.entrySet()) {
            quantity.put(e.getKey(),e.getValue().getQuantity(location));
        }

        return quantity;
    }

    public float getTotalQuantity(Vector2D location) {
        float total = 0;
        for(Map.Entry<GasType,Float> e : getQuantity(location).entrySet()) {
            if(!e.getKey().isFakeGas())
                total += e.getValue();
        }

        return total;
    }


    public float getTotalAreaQuantity(Vector2D location) {
        float total = 0;

        for(GasSimulation s : simulations.values())
            total += s.getAreaQuantity(location);

        return total;
    }

    @Override
    public World getWorld() {
        return m_world;
    }

    @Override
    public void associate(World world) {
        for(IEntity e : world.getEntities().all()) {
            if (e instanceof GasSimulationEntity)
                throw new WorldAssociationException("Cannot have more than one gas simulation per world.");
        }

        m_world = world;
        m_world.getObservers().add(m_worldObserver);
    }

    @Override
    public void disassociate() {
        m_world.getObservers().remove(m_worldObserver);
        m_world = null;
    }

    @Override
    public String getInstanceName() {
        return m_instanceName;
    }

    @Override
    public Map<String, Integer> getFlags() {
        return m_flags;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return new NullSceneModel();
    }

    @Override
    public IPhysicsBody getBody() {
        return new NonparticipantPhysicsBody();
    }

    @Override
    public IEntityTaskModel getTaskModel() {
        return new NullEntityTaskModel();
    }

    @Override
    public IObserverRegistry getObservers() {
        return new Observers();
    }

    @Override
    public EntityBridge getBridge() {
        return new EntityBridge(this);
    }

    @Override
    public void update(int delta) {
        if(connectionMapDirty) {
            isAirTightMap.clear();
            isBlockingMap.clear();
        }

        for(GasSimulation g : simulations.values())
            g.update(delta);
    }

    @Override
    public void dispose() { }

    private class WorldObserver implements World.IWorldObserver {
        @Override
        public void addedEntity(IEntity e) {
            connectionMapDirty = true;
        }

        @Override
        public void removedEntity(Vector3F loc, IEntity e) {
            connectionMapDirty = true;
            for(GasSimulation g : simulations.values())
                g.removedEntity(loc);
        }
    }
}
