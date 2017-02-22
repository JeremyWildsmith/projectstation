/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import java.util.ArrayList;
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
	public List<AreaNetworkController> getAreaNetworkControllers(List<INetworkDataCarrier> requested) {
		if(requested.contains(this))
			return new ArrayList<>();
		
		requested.add(this);
		
		List<AreaNetworkController> controllers = new ArrayList<>();
		
		for(INetworkDataCarrier carrier : getConnections(INetworkDataCarrier.class)) {
			controllers.addAll(carrier.getAreaNetworkControllers(requested));
		}
		
		return controllers;
	}
}
