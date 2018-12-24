/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.Wire;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class NetworkBinarySignalInverter extends WiredDevice implements INetworkDataCarrier {

	private final ISceneModel m_model;

	public NetworkBinarySignalInverter(String name, IAnimationSceneModel model) {
		super(name, true);
		m_model = model;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;

		return (d instanceof WiredDevice) && (d instanceof INetworkDataCarrier);
	}

	@Override
	protected void connectionChanged() { }

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	@Override
	public void carry(List<INetworkDataCarrier> carried, NetworkPacket packet) {
		if(carried.contains(this))
			return;
		
		carried.add(this);
		
		for(INetworkDataCarrier d : getConnections(INetworkDataCarrier.class)) {
			BinarySignalProtocol.BinarySignal sig = BinarySignalProtocol.decode(packet);
			if(sig != null) {
				NetworkPacket p = BinarySignalProtocol.encode(new BinarySignalProtocol.BinarySignal(!sig.signal));
				p.Port = packet.Port;
				p.SenderAddress = packet.SenderAddress;
				p.RecieverAddress = packet.RecieverAddress;
				d.carry(carried, p);
			} else
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
