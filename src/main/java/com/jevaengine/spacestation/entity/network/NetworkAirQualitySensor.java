package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.network.protocols.MeasurementProtocol;
import com.jevaengine.spacestation.entity.network.protocols.PingProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.*;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

public class NetworkAirQualitySensor extends NetworkDevice {

    private static final int BROADCAST_INTERVAL = 2000;

    private final IAnimationSceneModel m_model;

    private World m_world;
    private GasSimulationEntity m_simulation;

    private final float IDEAL_PRESSURE = 102000;
    private final float IDEAL_PERCENT_OXYGEN = 0.2F;
    private final float IDEAL_PERCENT_OXYGEN_TOLERANCE = 0.05f;
    private final float IDEAL_PRESSURE_TOLERANCE = 20000;

    private boolean m_invertBinarySignal;

    public NetworkAirQualitySensor(String name, IAnimationSceneModel model, int ipAddress, boolean invertBinarySignal) {
        super(name, true, ipAddress);
        m_model = model;
        m_invertBinarySignal = invertBinarySignal;
    }

    @Override
    protected void connectionChanged() { }

    @Override
    protected boolean canConnectTo(IDevice d) {
        if(!super.canConnectTo(d))
            return false;

        if(!(d instanceof INetworkDataCarrier))
            return false;

        if(getConnections(INetworkDataCarrier.class).size() == 0)
            return true;

        return false;
    }

    @Override
    protected void processPacket(NetworkPacket p) {
        if(PingProtocol.decode(p)) {
            transmitQualitySignal(p.SenderAddress);
            transmitMeasurementSignal(p.SenderAddress);
        }
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    private Vector2D getTestLocation() {
        Vector2D location = this.getBody().getLocation().getXy().round();
        location = location.add(this.getBody().getDirection().getDirectionVector());

        return location;
    }

    private boolean isIdeal() {
        Vector2D location = getTestLocation();
        GasMetaData sample = m_simulation.sample(GasSimulationNetwork.Environment, location);
        float sampleVolume = m_simulation.getVolume(GasSimulationNetwork.Environment, location);

        if(Math.abs(IDEAL_PRESSURE - sample.calculatePressure(sampleVolume)) > IDEAL_PRESSURE_TOLERANCE)
            return false;

        if(Math.abs(IDEAL_PERCENT_OXYGEN - sample.getPercentContent(GasType.Oxygen)) > IDEAL_PERCENT_OXYGEN_TOLERANCE)
            return false;

        return true;
    }

    private void transmitQualitySignal(int reciever) {
        boolean transmitSignal = !isIdeal();

        if(m_invertBinarySignal)
            transmitSignal = !transmitSignal;

        NetworkPacket packet = BinarySignalProtocol.encode(new BinarySignalProtocol.BinarySignal(transmitSignal));
        packet.RecieverAddress = reciever;

        transmitMessage(packet);
    }

    private void transmitMeasurementSignal(int reciever) {
        if(m_simulation == null)
            return;

        Vector2D testLocation = getTestLocation();
        float sampleVolume = m_simulation.getVolume(GasSimulationNetwork.Environment, testLocation);
        GasMetaData sample = m_simulation.sample(GasSimulationNetwork.Environment, testLocation);

        int[] percentOxygen = MeasurementProtocol.getParts(sample.getPercentContent(GasType.Oxygen) * 100);
        int[] pressure = MeasurementProtocol.getParts(sample.calculatePressure(sampleVolume) / 1000.0f);

        NetworkPacket packet = MeasurementProtocol.encode(new MeasurementProtocol.MeasurementSignal("kPa", pressure[0], pressure[1]));
        packet.RecieverAddress = reciever;
        transmitMessage(packet);


        packet = MeasurementProtocol.encode(new MeasurementProtocol.MeasurementSignal("%O2", percentOxygen[0], percentOxygen[1]));
        packet.RecieverAddress = reciever;
        transmitMessage(packet);
    }

    @Override
    public void update(int delta) {
        if(m_world != getWorld())
        {
            m_world = getWorld();
            m_simulation = m_world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
        }

        String animation = isIdeal() ? "good" : "poor";

        m_model.getAnimation(animation).setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);

        m_model.setDirection(this.getBody().getDirection());
    }
}
