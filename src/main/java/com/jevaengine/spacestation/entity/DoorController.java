/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
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
public class DoorController extends BasicDevice implements IPowerDevice, INetworkNode, IInteractableEntity {
	private static final int POWER_USEAGE_WATTS = 10;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.
	
	private final IAnimationSceneModel m_model;
	
	private boolean m_isOn = false;
	
	private final String m_nodeName;
	
	private final int m_listenPort;
	
	private final MessageListener m_messageListener = new MessageListener();
	
	private AreaNetworkController m_currentAreaNetworkController;
	
	public DoorController(String name, IAnimationSceneModel model, String nodeName, int listenPort) {
		super(name, false);
		m_model =  model;
		m_nodeName = nodeName;
		m_listenPort = listenPort;
	}
	
	private AreaNetworkController getAreaNetworkController() {
		List<AreaNetworkController> controller = getConnections(AreaNetworkController.class);
		
		return controller.isEmpty() ? null : controller.get(0);
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

	private void closeDoors() {
		for(NetworkDoor d : getConnections(NetworkDoor.class)) {
			d.close();
		}
	}
	
	private void openDoors() {
		for(NetworkDoor d : getConnections(NetworkDoor.class)) {
			d.open();
		}
	}
	
	public boolean isOn() {
		return m_isOn;
	}
	
	public void turnOn() {
		if (m_isOn || !drawEnergy(ON_POWER_USAGE_SECONDS)) {
			return;
		}

		m_isOn = true;
	}
	
	public void turnOff() {
		m_isOn = false;
	}
	
	@Override
	protected void connectionChanged() { 
		AreaNetworkController newNetworkController = getAreaNetworkController();
		
		if(m_currentAreaNetworkController != null)
			m_currentAreaNetworkController.getObservers().remove(m_messageListener);
		
		m_currentAreaNetworkController = newNetworkController;
		
		if(newNetworkController != null)
			newNetworkController.getObservers().add(m_messageListener);
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
			if(on.getState() != AnimationSceneModelAnimationState.Play)
				on.setState(AnimationSceneModelAnimationState.Play);
		} else if (off.getState() != AnimationSceneModelAnimationState.Play)
			off.setState(AnimationSceneModelAnimationState.Play);
		
		
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(d instanceof AreaNetworkController)
			return m_currentAreaNetworkController == null;
		
		return (d instanceof NetworkDoor) || (d instanceof IPowerDevice);
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
	
	private class MessageListener implements AreaNetworkController.IAreaNetworkControllerObserver {

		@Override
		public void recievedMessage(NetworkPacket packet) {
			if(packet.Port != m_listenPort || m_currentAreaNetworkController == null || !m_isOn)
				return;
			
			if(packet.data[0] == 0)
				closeDoors();
			else
				openDoors();
			
			NetworkPacket response = new NetworkPacket();
			
			response.RecieverAddress = packet.SenderAddress;
			response.Port = m_listenPort;
			response.data[0] = 1;
			
			m_currentAreaNetworkController.transmitMessage(new NetworkPacket());
		}

	}
}
