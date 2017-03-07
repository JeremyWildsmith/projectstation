/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class ApcJunction extends WiredDevice implements IPowerDevice {

	private final ISceneModel m_model;
	
	public ApcJunction(String name, ISceneModel model) {
		super(name, true);
		m_model = model;
	}

	private AreaPowerController getAreaPowerController() {
		List<AreaPowerController> controllers = getConnections(AreaPowerController.class);
		
		if(controllers.isEmpty())
			return null;
		
		return controllers.get(0);
	}
	
	private IPowerDevice getPowerDevice() {
		List<IPowerDevice> devices = getConnections(IPowerDevice.class);
		
		for(IPowerDevice d : devices) {
			if(d == getAreaPowerController())
				continue;
			
			return d;
		}
		
		return null;
	}
	
	@Override
	protected void connectionChanged() {
		IPowerDevice device = getPowerDevice();
		
		if(device == null)
			return;
		
		Vector2F delta = device.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		Direction dir = Direction.fromVector(delta);
		
		m_model.setDirection(dir);
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(d instanceof AreaPowerController)
			return getAreaPowerController() == null;
		
		if(!super.canConnectTo(d))
			return false;
		
		if(d instanceof WiredDevice && d instanceof IPowerDevice) {
			return getPowerDevice() == null;
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
	public void update(int delta) { }

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		int drawn = getAreaPowerController() == null ? 0 : getAreaPowerController().drawEnergy(requested, joules);
		drawn += getPowerDevice() == null ? 0 : getPowerDevice().drawEnergy(requested, joules - drawn);
		
		return drawn;
	}
	
}
