/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.search.ISearchFilter;
import io.github.jevaengine.world.search.RadialSearchFilter;

/**
 *
 * @author Jeremy
 */
public abstract class WiredDevice extends BasicDevice {
	private final float CONNECTION_DISTANCE = 1.1F;
	
	public WiredDevice(String name, boolean isTraversable) {
		super(name, isTraversable);
		getObservers().add(new MovementObserver());
	}
	
	@Override
	protected boolean canConnectTo(IDevice d) {
		Vector2F delta = d.getBody().getLocation().getXy().difference(getBody().getLocation().getXy());
		
		return delta.getLength() <= CONNECTION_DISTANCE;
	}
	
	private void updateConnections() {
		clearConnections();
		
		ISearchFilter<IDevice> searchFilter = new RadialSearchFilter<>(getBody().getLocation().getXy(), 1.1F);
		IDevice wires[] = getWorld().getEntities().search(IDevice.class, searchFilter);
	
		for(IDevice w : wires) {
			if(w != this)
				addConnection(w);
		}
	}

	private class MovementObserver implements IPhysicsBodyOrientationObserver, IEntityBodyObserver {
		@Override
		public void locationSet() {
			updateConnections();
		}

		@Override
		public void directionSet() { }

		@Override
		public void bodyChanged(IPhysicsBody oldBody, IPhysicsBody newBody) {
			newBody.getObservers().add(this);
		}
	}
}
