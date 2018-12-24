/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public final class GasVent extends LiquidPipe {

    private static final int MAX_CONNECTIONS = 1;

    public GasVent(String name, IAnimationSceneModel model) {
        super(name, model, GasSimulationNetwork.Environment);
    }


    @Override
    protected void connectionChanged() {
        m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
    }

    @Override
    public float getVolume() {
        return GasSimulationNetwork.ENVIRONMENT_UNIT_VOLUME;
    }

    @Override
    public GasSimulationNetwork getNetwork() {
        return GasSimulationNetwork.Environment;
    }

    @Override
    public Map<Vector2D, GasSimulationNetwork> getLinks() {
        List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);

        if(connections.isEmpty())
            return new HashMap<>();

        Map<Vector2D, GasSimulationNetwork> links = new HashMap<>();

        links.put(this.getBody().getLocation().getXy().round(), connections.get(0).getNetwork());

        return links;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public boolean isFreeFlow() {
        return false;
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

}
