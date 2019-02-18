package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.network.INetworkDataCarrier;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PressureCollapseValve extends WiredDevice implements ILiquidCarrier {

    private final int STABLE_DECISION_PERIOD = 2000;

    private IAnimationSceneModel m_model;
    private boolean m_isOpen;
    private float m_collapsePressure;
    private World m_world;
    private GasSimulationEntity m_sim;

    private final GasSimulationNetwork m_simNetwork = GasSimulationNetwork.PipeA;

    private int lastDecisionMade = 0;

    private boolean decision = false;

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
    public GasSimulationNetwork getNetwork() {
        return m_simNetwork;
    }

    @Override
    public Map<Vector2D, GasSimulationNetwork> getLinks() {
        HashMap<Vector2D, GasSimulationNetwork> links = new HashMap<>();

        for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            if(c.getNetwork() != m_simNetwork) {
                links.put(c.getBody().getLocation().getXy().round(), c.getNetwork());
            }
        }
        return links;
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
        m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
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
        lastDecisionMade += delta;

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

            GasSimulationNetwork networkA = a.getNetwork();
            GasSimulationNetwork networkB = b.getNetwork();

            float aVol = m_sim.getVolume(networkA, locationA);
            float bVol = m_sim.getVolume(networkB, locationB);
            float aTemp = m_sim.getTemperature(locationA);
            float bTemp = m_sim.getTemperature(locationB);

            float pressureA = m_sim.sample(networkA, locationA).calculatePressure(aVol, aTemp);
            float pressureB = m_sim.sample(networkB, locationB).calculatePressure(bVol, bTemp);

            boolean open = Math.abs(pressureA - pressureB) > m_collapsePressure;

            if(open != decision) {
                lastDecisionMade = 0;
                decision = open;
            }

            if(decision != m_isOpen && (decision == true || lastDecisionMade >= STABLE_DECISION_PERIOD)) {
                if (decision) {
                    open();
                } else {
                    close();
                }
            }
        } else
            lastDecisionMade = 0;
    }
}
