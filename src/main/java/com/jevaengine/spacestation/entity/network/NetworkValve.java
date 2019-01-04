package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.atmos.ILiquidCarrier;
import com.jevaengine.spacestation.entity.atmos.LiquidPipe;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkValve extends NetworkDevice implements ILiquidCarrier {

    private final GasSimulationNetwork simNetwork = GasSimulationNetwork.PipeA;

    private IAnimationSceneModel m_model;
    private boolean m_isOpen;

    public NetworkValve(String name, IAnimationSceneModel model, int ipAddress, boolean isOpen) {
        super(name, true, ipAddress);
        m_model = model;
        m_isOpen = false;
    }

    @Override
    public GasSimulationNetwork getNetwork() {
        return simNetwork;
    }

    @Override
    public Map<Vector2D, GasSimulationNetwork> getLinks() {
        List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);
        HashMap<Vector2D, GasSimulationNetwork> links = new HashMap<>();

        for(ILiquidCarrier c : connections) {
            if(c.getNetwork() != this.getNetwork() && c.isFreeFlow()) {
                links.put(this.getBody().getLocation().getXy().round(), c.getNetwork());
            }
        }

        return links;
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

            if(getConnections(ILiquidCarrier.class).size() >= 2)
                return false;

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
    protected void processPacket(NetworkPacket p) {
        BinarySignalProtocol.BinarySignal signal = BinarySignalProtocol.decode(p);

        if(signal != null) {
            m_isOpen = signal.signal;
            m_observers.raise(ILiquidCarrierObserver.class).freeFlowChanged();
            m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
        }
    }

    @Override
    protected void connectionChanged() {
        m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public IImmutableSceneModel getModel() {
        m_model.setDirection(this.getBody().getDirection());
        return m_model;
    }

    @Override
    public void update(int delta) { }
}
