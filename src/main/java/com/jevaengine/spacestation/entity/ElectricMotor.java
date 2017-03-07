package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.List;

public final class ElectricMotor extends WiredDevice implements IMechanicalDevice, IPowerDevice {

	private final IAnimationSceneModel m_model;
	private final int m_rpm;
	private final int m_powerConsumptionWatts;
	
	private boolean m_isOn;
	
	public ElectricMotor(String instanceName, IAnimationSceneModel model, int rpm, int powerConsumptionWatts) {
		super(instanceName, false);
		
		m_model = model;
		m_rpm = rpm;
		m_powerConsumptionWatts = powerConsumptionWatts;
	}
	
	private IPowerDevice getPowerDevice() {
		List<IPowerDevice> powerDevices = getConnections(IPowerDevice.class);
		
		if(powerDevices.isEmpty())
			return null;
		
		return powerDevices.get(0);
	}
	
	private IMechanicalDevice getMechanicalDevice() {
		List<IMechanicalDevice> devices = getConnections(IMechanicalDevice.class);
		
		if(devices.isEmpty())
			return null;
		
		return devices.get(0);
	}
	
	@Override
	protected void connectionChanged() {
		IMechanicalDevice device = getMechanicalDevice();
		
		if(device == null)
			return;
		
		Vector2F delta = device.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		
		m_model.setDirection(Direction.fromVector(delta));
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		if(d instanceof WiredDevice && d instanceof IPowerDevice)
			return getPowerDevice() == null;
		else if(d instanceof IMechanicalDevice) {
			if(getConnections(IMechanicalDevice.class).size() > 0)
				return false;
			
			Vector2F delta = this.getBody().getLocation().difference(d.getBody().getLocation()).getXy();
			
			Direction dir = Direction.fromVector(delta);
			
			return dir == Direction.XMinus || dir == Direction.XPlus;
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
		IPowerDevice d = getPowerDevice();
		
		List<IDevice> requested = new ArrayList<>();
		requested.add(this);
		
		int powerRequirement = (int)Math.floor(delta / 1000.0F * m_powerConsumptionWatts);
		
		if(d != null && d.drawEnergy(requested, powerRequirement) - powerRequirement >= 0)
			m_isOn = true;
		else
			m_isOn = false;
		
		m_model.getAnimation(m_isOn ? "on" : "off").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
	}

	@Override
	public int getRpm() {
		return m_isOn ? m_rpm : 0;
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
	}
}
