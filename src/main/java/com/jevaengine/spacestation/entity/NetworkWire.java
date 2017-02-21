/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class NetworkWire extends Wire implements INetworkDevice {

	public NetworkWire(String name, IAnimationSceneModel model) {
		super(name, model);
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof WiredDevice) && (d instanceof INetworkDevice);
	}

	@Override
	public <T extends INetworkDevice> List<T> getConnected(List<INetworkDevice> requested, Class<T> device) {
		ArrayList<T> devices = new ArrayList<>();
		
		if (requested.contains(this))
			return devices;

		requested.add(this);

		List<INetworkDevice> connections = getConnections(INetworkDevice.class);
		Collections.shuffle(connections);

		for (INetworkDevice w : connections) {
			if(device.isAssignableFrom(w.getClass()))
				devices.add((T)w);
			
			devices.addAll(w.getConnected(requested, device));
		}

		return devices;
	}

	@Override
	public boolean isConnected(List<INetworkDevice> requested, INetworkDevice device) {
		if (requested.contains(this)) {
			return false;
		}

		requested.add(this);

		if (this == device) {
			return true;
		}

		List<INetworkDevice> connections = getConnections(INetworkDevice.class);
		Collections.shuffle(connections);

		for (INetworkDevice w : connections) {
			if (!requested.contains(w)) {
				if (w.isConnected(requested, device)) {
					return true;
				}
			}
		}

		return false;
	}
}
