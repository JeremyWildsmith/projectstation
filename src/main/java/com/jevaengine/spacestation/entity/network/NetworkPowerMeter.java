/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.network.protocols.MeasurementProtocol;
import com.jevaengine.spacestation.entity.network.protocols.PingProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.entity.power.IPowerDevice;
import com.jevaengine.spacestation.entity.power.PowerWire;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public class NetworkPowerMeter extends NetworkDevice implements IPowerDevice, INetworkDataCarrier {

	private final IAnimationSceneModel m_model;

	private final int UPDATE_INTERVAL = 1000;

	private float oldAverage = 0;
	private int totalDrawn = 0;
	private int lastUpdate = 0;

	public NetworkPowerMeter(String name, IAnimationSceneModel model, int ipAddress) {
		super(name, false, ipAddress);
		m_model = model;
	}

	private IPowerDevice getOtherDevice(Direction not) {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);

			if(dir != not)
				return d;
		}

		return null;
	}

	private IPowerDevice getDevice(Direction thisDir) {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);

			if(dir == thisDir)
				return d;
		}

		return null;
	}

	@Override
	protected void connectionChanged() { }

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(d instanceof INetworkDataCarrier) {
			return super.canConnectTo(d) && getConnections(INetworkDataCarrier.class).size() == 0;
		}

		Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		Direction dir = Direction.fromVector(delta);

		if(!(d instanceof PowerWire))
			return false;

		if(this.getConnections(IPowerDevice.class).size() >= 2)
			return false;

		if(getDevice(dir) != null)
			return false;

		return true;
	}

	@Override
	protected void processPacket(NetworkPacket p) {
		if(PingProtocol.decode(p)) {
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

	@Override
	public void update(int delta) {
		lastUpdate += delta;

		if(lastUpdate >= UPDATE_INTERVAL) {
			float avgDraw = totalDrawn / (UPDATE_INTERVAL / 1000);
			oldAverage = avgDraw;
			totalDrawn = 0;
			lastUpdate = 0;
		}
	}

	private void transmitMeasurementSignal(int reciever) {
		int[] pwr = MeasurementProtocol.getParts(oldAverage / 1000.0f);

		NetworkPacket packet = MeasurementProtocol.encode(new MeasurementProtocol.MeasurementSignal(" kW", pwr[0], pwr[1]));
		packet.RecieverAddress = reciever;
		transmitMessage(packet);

		packet = MeasurementProtocol.encode(new MeasurementProtocol.MeasurementSignal("---", 0, 0));
		packet.RecieverAddress = reciever;
		transmitMessage(packet);
	}


	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);

		int drawn = 0;

		for(IPowerDevice d : getConnections(IPowerDevice.class))
			drawn += d.drawEnergy(requested, joules);

		totalDrawn += drawn;

		return drawn;
	}
}
