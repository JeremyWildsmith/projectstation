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

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.power.IDevice;
import io.github.jevaengine.graphics.IGraphicShader;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.*;
import io.github.jevaengine.world.physics.PhysicsBodyDescription.PhysicsBodyType;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.search.ISearchFilter;
import io.github.jevaengine.world.search.RadialSearchFilter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Infrastructure implements IEntity, IDamageConsumer {
	private static final AtomicInteger m_unnamedCount = new AtomicInteger(0);

	private final String m_name;
	@Nullable
	private final PhysicsBodyDescription m_physicsBodyDescription;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	private final boolean m_isStatic;
	private final boolean m_isTransparent;
	@Nullable
	private final IAnimationSceneModel m_model;
	private IPhysicsBody m_body = new NullPhysicsBody();
	@Nullable
	private World m_world;

	private final boolean m_isAirTight;

	private HashMap<String, Integer> m_flags = new HashMap<>();

	private final Set<String> m_infrastructureType = new HashSet<>();

	private final float m_heatConductivity;
	private final boolean m_isTraversable;

	private int m_hitpoints;
	private final Map<DamageCategory, Integer> m_baseDamage;
	private final Map<DamageSeverity, Integer> m_damageMultiplier;
	private final Map<Integer, String> m_hitpointsAnimationMapping;
	private final HashMap<Infrastructure, Float> m_consumed = new HashMap<>();

	private final IRubbleProducer m_rubbleProducer;

	private boolean m_consumes;

	public Infrastructure(String name, IAnimationSceneModel model, boolean isStatic, boolean isTraversable, String[] infrastructureTypes, boolean isAirTight, boolean isTransparent, float heatConductivity, Map<DamageCategory, Integer> baseDamage, Map<DamageSeverity, Integer> damageMultiplier, int hitpoints, Map<Integer, String> hitpointsAnimation, @Nullable IRubbleProducer rubbleProducer, boolean consumes) {
		m_name = name;
		m_consumes = consumes;
		m_isTransparent = isTransparent;
		m_isAirTight = isAirTight;
		m_infrastructureType.addAll(Arrays.asList(infrastructureTypes));
		m_isStatic = isStatic;
		m_heatConductivity = heatConductivity;
		m_model = model;
		m_isTraversable = isTraversable;

		m_hitpointsAnimationMapping = hitpointsAnimation;
		m_baseDamage = baseDamage;
		m_damageMultiplier = damageMultiplier;
		m_hitpoints = hitpoints;
		m_rubbleProducer = rubbleProducer;
		if (!isTraversable) {
			m_physicsBodyDescription = new PhysicsBodyDescription(PhysicsBodyType.Static, model.getBodyShape(), 1.0F, true, false, 1.0F);
			m_physicsBodyDescription.collisionExceptions = new Class[] {
					Infrastructure.class,
					Door.class
			};
		} else
			m_physicsBodyDescription = null;


		m_bridge = new EntityBridge(this);
		if(consumes)
			getObservers().add(new MovementObserver());
	}

	private void updateAnimation() {
		String animation = null;
		int min = Integer.MAX_VALUE;
		for(Map.Entry<Integer, String> a : m_hitpointsAnimationMapping.entrySet()) {
			if(m_hitpoints <= a.getKey() && a.getKey() < min) {
				animation = a.getValue();
				min = a.getKey();
			}
		}

		if(animation != null && m_model.hasAnimation(animation)) {
			m_model.getAnimation(animation).setState(IAnimationSceneModel.AnimationSceneModelAnimationState.PlayToEnd);
		}
	}

	public boolean isTraversable() {
		return m_isTraversable;
	}

	public float getHeatConductivity() {
		return m_heatConductivity;
	}

	public boolean hasInfrastructureType(String type) {
		return m_infrastructureType.contains(type);
	}

	public String[] getInfrastructureTypes() {
		return m_infrastructureType.toArray(new String[m_infrastructureType.size()]);
	}

	public boolean isAirTight() {
		return m_isAirTight;
	}

	public boolean isTransparent() {
		return m_isTransparent;
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
		Direction dir = m_body.getDirection();
		Vector3F oldPos = m_body.getLocation();
		if (m_physicsBodyDescription == null)
			m_body = new NonparticipantPhysicsBody(this, m_model.getAABB());
		else {
			m_body = m_world.getPhysicsWorld().createBody(this, m_physicsBodyDescription);
			m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
		}

		m_body.setLocation(oldPos);
		m_body.setDirection(dir);
	}

	private void destroyPhysicsBody() {
		Direction dir = m_body.getDirection();

		Vector3F loc = m_body.getLocation();
		m_body.destory();
		m_body = new NonparticipantPhysicsBody();
		m_body.setLocation(loc);
		m_body.setDirection(dir);
		m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
	}

	@Override
	public void consume(DamageDescription damage) {
		int hitpoints = 0;

		for(DamageCategory cat : DamageCategory.values()) {
			DamageSeverity sev = damage.getDamageSeverity(cat);
			if(sev == DamageSeverity.None)
				continue;

			int calcDamage = m_baseDamage.get(cat);
			calcDamage *= m_damageMultiplier.get(sev);

			hitpoints += calcDamage;
		}

		m_hitpoints = Math.max(0, m_hitpoints - hitpoints);

		updateAnimation();


		if(m_hitpoints == 0) {

			if(m_rubbleProducer != null) {
				IEntity rubble = m_rubbleProducer.produce();

				if (rubble != null) {
					m_world.addEntity(rubble);
					rubble.getBody().setLocation(m_body.getLocation());
				}
			}
			releaseConsumed();
			m_world.removeEntity(this);
		}
	}

	@Override
	public boolean isStatic() {
		return m_isStatic;
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
		Direction dir = this.getBody().getDirection();
		if(dir != Direction.Zero)
			m_model.setDirection(this.getBody().getDirection());
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

	public interface IRubbleProducer {
		@Nullable IEntity produce();
	}

	private HashMap<Infrastructure, Float> absorbChildren(Infrastructure i) {
		HashMap<Infrastructure, Float> children = new HashMap<>();

		children.putAll(i.m_consumed);
		for(Infrastructure c : i.m_consumed.keySet()) {
			children.putAll(absorbChildren(c));
		}

		i.m_consumed.clear();
		return children;
	}

	public void releaseConsumed() {
		if(m_consumed.isEmpty())
			return;

		//to prevent reconsuming
		m_consumes = false;
		Infrastructure top = null;
		float depth = 0;

		for(Map.Entry<Infrastructure, Float> e : m_consumed.entrySet()) {
			if(top == null || depth < e.getValue()) {
				depth = e.getValue();
				top = e.getKey();
			}
		}

		top.m_consumed.putAll(m_consumed);
		top.m_consumed.remove(top);
		m_consumed.clear();

		m_world.addEntity(top);
		Vector3F location = new Vector3F(getBody().getLocation());
		location.z = depth;
		top.getBody().setLocation(location);
	}

	private void tryConsumeInfrastructure() {
		if(m_world == null || !m_consumes)
			return;

		ISearchFilter<Infrastructure> searchFilter = new RadialSearchFilter<>(getBody().getLocation().getXy(), 0.1F);
		Infrastructure other[] = getWorld().getEntities().search(Infrastructure.class, searchFilter);
		HashMap<Infrastructure, Float> canidates = new HashMap<>();

		for (Infrastructure i : other) {
			if(!i.m_consumes)
				continue;

			float curDepth = getBody().getLocation().z;
			float otherDepth = i.getBody().getLocation().z;

			if(Math.abs(curDepth - otherDepth) < 0.0001f)
				continue;
			if(curDepth < otherDepth) {
				i.tryConsumeInfrastructure();
				return;
			} else {
				canidates.put(i, otherDepth);
			}
		}

		HashMap<Infrastructure, Float> netCanidates = new HashMap<>(canidates);
		for(Infrastructure i : canidates.keySet()) {
			netCanidates.putAll(absorbChildren(i));
			m_world.removeEntity(i);
		}

		m_consumed.putAll(netCanidates);
	}
	private class MovementObserver implements IPhysicsBodyOrientationObserver, IEntityBodyObserver, IEntityWorldObserver {
		@Override
		public void locationSet() {
			tryConsumeInfrastructure();
		}

		@Override
		public void directionSet() { }

		@Override
		public void bodyChanged(IPhysicsBody oldBody, IPhysicsBody newBody) {
			newBody.getObservers().add(this);
		}

		@Override
		public void enterWorld() {
			Infrastructure.this.getBody().getObservers().add(this);
		}

		@Override
		public void leaveWorld() {

		}
	}
}
