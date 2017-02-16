/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Rect2F;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.NullPhysicsBody;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.search.RectangleSearchFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class Wire implements IEntity {

	private final String m_name;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	private final IAnimationSceneModel m_model;
	
	private final HashMap<String, Integer> m_flags = new HashMap<>();
	
	private final List<Wire> m_connections = new ArrayList<>();
	
	private World m_world;
	private IPhysicsBody m_body;
	
	public Wire(String name, IAnimationSceneModel model) {
		m_name = name;
		m_model = model;
		
		m_body = new NullPhysicsBody();
		m_bridge = new EntityBridge(this);
	}

	private void constructPhysicsBody() {
		m_body = new NonparticipantPhysicsBody(this);

		m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
	}

	private void destroyPhysicsBody() {
		m_body.destory();
		m_body = new NullPhysicsBody();

		//Since we are destroying the old body, there is nothing the observer can
		//or needs to do to the old body (ie, remove observers.) Thus, it is effectivley
		//equivlent to passing NullPhysicsBody.
		m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), new NullPhysicsBody());
	}

	private void removeConnection(Wire wire) {
		if(!m_connections.contains(wire))
			return;
		
		m_connections.remove(wire);
		wire.removeConnection(this);
		updateModel();
	}
	
	private void addConnection(Wire wire) {
		if(m_connections.contains(wire))
			return;
		
		m_connections.add(wire);
		wire.addConnection(this);
		updateModel();
	}
	
	private void clearConnections() {
		while(!m_connections.isEmpty())
			removeConnection(m_connections.get(0));
	}
	
	private void updateModel() {
		
		HashSet<Direction> directions = new HashSet<>();
		List<String> directionNames = new ArrayList<>();
		
		for(Wire w : m_connections) {
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
	
	private void updateConnections() {
		clearConnections();
		
		Rect2F rect = new Rect2F(3, 3).add(m_body.getLocation().getXy()).add(new Vector2F(-1, -1));
		Wire wires[] = m_world.getEntities().search(Wire.class, new RectangleSearchFilter<Wire>(rect));
	
		for(Wire w : wires) {
			if(w != this)
				addConnection(w);
		}
	}
	
	@Override
	public World getWorld() {
		return m_world;
	}

	@Override
	public final void associate(World world)
	{
		if (m_world != null)
			throw new WorldAssociationException("Already associated with world");

		m_world = world;

		m_observers.raise(IEntityWorldObserver.class).enterWorld();
		
		constructPhysicsBody();
		updateConnections();
		m_body.getObservers().add(new MovementObserver());
	}

	@Override
	public final void disassociate()
	{
		if (m_world == null)
			throw new WorldAssociationException("Not associated with world");

		destroyPhysicsBody();

		m_observers.raise(IEntityWorldObserver.class).leaveWorld();

		m_world = null;
	}

	@Override
	public String getInstanceName() {
		return m_name;
	}

	@Override
	public Map<String, Integer> getFlags() {
		return Collections.unmodifiableMap(m_flags);
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
	public IPhysicsBody getBody() {
		return m_body;
	}

	@Override
	public IEntityTaskModel getTaskModel() {
		return new NullEntityTaskModel();
	}

	@Override
	public IObserverRegistry getObservers() {
		return m_observers;
	}

	@Override
	public EntityBridge getBridge() {
		return m_bridge;
	}

	@Override
	public void update(int delta) {
	
	}

	@Override
	public void dispose() {
		m_observers.clear();

		if (m_world != null) {
			m_world.removeEntity(this);
		}
	}

	private class MovementObserver implements IPhysicsBodyOrientationObserver {
		@Override
		public void locationSet() {
			updateConnections();
		}

		@Override
		public void directionSet() { }
	}
}
