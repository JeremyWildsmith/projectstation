/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;

/**
 *
 * @author Jeremy
 */
public class NetworkDoor extends Door implements INetworkNode {
	private final String m_nodeName;
	private DoorController m_controller;
	
	public NetworkDoor(IAnimationSceneModel model, String name, boolean isOpen, String nodeName) {
		super(model, name, isOpen);
		m_nodeName = nodeName;
	}

	@Override
	public String getNodeName() {
		return m_nodeName;
	}

	@Override
	public boolean addConnection(IDevice device) {
		if(m_controller != null)
			return false;
		
		if(!(device instanceof DoorController))
			return false;
			
		m_controller = (DoorController)device;
		
		return true;
	}

	@Override
	public void removeConnection(IDevice device) {
		if(device == m_controller) {
			m_controller = null;
		}
	}

	@Override
	public void clearConnections() {
		removeConnection(m_controller);
	}
}
