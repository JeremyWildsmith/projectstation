/*
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.rpg.item.DefaultItemSlot;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
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
import io.github.jevaengine.world.physics.PhysicsBodyDescription.PhysicsBodyType;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ItemLocker implements IEntity, IItemStore {
	private static final AtomicInteger m_unnamedCount = new AtomicInteger(0);

	private final String m_name;
	@Nullable
	private final PhysicsBodyDescription m_physicsBodyDescription;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	@Nullable
	private final ISceneModel m_model;
	private IPhysicsBody m_body = new NullPhysicsBody();
	@Nullable
	private World m_world;

	private HashMap<String, Integer> m_flags = new HashMap<>();

	private final List<IItemSlot> m_slots = new ArrayList<>();

	public ItemLocker(String instanceName, ISceneModel model, int capacity) {
		m_name = instanceName;
		m_model = model;

		m_physicsBodyDescription = new PhysicsBodyDescription(PhysicsBodyType.Static, model.getBodyShape(), 1.0F, true, false, 1.0F);

		m_bridge = new EntityBridge(this);

		for(int i = 0; i < capacity; i++) {
			m_slots.add(new DefaultItemSlot());
		}
	}

	@Override
	public IItemSlot[] getSlots() {
		return m_slots.toArray(new IItemSlot[m_slots.size()]);
	}

	@Override
	public IItemSlot getEmptySlot() {
		for(IItemSlot s : m_slots) {
			if(s.isEmpty())
				return s;
		}

		return null;
	}

	@Override
	public boolean hasItem(IItem item) {
		for(IItemSlot s : m_slots) {
			if(!s.isEmpty() && s.getItem() == item)
				return true;
		}

		return false;
	}

	@Override
	public boolean addItem(IItem item) {
		IItemSlot freeSlot = getEmptySlot();

		if(freeSlot == null)
			return false;

		freeSlot.setItem(item);

		return true;
	}

	@Override
	public boolean removeItem(IItem item) {
		for(IItemSlot s : m_slots) {
			if(!s.isEmpty() && s.getItem() == item) {
				s.clear();
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isFull() {
		return getEmptySlot() == null;
	}

	@Override
	public void dispose() {
		if (m_world != null)
			m_world.removeEntity(this);

		m_observers.clear();
	}

	@Override
	public String getInstanceName() {
		return m_name;
	}

	@Override
	public final World getWorld() {
		return m_world;
	}

	@Override
	public final void associate(World world) {
		if (m_world != null)
			throw new WorldAssociationException("Already associated with world");

		m_world = world;

		constructPhysicsBody();
		m_observers.raise(IEntityWorldObserver.class).enterWorld();
	}

	@Override
	public final void disassociate() {
		if (m_world == null)
			throw new WorldAssociationException("Not associated with world");

		m_observers.raise(IEntityWorldObserver.class).leaveWorld();

		destroyPhysicsBody();

		m_world = null;
	}

	private void constructPhysicsBody() {
		if (m_physicsBodyDescription == null)
			m_body = new NonparticipantPhysicsBody(this, m_model.getAABB());
		else {
			m_body = m_world.getPhysicsWorld().createBody(this, m_physicsBodyDescription);
			m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
		}
	}

	private void destroyPhysicsBody() {
		m_body.destory();
		m_body = new NullPhysicsBody();
		m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public final IPhysicsBody getBody() {
		return m_body;
	}

	@Override
	public void update(int deltaTime) {
		m_model.update(deltaTime);
	}

	@Override
	@Nullable
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	@Override
	public Map<String, Integer> getFlags() {
		return m_flags;
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
	public IEntityTaskModel getTaskModel() {
		return new NullEntityTaskModel();
	}
}
