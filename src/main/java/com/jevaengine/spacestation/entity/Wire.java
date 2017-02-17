/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class Wire extends BasicPowerDevice {

	private final IAnimationSceneModel m_model;
	
	private World m_world;
	
	public Wire(String name, IAnimationSceneModel model) {
		super(name);
		m_model = model;
	}
	
	@Override
	public int drawEnergy(List<IPowerDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		int drawn = 0;
		
		List<IPowerDevice> connections = getConnections();
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
	
	private void updateModel() {
		
		HashSet<Direction> directions = new HashSet<>();
		List<String> directionNames = new ArrayList<>();
		
		for(IPowerDevice w : getConnections()) {
			Vector3F delta = w.getBody().getLocation().difference(getBody().getLocation());
			Direction d = Direction.fromVector(delta.getXy());
			if(d != Direction.Zero && !d.isDiagonal())
				directions.add(d);
		}
		
		for(Direction d : directions)
			directionNames.add(d.toString());
		
		Collections.sort(directionNames);
		
		String animationName = String.join(",", directionNames);
		
		if(animationName.isEmpty())
			animationName = "idle";
		
		m_model.getAnimation(animationName).setState(AnimationSceneModelAnimationState.Play);
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
	
	}

	@Override
	protected void connectionChanged() {
		updateModel();
	}
}
