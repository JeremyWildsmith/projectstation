/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.dcpu.devices.NetworkIoTerminal;
import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.AreaNetworkController.IAreaNetworkControllerObserver;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.IAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class NetworkInterfaceController extends BasicDevice implements IDcpuCompatibleDevice, IPowerDevice, INetworkNode, IInteractableEntity {
	private static final int POWER_USEAGE_WATTS = 50;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.
	
	private final IAnimationSceneModel m_model;
	private final NetworkIoTerminal m_networkTerminal = new NetworkIoTerminal();
	private AreaNetworkController m_lastAreaNetworkController;
	private final MessageObserver m_messageObserver = new MessageObserver();
	
	private boolean m_isOn = false;
	
	public String m_nodeName;
	
	public NetworkInterfaceController(String name, IAnimationSceneModel model, String nodeName) {
		super(name, false);
		m_model =  model;
		m_nodeName = nodeName;
	}
		
	private AreaPowerController getAreaPowerController() {
		List<AreaPowerController> controller = getConnections(AreaPowerController.class);
		
		return controller.isEmpty() ? null : controller.get(0);
	}
	
	private boolean drawEnergy(int timeDelta) {
		AreaPowerController c = getAreaPowerController();
		
		List<IDevice> requested = new ArrayList<>();
		requested.add(this);
		
		if(c == null)
			return false;
		
		int requiredEnergy = (int)Math.ceil((((float)timeDelta) / 1000) * POWER_USEAGE_WATTS);
		
		return c.drawEnergy(requested, requiredEnergy) >= requiredEnergy;
	}

	public boolean isOn() {
		return m_isOn;
	}
	
	public void turnOn() {
		if (m_isOn || !drawEnergy(ON_POWER_USAGE_SECONDS)) {
			return;
		}

		m_networkTerminal.reset();
		m_isOn = true;
	}
	
	public void turnOff() {
		m_isOn = false;
		m_networkTerminal.reset();
	}
	
	@Override
	protected void connectionChanged() {
		if(m_lastAreaNetworkController != null) {
			m_lastAreaNetworkController.getObservers().remove(m_messageObserver);
		}
		
		m_lastAreaNetworkController = null;
		
		List<AreaNetworkController> ancs = getConnections(AreaNetworkController.class);
		
		if(!ancs.isEmpty())
			m_lastAreaNetworkController = ancs.get(0);
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
		
		if(!drawEnergy(delta))
			turnOff();
		
		if(m_isOn) {
			on.setState(AnimationSceneModelAnimationState.Play);
		} else if (off.getState() != AnimationSceneModelAnimationState.Play)
			off.setState(AnimationSceneModelAnimationState.Play);
		
		if(m_isOn && m_lastAreaNetworkController != null) {
			NetworkPacket p;
			while((p = m_networkTerminal.pollTransmitQueue()) != null) {
				m_lastAreaNetworkController.transmitMessage(p);
			}
		}
	}
	
	@Override
	protected boolean canConnectTo(IDevice d) {
		return true;
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
	public String getNodeName() {
		return m_nodeName;
	}

	@Override
	public void interactedWith(IRpgCharacter subject) {
		if(m_isOn)
			turnOff();
		else
			turnOn();
	}

	@Override
	public void interactWith(IRpgCharacter subject, String interaction) {
		interactedWith(subject);
	}

	@Override
	public String[] getInteractions() { 
		return new String[0];
	}
	
	private class MessageObserver implements IAreaNetworkControllerObserver {
		@Override
		public void recievedMessage(NetworkPacket packet) {
			if(m_isOn)
				m_networkTerminal.recievedMessage(packet);
		}
	}
}
