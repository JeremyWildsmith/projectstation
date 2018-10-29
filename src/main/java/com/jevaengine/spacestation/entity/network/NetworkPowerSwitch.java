/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.entity.power.IPowerDevice;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public class NetworkPowerSwitch extends NetworkDevice implements IPowerDevice, INetworkDataCarrier {

	private final IAnimationSceneModel m_model;

	private boolean m_isOn;

	public NetworkPowerSwitch(String name, IAnimationSceneModel model, int ipAddress) {
		super(name, false, ipAddress);
		m_model = model;
	}
	
	private IPowerDevice getUpperDevice() {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);
			
			if(dir == Direction.YMinus)
				return d;
		}
		
		return null;
	}
	
	private IPowerDevice getLowerDevice() {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);
			
			if(dir == Direction.YPlus)
				return d;
		}
		
		return null;		
	}
	
	@Override
	protected void connectionChanged() { }

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		Direction dir = Direction.fromVector(delta);
		
		if(!(d instanceof IPowerDevice) || !(d instanceof WiredDevice))
			return false;
		
		if(dir == Direction.YMinus) {
			return getUpperDevice() == null;
		} else if(dir == Direction.YPlus)
			return getLowerDevice() == null;
		
		return false;
	}

	@Override
	protected void processPacket(NetworkPacket p) {
		BinarySignalProtocol.BinarySignal s = BinarySignalProtocol.decode(p);

		if(s != null)
			m_isOn = s.signal;

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
		m_model.getAnimation(m_isOn ? "on" : "off").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		if(!m_isOn)
			return 0;
		
		int drawn = getUpperDevice() == null ? 0 : getUpperDevice().drawEnergy(requested, joules);
		drawn += getLowerDevice() == null ? 0 : getLowerDevice().drawEnergy(requested, joules - drawn);
		
		return drawn;
	}
}
