package com.jevaengine.spacestation.entity;

import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultClock;
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
public class ProgrammableIntervalTimer extends BasicDevice implements IDcpuCompatibleDevice, IPowerDevice, IInteractableEntity {

	private static final int POWER_USEAGE_WATTS = 10;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.

	private final IAnimationSceneModel m_model;

	private final DefaultClock m_clock = new DefaultClock();

	private boolean m_isOn = false;

	public ProgrammableIntervalTimer(String name, IAnimationSceneModel model) {
		super(name, false);
		m_model = model;
	}

	private IPowerDevice getAreaPowerSource() {
		List<IPowerDevice> controller = getConnections(IPowerDevice.class);

		return controller.isEmpty() ? null : controller.get(0);
	}

	private boolean drawEnergy(int timeDelta) {
		IPowerDevice c = getAreaPowerSource();

		List<IDevice> requested = new ArrayList<>();
		requested.add(this);

		if (c == null) {
			return false;
		}

		int requiredEnergy = (int) Math.ceil((((float) timeDelta) / 1000) * POWER_USEAGE_WATTS);

		return c.drawEnergy(requested, requiredEnergy) >= requiredEnergy;
	}

	public boolean isOn() {
		return m_isOn;
	}

	public void turnOn() {
		if (m_isOn || !drawEnergy(ON_POWER_USAGE_SECONDS)) {
			return;
		}

		m_isOn = true;
		m_clock.reset();
	}

	public void turnOff() {
		m_isOn = false;
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

		if (!drawEnergy(delta)) {
			turnOff();
		}

		if (m_isOn) {
			if (on.getState() != AnimationSceneModelAnimationState.Play) {
				on.setState(AnimationSceneModelAnimationState.Play);
			}
		} else if (off.getState() != AnimationSceneModelAnimationState.Play) {
			off.setState(AnimationSceneModelAnimationState.Play);
		}

		if (m_isOn) {
			m_clock.update(delta);
		}
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return true;
	}

	@Override
	public IDcpuHardware[] getHardware() {
		return new IDcpuHardware[]{m_clock};
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
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
}
