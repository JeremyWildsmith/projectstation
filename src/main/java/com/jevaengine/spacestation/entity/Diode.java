/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class Diode extends WiredDevice implements IPowerDevice {

	private final ISceneModel m_model;

	//Is top to down OR left to right. If false, it is opposite.
	private final boolean m_isForward;
		
	public Diode(String name, IAnimationSceneModel model, boolean isForward) {
		super(name, true);
		
		m_model = model;
		m_isForward = isForward;
	}

	private IPowerDevice getDevice(boolean getFrom) {
		List<IPowerDevice> devices = getConnections(IPowerDevice.class);
		
		for(IPowerDevice d : devices) {
			Vector2F delta = d.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
			Direction dir = Direction.fromVector(delta);
			
			boolean findForward = m_isForward ? getFrom : !getFrom;
			
			if(findForward) {
				if(dir == Direction.XMinus || dir == Direction.YMinus)
					return d;
			} else {
				if(dir == Direction.XPlus || dir == Direction.YPlus)
					return d;
			}
		}
		
		return null;
	}
	
	@Override
	protected void connectionChanged() { 
		IPowerDevice d = getDevice(true) == null ? getDevice(false) : getDevice(true);
		
		if(d == null)
			return;
		
		Vector2F delta = d.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
		Direction dir = Direction.fromVector(delta);
		
		if(dir == Direction.YMinus || dir == Direction.YPlus) {
			m_model.setDirection(m_isForward ? Direction.YPlus : Direction.YMinus);
		} else if(dir == Direction.XMinus || dir == Direction.XPlus) {
			m_model.setDirection(m_isForward ? Direction.XPlus : Direction.XMinus);
		}
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
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		if(!(d instanceof IPowerDevice))
			return false;
		
		Vector2F delta = d.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
		Direction connecteeDir = Direction.fromVector(delta);
		
		if(getDevice(true) == null) {
			IPowerDevice to = getDevice(false);
			
			if(to == null)
				return true;
			
			Vector2F toDelta = to.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
			toDelta = toDelta.negative();
			
			return Direction.fromVector(toDelta) == connecteeDir;
		} else if(getDevice(false) == null) {
			IPowerDevice from = getDevice(true);
			
			if(from == null)
				return true;
			
			Vector2F fromDelta = from.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
			fromDelta = fromDelta.negative();
			
			return Direction.fromVector(fromDelta) == connecteeDir;
		}
		
		return false;
	}
	
	@Override
	public void update(int delta) { }

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(getDevice(true) == null || getDevice(false) == null)
			return 0;
		
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		if(requested.contains(getDevice(true)))
			return 0;
		
		return getDevice(true).drawEnergy(requested, joules);
	}
}
