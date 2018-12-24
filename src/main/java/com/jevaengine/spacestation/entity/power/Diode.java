/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.WiredDevice;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
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
	private static final int MAX_CONNECTIONS = 2;

	private final ISceneModel m_model;

	public Diode(String name, IAnimationSceneModel model) {
		super(name, true);
		
		m_model = model;
	}


	private IPowerDevice getSource() {
		for(IPowerDevice c : getConnections(IPowerDevice.class)) {
			Vector2F delta = c.getBody().getLocation().difference(getBody().getLocation()).getXy();

			if(Direction.fromVector(delta) != getBody().getDirection())
				return c;
		}

		return null;
	}

	private IPowerDevice getDestination() {
		for(IPowerDevice c : getConnections(IPowerDevice.class)) {
			Vector2F delta = c.getBody().getLocation().difference(getBody().getLocation()).getXy();

			if(Direction.fromVector(delta) == getBody().getDirection())
				return c;
		}

		return null;
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
		m_model.setDirection(this.getBody().getDirection());
		return m_model;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		if(!(d instanceof IPowerDevice))
			return false;
		Direction thisDir = getBody().getDirection();

		if(thisDir.isDiagonal() || thisDir == Direction.Zero)
			return false;

		if(getConnections().size() >= MAX_CONNECTIONS)
			return false;

		Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
		Direction dir = Direction.fromVector(delta.getXy());

		if(Direction.fromVector(new Vector2F(thisDir.getDirectionVector().add(dir.getDirectionVector()))).isDiagonal())
			return false;

		return true;
	}
	
	@Override
	public void update(int delta) { }

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		IPowerDevice source = getSource();
		IPowerDevice dest = getDestination();
		if(source == null || dest == null)
			return 0;
		
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		if(requested.contains(source))
			return 0;
		
		return source.drawEnergy(requested, joules);
	}
}
