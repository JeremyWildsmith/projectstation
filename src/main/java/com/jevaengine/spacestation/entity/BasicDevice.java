/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.NullPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public abstract class BasicDevice implements IEntity, IDevice {

	private final String m_name;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	
	private final HashMap<String, Integer> m_flags = new HashMap<>();
	private final List<IDevice> m_connections = new ArrayList<>();
	
	private World m_world;
	private IPhysicsBody m_body;
	
	private final boolean m_isTraversable;
	
	public BasicDevice(String name, boolean isTraversable) {
		m_name = name;
		
		m_body = new NullPhysicsBody();
		m_bridge = new EntityBridge(this);
		m_isTraversable = isTraversable;
	}

	private void constructPhysicsBody() {
		PhysicsBodyDescription physicsBodyDescription = new PhysicsBodyDescription(PhysicsBodyDescription.PhysicsBodyType.Static, getModel().getBodyShape(), 1.0F, true, false, 1.0F);
		
		if(m_isTraversable)
			m_body = new NonparticipantPhysicsBody(this);
		else
			m_body = m_world.getPhysicsWorld().createBody(physicsBodyDescription);
		
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

	@Override
	public final void removeConnection(IDevice wire) {
		if(!m_connections.contains(wire))
			return;
		
		m_connections.remove(wire);
		wire.removeConnection(this);
		connectionChanged();
	}
	
	@Override
	public final void clearConnections() {
		while(!m_connections.isEmpty())
			removeConnection(m_connections.get(0));
	}
	
	@Override
	public final boolean addConnection(IDevice wire) {
		if(!canConnectTo(wire))
			return false;
		
		if(m_connections.contains(wire))
			return true;
		
		m_connections.add(wire);
		if(!wire.addConnection(this))
		{
			m_connections.remove(wire);
			return false;
		}
		
		connectionChanged();
		return true;
	}
	
	protected final List<IDevice> getConnections() {
		return new ArrayList<>(m_connections);
	}
	
	protected final <T extends IDevice> List<T> getConnections(Class<T> device) {
		List<T> devices = new ArrayList<>();
		
		for(IDevice d : m_connections)
		{
			if(device.isAssignableFrom(d.getClass()))
				devices.add((T)d);
		}
		
		return devices;
	}
	
	@Override
	public final World getWorld() {
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
	}

	@Override
	public final void disassociate()
	{
		clearConnections();
		
		if (m_world == null)
			throw new WorldAssociationException("Not associated with world");

		destroyPhysicsBody();

		m_observers.raise(IEntityWorldObserver.class).leaveWorld();

		m_world = null;
	}

	@Override
	public final String getInstanceName() {
		return m_name;
	}

	@Override
	public final Map<String, Integer> getFlags() {
		return Collections.unmodifiableMap(m_flags);
	}

	@Override
	public final IPhysicsBody getBody() {
		return m_body;
	}

	@Override
	public final IEntityTaskModel getTaskModel() {
		return new NullEntityTaskModel();
	}

	@Override
	public final IObserverRegistry getObservers() {
		return m_observers;
	}

	@Override
	public final EntityBridge getBridge() {
		return m_bridge;
	}
	
	protected abstract void connectionChanged();
	protected abstract boolean canConnectTo(IDevice d);

	@Override
	public final void dispose() {
		m_observers.clear();

		if (m_world != null) {
			m_world.removeEntity(this);
		}
	}
}
