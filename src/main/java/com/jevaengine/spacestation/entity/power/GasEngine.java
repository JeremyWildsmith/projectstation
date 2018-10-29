/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.atmos.ILiquidCarrier;
import com.jevaengine.spacestation.entity.WiredDevice;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public final class GasEngine extends WiredDevice implements IMechanicalDevice {

	private final IAnimationSceneModel m_model;
	private boolean m_isOn;

	private final int m_startupTime;

	private int m_timeSpentStarting;

	private final int m_starterRpmRequirement;
	private final float m_gasConsumptionPerSecond;
	private final int m_outputRpm;

	public GasEngine(String name, IAnimationSceneModel model, int startupTime, int starterRpm, float gasConsumptionPerSecond, int outputRpm) {
		super(name, false);
		m_model = model;
		
		m_startupTime = starterRpm;
		m_starterRpmRequirement = starterRpm;
		m_gasConsumptionPerSecond = gasConsumptionPerSecond;
		m_outputRpm = outputRpm;
	}
	
	private boolean isFueled(int time) {
		float required = m_gasConsumptionPerSecond * (time / 1000.0F);
		
		FuelChamber chamber = getFuelChamber();
		
		if(chamber == null)
			return false;
		
		List<ILiquidCarrier> cause = new ArrayList<>();
		//Map<ILiquid, Float> retrieved = chamber.remove(cause, required);

		//Float amount = retrieved.get(new GasolineLiquid());
		//amount = amount == null ? 0 : amount;
		//return amount - required > -0.0000001F;

		return true;
	}
	
	private boolean isStarting() {
		IMechanicalDevice starter = getStarter();
		
		if(starter == null)
			return false;
		
		return starter.getRpm() - m_starterRpmRequirement > -0.000001F;
	}
	
	private IMechanicalDevice getStarter() {
		List<IMechanicalDevice> mechanicals = getConnections(IMechanicalDevice.class);

		if (mechanicals.isEmpty()) {
			return null;
		}

		for (IMechanicalDevice d : mechanicals) {
			Vector2F delta = d.getBody().getLocation().difference(getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);

			if (dir == Direction.XMinus || dir == Direction.XPlus) {
				return d;
			}
		}

		return null;
	}

	private IMechanicalDevice getOutput() {
		List<IMechanicalDevice> mechanicals = getConnections(IMechanicalDevice.class);

		if (mechanicals.isEmpty()) {
			return null;
		}

		for (IMechanicalDevice d : mechanicals) {
			Vector2F delta = d.getBody().getLocation().difference(d.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);

			if (dir == Direction.YPlus) {
				return d;
			}
		}

		return null;
	}

	private FuelChamber getFuelChamber() {
		List<FuelChamber> chambers = getConnections(FuelChamber.class);

		if (chambers.isEmpty()) {
			return null;
		}

		return chambers.get(0);
	}

	@Override
	protected void connectionChanged() {
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		Direction dir = Direction.fromVector(delta);
			
		if (d instanceof IMechanicalDevice) {

			if (dir == Direction.XMinus || dir == Direction.XPlus) {
				if (getStarter() == null)
					return true;
			} else if (dir == Direction.YPlus) {
				if (getOutput() == null)
					return true;
			}
			
			return false;
		} else if (d instanceof FuelChamber) {
			if(dir != Direction.YMinus)
				return false;
			
			if(getFuelChamber() == null)
				return true;
		}
		
		return false;
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
		if(m_isOn) {
			m_model.getAnimation("on").setState(AnimationSceneModelAnimationState.Play);
			if(!isFueled(delta)) {
				m_isOn = false;
				m_timeSpentStarting = 0;
			}
		} else if (isStarting()) {
			m_model.getAnimation("starting").setState(AnimationSceneModelAnimationState.Play);
			if(!isFueled(delta)) {
				m_timeSpentStarting = 0;
			} else {
				m_timeSpentStarting += delta;
				
				if(m_timeSpentStarting >= m_startupTime)
					m_isOn = true;
			}
		} else {
			m_timeSpentStarting = 0;
			m_model.getAnimation("off").setState(AnimationSceneModelAnimationState.Play);
		}
	}

	@Override
	public int getRpm() {
		return m_isOn ? m_outputRpm : 0;
	}
}
