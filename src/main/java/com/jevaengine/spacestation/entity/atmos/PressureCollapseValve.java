package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.network.INetworkDataCarrier;
import com.jevaengine.spacestation.entity.network.NetworkDevice;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.List;

public class PressureCollapseValve extends WiredDevice implements ILiquidCarrier {

    private IAnimationSceneModel m_model;
    private boolean m_isOpen;
    private float m_collapsePressure;
    private World m_world;
    private GasSimulationEntity m_sim;

    public PressureCollapseValve(String name, IAnimationSceneModel model, float collapsePressure) {
        super(name, true);
        m_model = model;
        m_isOpen = false;
        m_collapsePressure = collapsePressure;
    }

    @Override
    public float getVolume() {
        return LiquidPipe.PIPE_VOLUME;
    }

    @Override
    protected boolean canConnectTo(IDevice d) {
        if(d instanceof INetworkDataCarrier) {
            return d.getBody().getLocation().getXy().round().difference(this.getBody().getLocation().getXy().round()).isZero() &&
                    this.getConnections(INetworkDataCarrier.class).size() <= 0;
        } else if (d instanceof ILiquidCarrier) {

            Direction thisDir = getBody().getDirection();

            if(thisDir.isDiagonal() || thisDir == Direction.Zero)
                return false;

            Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
            Direction dir = Direction.fromVector(delta.getXy());

            if(dir.isDiagonal() || Direction.fromVector(new Vector2F(thisDir.getDirectionVector().add(dir.getDirectionVector()))).isDiagonal())
                return false;

            return true;
        } else
            return true;
    }

    @Override
    public boolean isFreeFlow() {
        return m_isOpen;
    }


    @Override
    protected void connectionChanged() {

    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        m_model.setDirection(this.getBody().getDirection());
        return m_model;
    }

    private void open() {
        if(m_isOpen)
            return;

        m_isOpen = true;
        m_observers.raise(ILiquidCarrierObserver.class).freeFlowChanged();
    }

    private void close() {
        if(!m_isOpen)
            return;

        m_isOpen = false;
        m_observers.raise(ILiquidCarrierObserver.class).freeFlowChanged();
    }

    @Override
    public void update(int delta) {
        if(m_world != getWorld())
        {
            m_world = getWorld();
            m_sim = m_world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
        }

        List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);
        if(connections.size() == 2) {
            ILiquidCarrier a = connections.get(0);
            ILiquidCarrier b = connections.get(1);

            Vector2D locationA = a.getBody().getLocation().getXy().round();
            Vector2D locationB = b.getBody().getLocation().getXy().round();

            float aVol = m_sim.getVolume(GasSimulationNetwork.Pipe, locationA);
            float bVol = m_sim.getVolume(GasSimulationNetwork.Pipe, locationB);

            float pressureA = m_sim.sample(GasSimulationNetwork.Pipe, locationA).calculatePressure(aVol);
            float pressureB = m_sim.sample(GasSimulationNetwork.Pipe, locationB).calculatePressure(bVol);

            if(Math.abs(pressureA - pressureB) > m_collapsePressure)
                open();
            else
                close();
        }
    }
}
