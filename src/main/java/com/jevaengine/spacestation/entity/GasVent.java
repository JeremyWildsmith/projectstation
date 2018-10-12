/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.gas.GasSimulation;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasType;
import com.jevaengine.spacestation.liquid.GasLiquid;
import com.jevaengine.spacestation.liquid.ILiquid;
import com.sun.xml.internal.bind.v2.model.core.ID;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.*;

/**
 *
 * @author Jeremy
 */
public final class GasVent extends WiredDevice implements ILiquidCarrier {

    private static final int MAX_CONNECTIONS = 1;

    private final IAnimationSceneModel m_model;

    private GasSimulationEntity sim = null;
    private World m_world = null;

    public GasVent(String name, IAnimationSceneModel model) {
        super(name, true);
        m_model = model;
    }


    @Override
    protected void connectionChanged() {
    }

    @Override
    protected boolean canConnectTo(IDevice d) {
        if(getConnections().size() >= MAX_CONNECTIONS)
            return false;

        if(!(d instanceof LiquidPipe))
            return false;

        Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
        Direction dir = Direction.fromVector(delta.getXy());

        if(dir != Direction.Zero)
            return false;

        return true;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    private ILiquidCarrier getConnection() {
        List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);

        if(connections.isEmpty())
            return null;

        return connections.get(0);
    }

    private Map<ILiquid, Float> sampleContents() {

        Vector2D location = this.getBody().getLocation().getXy().round();

        Map<GasType, Float> sample = sim.getQuantity(location);

        Map<ILiquid, Float> contentsSampled = new HashMap<>();

        for(Map.Entry<GasType, Float> e : sample.entrySet()) {
            contentsSampled.put(new GasLiquid(e.getKey()), e.getValue());
        }

        return contentsSampled;
    }

    @Override
    public void update(int delta) {
        if(m_world != getWorld()) {
            m_world = getWorld();
            sim = m_world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
        }

        ILiquidCarrier connection = getConnection();

        if(connection == null)
            return;

        float toSpare = this.getLiquidVolume() - connection.getLiquidVolume();
        if(toSpare < 0)
            return;

        float room = Math.min(toSpare, connection.getCapacityVolume() - connection.getLiquidVolume());
        room = Math.min(room, getLiquidVolume() / 2);

        if(room <= 0)
            return;

        float amountFlow = room;// * (delta / 1000.0F);

        Vector2D location = this.getBody().getLocation().getXy().round();

        float consumed  = 0;

        Map<GasType, Float> consumedGasses = sim.consume(location, amountFlow);
        Map<ILiquid, Float> liquidsToPush = new HashMap<>();
        for(Map.Entry<GasType, Float> e : consumedGasses.entrySet())
            liquidsToPush.put(new GasLiquid(e.getKey()), e.getValue());

        List<ILiquidCarrier> cause = new ArrayList<>();
        cause.add(this);
        float added = connection.add(cause, liquidsToPush);
        float reabsorb = amountFlow - added;
        if(reabsorb > 0) {
            for(Map.Entry<ILiquid, Float> e : liquidsToPush.entrySet()) {
                sim.produce(location, ((GasLiquid)e.getKey()).getGasType(), e.getValue() * reabsorb / amountFlow);
            }
        }
    }

    @Override
    public float getLiquidVolume() {
        Vector2D location = this.getBody().getLocation().getXy().round();
        return sim == null ? 0 : sim.getTotalQuantity(location);
    }

    @Override
    public float getSourcedLiquidVolume(List<ILiquidCarrier> requested, float sourceWeight) {
        return getLiquidVolume();
    }

    @Override
    public float getCapacityVolume() {
        return Float.MAX_VALUE / 2;
    }


    private Map<ILiquid, Float> getSample(Map<ILiquid, Float> pool, float quantity) {
        float poolVolume = 0;

        for(Map.Entry<ILiquid, Float> e : pool.entrySet())
            poolVolume += e.getValue();

        float sampleSize = Math.min(quantity, poolVolume);
        float totalLiquid = poolVolume;

        if(totalLiquid == 0)
            return new HashMap<>();

        Map<ILiquid, Float> sample = new HashMap<>();

        for (Map.Entry<ILiquid, Float> liquid : pool.entrySet()) {
            sample.put(liquid.getKey(), (liquid.getValue() / totalLiquid) * sampleSize);
        }

        return sample;
    }

    @Override
    public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
        if(cause.contains(this) || sim == null)
            return 0;

        Vector2D location = this.getBody().getLocation().getXy().round();

        cause.add(this);

        float totalInput = 0;
        Map<ILiquid, Float> input = getSample(liquid, getCapacityVolume());

        for(Map.Entry<ILiquid, Float> e : input.entrySet()) {
            if(e.getKey() instanceof GasLiquid) {
                GasLiquid gasLiquid = (GasLiquid)e.getKey();
                sim.produce(location, gasLiquid.getGasType(), e.getValue());
                totalInput += e.getValue();
            }
        }

        return totalInput;
    }

    @Override
    public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity) {
        if(sim == null)
            return new HashMap<>();
        Vector2D location = this.getBody().getLocation().getXy().round();

        Map<GasType, Float> removed = sim.consume(location, quantity);

        Map<ILiquid, Float> contentsRemoved = new HashMap<>();

        for(Map.Entry<GasType, Float> e : removed.entrySet()) {
            contentsRemoved.put(new GasLiquid(e.getKey()), e.getValue());
        }

        return contentsRemoved;
    }
}
