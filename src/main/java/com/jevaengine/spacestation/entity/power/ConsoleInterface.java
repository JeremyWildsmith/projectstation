/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.BasicDevice;
import com.jevaengine.spacestation.entity.WiredDevice;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultKeyboard;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultScreen;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.IAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class ConsoleInterface extends WiredDevice implements IDcpuCompatibleDevice, IPowerDevice {
	private static final int POWER_USEAGE_WATTS = 40;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.
	
	private final IAnimationSceneModel m_model;
	
	private final DefaultKeyboard m_keyboard = new DefaultKeyboard(false);
	private final DefaultScreen m_screen = new DefaultScreen(false, false);
	
	private boolean m_isOn = false;

	public ConsoleInterface(String name, IAnimationSceneModel model) {
		super(name, false);
		m_model =  model;
	}
	
		
	private IPowerDevice getAreaPowerSource() {
		List<IPowerDevice> controller = getConnections(IPowerDevice.class);
		
		return controller.isEmpty() ? null : controller.get(0);
	}
	
	private boolean drawEnergy(int timeDelta) {

		return true;/*
		IPowerDevice c = getAreaPowerSource();
		
		List<IDevice> requested = new ArrayList<>();
		requested.add(this);
		
		if(c == null)
			return false;
		
		int requiredEnergy = (int)Math.ceil((((float)timeDelta) / 1000) * POWER_USEAGE_WATTS);
		
		return c.drawEnergy(requested, requiredEnergy) >= requiredEnergy;*/
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
	
	public void simulateKeyTyped(int keyCode, char keyChar) {
		if(!m_isOn)
			return;
		
		m_keyboard.simulateKeyTyped(keyCode, keyChar);
	}
	
	public BufferedImage getScreen() {
		if(!m_screen.isActive())
			return null;
		
		return m_screen.getScreenImage();
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
		IAnimationSceneModelAnimation inactive = m_model.getAnimation("inactive");
		
		if(!drawEnergy(delta))
			turnOff();
		
		if(m_isOn) {
			if(m_screen.isActive()) {
				if(on.getState() != AnimationSceneModelAnimationState.Play)
					on.setState(AnimationSceneModelAnimationState.Play);
			} else {
				inactive.setState(AnimationSceneModelAnimationState.Play);
			}
		} else if (off.getState() != AnimationSceneModelAnimationState.Play)
			off.setState(AnimationSceneModelAnimationState.Play);
		
		
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return d instanceof Dcpu;
	}

	@Override
	public IDcpuHardware[] getHardware() {
		return new IDcpuHardware[] {m_keyboard, m_screen};
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
	}
}
