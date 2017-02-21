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
public abstract class Wire extends WiredDevice {

	private final IAnimationSceneModel m_model;
	
	private World m_world;
	
	public Wire(String name, IAnimationSceneModel model) {
		super(name, true);
		m_model = model;
	}
	
	private void updateModel() {
		
		HashSet<Direction> directions = new HashSet<>();
		List<String> directionNames = new ArrayList<>();
		
		for(IDevice w : getConnections()) {
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
