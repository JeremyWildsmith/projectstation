/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;

/**
 *
 * @author Jeremy
 */
public class NetworkDoorController extends NetworkDevice implements INetworkDataCarrier {

	private final IAnimationSceneModel m_model;

	public NetworkDoorController(String name, IAnimationSceneModel model, int ipAddress) {
		super(name, true, ipAddress);
		m_model = model;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;

		return this.getConnections(INetworkDataCarrier.class).size() == 0;
	}

	private Door getDoor() {
		World w = getWorld();
		if (w == null)
			return null;

		Door[] doors = w.getEntities().search(Door.class, new RadialSearchFilter<Door>(this.getBody().getLocation().getXy(), 0.5f));

		if(doors.length > 0)
			return doors[0];

		return null;
	}
	
	@Override
	protected void connectionChanged() { }

	@Override
	protected void processPacket(NetworkPacket p) {
		BinarySignalProtocol.BinarySignal s = BinarySignalProtocol.decode(p);

		if(s != null) {
			Door d = getDoor();

			if(d == null)
				return;

			if(s.signal) {
				d.unlock();
				d.open();
			} else {
				d.close();
				d.lock();
			}
		}
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	@Override
	public void update(int delta) {
	}
}
