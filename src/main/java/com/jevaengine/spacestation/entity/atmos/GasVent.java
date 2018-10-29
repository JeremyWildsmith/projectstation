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

/**
 *
 * @author Jeremy
 */
public final class GasVent extends LiquidPipe {

    private static final int MAX_CONNECTIONS = 1;

    private GasSimulationEntity sim = null;
    private World m_world = null;

    public GasVent(String name, IAnimationSceneModel model) {
        super(name, model);
    }


    @Override
    protected void connectionChanged() {
    }

    @Override
    public float getVolume() {
        return GasSimulationNetwork.ENVIRONMENT_UNIT_VOLUME;
    }

    @Override
    public boolean isStatic() {
        return false;
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


    @Override
    public void update(int delta) {
        if (m_world != getWorld()) {
            m_world = getWorld();
            sim = m_world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);

            Vector2D location = this.getBody().getLocation().getXy().round();

            sim.addLink(location, GasSimulationNetwork.Environment, GasSimulationNetwork.Pipe);

        }

        super.update(delta);
    }

}
