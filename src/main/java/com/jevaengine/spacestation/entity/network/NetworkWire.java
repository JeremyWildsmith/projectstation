/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.Wire;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class NetworkWire extends Wire implements INetworkDataCarrier {

	public NetworkWire(String name, IAnimationSceneModel model) {
		super(name, model);
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		return (d instanceof WiredDevice) && (d instanceof INetworkDataCarrier);
	}

	@Override
	public void carry(List<INetworkDataCarrier> carried, NetworkPacket packet) {
		if(carried.contains(this))
			return;
		
		carried.add(this);
		
		for(INetworkDataCarrier d : getConnections(INetworkDataCarrier.class)) {
			d.carry(carried, packet);
		}
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public void update(int delta) {
	}
}
