/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class PowerWire extends Wire implements IPowerDevice {

	public PowerWire(String name, IAnimationSceneModel model) {
		super(name, model);
	}
	
	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		int drawn = 0;
		
		List<IPowerDevice> connections = getConnections(IPowerDevice.class);
		Collections.shuffle(connections);
		
		for(IPowerDevice w : connections) {
			if(!requested.contains(w))
			{
				drawn += w.drawEnergy(requested, joules);
				if(drawn >= joules)
					break;
			}
		}
		
		return drawn;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof WiredDevice) && (d instanceof IPowerDevice);
	}
}
