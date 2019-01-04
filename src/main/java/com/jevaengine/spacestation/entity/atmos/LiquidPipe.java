/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.*;

/**
 *
 * @author Jeremy
 */
public class LiquidPipe extends WiredDevice implements ILiquidCarrier {

	public static final float PIPE_VOLUME = 0.1f;

	private final int MAX_CONNECTIONS = 4;

	private final IAnimationSceneModel m_model;

	private final GasSimulationNetwork m_simNetwork;

	public LiquidPipe(String name, IAnimationSceneModel model, GasSimulationNetwork simNetwork) {
		super(name, true);
		m_model = model;
		m_simNetwork = simNetwork;
	}

	@Override
	public GasSimulationNetwork getNetwork() {
		return m_simNetwork;
	}

	@Override
	public Map<Vector2D, GasSimulationNetwork> getLinks() {
		HashMap<Vector2D, GasSimulationNetwork> links = new HashMap<>();
		for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
			if(c.getNetwork() != this.getNetwork() && c.isFreeFlow()) {
				links.put(c.getBody().getLocation().getXy().round(), c.getNetwork());
			}
		}

		return links;
	}

	private void updateModel() {

		HashSet<Direction> directions = new HashSet<>();
		List<String> directionNames = new ArrayList<>();

		for (IDevice w : getConnections()) {
			Vector3F delta = w.getBody().getLocation().difference(getBody().getLocation());
			Direction d = Direction.fromVector(delta.getXy());
			if (d != Direction.Zero && !d.isDiagonal()) {
				directions.add(d);
			}
		}

		for (Direction d : directions) {
			directionNames.add(d.toString());
		}

		Collections.sort(directionNames);

		String animationName = String.join(",", directionNames);

		if (animationName.isEmpty()) {
			animationName = "idle";
		}

		m_model.getAnimation(animationName).setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
	}

	@Override
	public float getVolume() {
		return PIPE_VOLUME;
	}

	@Override
	public boolean isFreeFlow() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}


	@Override
	public void update(int delta) { }

	@Override
	protected void connectionChanged() {
		m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
		updateModel();
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(getConnections().size() >= MAX_CONNECTIONS)
			return false;
		
		if(!(d instanceof ILiquidCarrier))
			return false;

		if(d.getClass().equals(LiquidPipe.class)) {
			boolean sameDepth = Math.abs(this.getBody().getLocation().z - d.getBody().getLocation().z) < Vector2F.TOLERANCE;
			if(!sameDepth)
				return false;

			if(((ILiquidCarrier) d).getNetwork() != this.getNetwork())
				return false;
		}
		
		Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
		Direction dir = Direction.fromVector(delta.getXy());
		
		if(dir.isDiagonal())
			return false;
			
		return true;
	}

}
