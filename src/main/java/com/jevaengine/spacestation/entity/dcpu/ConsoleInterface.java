/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.dcpu;

import com.jevaengine.spacestation.entity.*;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultKeyboard;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultScreen;
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
public class ConsoleInterface extends BasicDevice implements IDcpuCompatibleDevice {
	private final IAnimationSceneModel m_model;
	
	private final DefaultKeyboard m_keyboard = new DefaultKeyboard(false);
	private final DefaultScreen m_screen = new DefaultScreen(false, false);
	
	public ConsoleInterface(String name, IAnimationSceneModel model) {
		super(name, false);
		m_model =  model;
	}

	public DefaultKeyboard getKeyboard() {
		return m_keyboard;
	}
	
	public DefaultScreen getScreen() {
		return m_screen;
	}
	
	@Override
	protected void connectionChanged() { }
	
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
		
		if(m_screen.isActive()) {
			if(on.getState() != AnimationSceneModelAnimationState.Play)
				m_model.getAnimation("on").setState(AnimationSceneModelAnimationState.Play);
		} else if (off.getState() != AnimationSceneModelAnimationState.Play)
			m_model.getAnimation("off").setState(AnimationSceneModelAnimationState.Play);
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return true;
	}

	@Override
	public <T extends INetworkDevice> List<T> getConnected(List<INetworkDevice> requested, Class<T> device) {
		return new ArrayList<>();
	}

	@Override
	public boolean isConnected(List<INetworkDevice> requested, INetworkDevice device) {
		return false;
	}

	@Override
	public IDcpuHardware[] getHardware() {
		return new IDcpuHardware[] {m_keyboard, m_screen};
	}
}
