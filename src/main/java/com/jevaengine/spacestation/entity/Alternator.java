package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.List;

public class Alternator extends WiredDevice implements IPowerDevice, IMechanicalDevice {

	private final IAnimationSceneModel m_model;
	private final int m_wattPerRpm;
	
	private int m_wattageStore = 0;
	
	public Alternator(String name, IAnimationSceneModel model, int wattPerRpm) {
		super(name, false);
		m_model = model;
		m_wattPerRpm = wattPerRpm;
	}
	
	private IMechanicalDevice getTurbine() {
		List<IMechanicalDevice> d = getConnections(IMechanicalDevice.class);
		
		if(d.isEmpty())
			return null;
		
		return d.get(0);
	}
	
	private IPowerDevice getPowerOut() {
		List<IPowerDevice> d = getConnections(IPowerDevice.class);
		
		if(d.isEmpty())
			return null;
		
		return d.get(0);
	}
	
	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		if(d instanceof IPowerDevice && getPowerOut() == null)
			return true;
		
		if(!(d instanceof IMechanicalDevice))
			return false;
		
		Vector2F delta = d.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
		Direction dir = Direction.fromVector(delta);
		
		if(dir == Direction.YMinus && getTurbine() == null)
			return true;
		
		return false;
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
		IMechanicalDevice turbine = getTurbine();
		
		if(turbine == null || turbine.getRpm() == 0) {
			m_model.getAnimation("off").setState(AnimationSceneModelAnimationState.Play);
			m_wattageStore = 0;
		} else {
			int wattagePerSecond = turbine.getRpm() * m_wattPerRpm;
			int generatedWattage = (int)Math.ceil((delta / 10000.0F)*turbine.getRpm()*m_wattPerRpm);
			
			m_model.getAnimation("on").setState(AnimationSceneModelAnimationState.Play);
			
			m_wattageStore = Math.min(wattagePerSecond, m_wattageStore  + generatedWattage);
		}
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		int drawn = Math.min(joules, m_wattageStore);
		m_wattageStore -= drawn;
		
		return drawn;
	}

	@Override
	public int getRpm() {
		return 0;
	}
	
}
