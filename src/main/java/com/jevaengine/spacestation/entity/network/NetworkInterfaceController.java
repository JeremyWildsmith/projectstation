/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkIoTerminal;
import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.power.Dcpu;
import com.jevaengine.spacestation.entity.power.IDcpuCompatibleDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.entity.power.IPowerDevice;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.IAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public class NetworkInterfaceController extends NetworkDevice implements IDcpuCompatibleDevice, IPowerDevice {
	private static final int POWER_USEAGE_WATTS = 50;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.
	
	private final IAnimationSceneModel m_model;
	private final NetworkIoTerminal m_networkTerminal = new NetworkIoTerminal();
	
	public NetworkInterfaceController(String name, IAnimationSceneModel model, int ipAddress) {
		super(name, false, ipAddress);
		m_model =  model;
	}
	
	private boolean drawEnergy(int timeDelta) {/*
		List<IPowerDevice> connections = getConnections(IPowerDevice.class);
		if(connections.isEmpty())
			return false;

		List<IDevice> requested = new ArrayList<>();
		requested.add(this);

		int requiredEnergy = (int)Math.ceil((((float)timeDelta) / 1000) * POWER_USEAGE_WATTS);
		
		return connections.get(0).drawEnergy(requested, requiredEnergy) >= requiredEnergy;*/
		return true;
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
		return m_model;
	}

	@Override
	public void update(int delta) { 
		m_model.update(delta);
		
		IAnimationSceneModelAnimation on = m_model.getAnimation("on");
		IAnimationSceneModelAnimation off = m_model.getAnimation("off");

		if(drawEnergy(delta)) {
			on.setState(AnimationSceneModelAnimationState.Play);

			NetworkPacket p;
			while((p = m_networkTerminal.pollTransmitQueue()) != null) {
				transmitMessage(p);
			}

		} else if (off.getState() != AnimationSceneModelAnimationState.Play)
			off.setState(AnimationSceneModelAnimationState.Play);
	}
	
	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof Dcpu) || (d instanceof NetworkWire);
	}

	@Override
	public IDcpuHardware[] getHardware() {
		return new IDcpuHardware[] {m_networkTerminal};
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
	}

	@Override
	public void processPacket(NetworkPacket packet) {
		m_networkTerminal.recievedMessage(packet);
	}
}
