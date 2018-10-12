package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.liquid.ILiquid;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiquidPump extends WiredDevice implements ILiquidCarrier {

    private final int MAX_CONNECTIONS = 2;
    private final float TOLERANCE = 0.001F;
    private final IAnimationSceneModel m_model;

    public LiquidPump(String name, IAnimationSceneModel model) {
        super(name, true);
        m_model = model;
    }

    private ILiquidCarrier getSource() {
        for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            Vector2F delta = c.getBody().getLocation().difference(getBody().getLocation()).getXy();

            if(Direction.fromVector(delta) != getBody().getDirection())
                return c;
        }

        return null;
    }

    private ILiquidCarrier getDestination() {
        for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            Vector2F delta = c.getBody().getLocation().difference(getBody().getLocation()).getXy();

            if(Direction.fromVector(delta) == getBody().getDirection())
                return c;
        }

        return null;
    }

    @Override
    protected boolean canConnectTo(IDevice d) {
        Direction thisDir = getBody().getDirection();

        if(thisDir.isDiagonal() || thisDir == Direction.Zero)
            return false;

        if(getConnections().size() >= MAX_CONNECTIONS)
            return false;

        if(!(d instanceof ILiquidCarrier))
            return false;

        Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
        Direction dir = Direction.fromVector(delta.getXy());

        if(Direction.fromVector(new Vector2F(thisDir.getDirectionVector().add(dir.getDirectionVector()))).isDiagonal())
            return false;

        return true;
    }

    @Override
    protected void connectionChanged() {
    }

    @Override
    public float getLiquidVolume() {
        return 0;
    }

    @Override
    public float getSourcedLiquidVolume(List<ILiquidCarrier> requested, float sourceWeight) {
        return sourceWeight;
    }

    @Override
    public float getCapacityVolume() {
        return 0;
    }

    @Override
    public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
        return 0;
    }

    @Override
    public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity) {
        return new HashMap<>();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    private Map<ILiquid, Float> removeVolume(Map<ILiquid, Float> liquid, float volume) {
        Map<ILiquid, Float> net = new HashMap<>();

        float total = 0;
        for(Float f : liquid.values())
            total += f;

        if(total == 0)
            return new HashMap<>();

        for(Map.Entry<ILiquid, Float> e : liquid.entrySet()) {
            net.put(e.getKey(), e.getValue() - volume * e.getValue() / total);
        }

        return net;
    }

    private Map<ILiquid, Float> addVolume(Map<ILiquid, Float> liquidA, Map<ILiquid, Float> liquidB) {
        Map<ILiquid, Float> net = new HashMap<>();

        for(Map.Entry<ILiquid, Float> e : liquidA.entrySet()) {
            net.put(e.getKey(), e.getValue());
        }

        for(Map.Entry<ILiquid, Float> e : liquidB.entrySet()) {
            float current = net.containsKey(e.getKey()) ? net.get(e.getKey()) : 0;
            net.put(e.getKey(), current + e.getValue());
        }

        return net;
    }

    private Map<ILiquid, Float> suck(ILiquidCarrier src, float srcAmnt) {
        List<ILiquidCarrier> cause = new ArrayList<>();
        Map<ILiquid, Float> total = new HashMap<>();
        float calcRemoved = 0;
        boolean accepted = true;
        while (accepted) {
            cause.clear();
            cause.add(this);
            Map<ILiquid, Float> removed = src.remove(cause, srcAmnt - calcRemoved);

            float amntRemoved = 0;
            for(Float f : removed.values())
                amntRemoved += f;

            if(amntRemoved < 0.01F) {
                accepted = false;
            }
            calcRemoved += amntRemoved;

            total = addVolume(total, removed);
        }

        return total;
    }

    private Map<ILiquid, Float> push(ILiquidCarrier dest, Map<ILiquid, Float> srcAmnt) {
        List<ILiquidCarrier> cause = new ArrayList<>();
        float calcAdded = 0;
        boolean accepted = true;
        while (accepted) {
            cause.clear();
            cause.add(this);
            float added = dest.add(cause, srcAmnt);
            if(added < TOLERANCE) {
                accepted = false;
            }
            calcAdded += added;
            srcAmnt = removeVolume(srcAmnt, added);
        }

        return srcAmnt;
    }

    @Override
    public void update(int delta) {
        float attemptPushQuantity = 1000 * delta / 1000;

        m_model.setDirection(this.getBody().getDirection());
        ILiquidCarrier dest = getDestination();
        ILiquidCarrier src = getSource();

        Map<ILiquid, Float> srcAmnt = suck(src, attemptPushQuantity);
        srcAmnt = push(dest, srcAmnt);
        srcAmnt = push(src, srcAmnt);

        float total = 0;
        for(Float f : srcAmnt.values()) {
            total += f;
        }

        if(total > TOLERANCE)
            throw new RuntimeException("Leak detected in pump.");

        return;
    }
}
