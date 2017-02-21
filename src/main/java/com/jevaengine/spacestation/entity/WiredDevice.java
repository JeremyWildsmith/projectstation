/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.World;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.search.ISearchFilter;
import io.github.jevaengine.world.search.RadialSearchFilter;

/**
 *
 * @author Jeremy
 */
public abstract class WiredDevice extends BasicDevice {
	public WiredDevice(String name, boolean isTraversable) {
		super(name, isTraversable);
		getObservers().add(new MovementObserver());
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
