package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulation;
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
import java.util.Map;

public class LiquidPump extends WiredDevice implements ILiquidCarrier {

    private final int MAX_CONNECTIONS = 2;
    private final float TOLERANCE = 0.001F;
    private final IAnimationSceneModel model;
    private final float outputPressure;
    private final float volumePerSecond;

    private World world = null;
    private GasSimulationEntity gasSim = null;

    public LiquidPump(String name, IAnimationSceneModel model, float outputPressure, float volumePerSecond) {
        super(name, true);
        this.model = model;
        this.outputPressure = outputPressure;
        this.volumePerSecond = volumePerSecond;
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
    public float getVolume() {
        return LiquidPipe.PIPE_VOLUME;
    }

    @Override
    public boolean isFreeFlow() {
        return false;
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
        return model;
    }

    @Override
    public GasSimulationNetwork getNetwork() {
        return GasSimulationNetwork.PipeA;
    }

    @Override
    public Map<Vector2D, GasSimulationNetwork> getLinks() {
        return new HashMap<>();
    }

    @Override
    public void update(int delta) {

        if(world != getWorld()) {
            world = getWorld();
            gasSim = world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
        }

        if(gasSim == null)
            return;

        model.setDirection(this.getBody().getDirection());
        ILiquidCarrier dest = getDestination();
        ILiquidCarrier src = getSource();

        if(src == null || dest == null)
            return;

        Vector2D sourceLocation = src.getBody().getLocation().getXy().round();
        Vector2D destLocation = dest.getBody().getLocation().getXy().round();

        GasSimulationNetwork sourceNetwork = src.getNetwork();
        GasSimulationNetwork destNetwork = dest.getNetwork();

        GasSimulation.GasMetaData destData = gasSim.sample(destNetwork, destLocation);

        float destVolume = gasSim.getVolume(destNetwork, destLocation);

        float attemptConsumeLitres = volumePerSecond * delta / 1000.0f;

        if(outputPressure < 0 || destData.calculatePressure(destVolume) < outputPressure) {
            GasSimulation.GasMetaData sample = gasSim.consume(sourceNetwork, sourceLocation, attemptConsumeLitres);

            if(sample.getTotalMols() > 0)
                gasSim.produce(destNetwork, destLocation, sample);
        }
    }
}
