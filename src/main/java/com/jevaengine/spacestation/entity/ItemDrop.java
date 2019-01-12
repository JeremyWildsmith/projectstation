/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.scene.model.ItemSceneModel;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
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
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Jeremy
 */
public final class ItemDrop implements IEntity, IInteractableEntity {

	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger(0);
	
	private final String m_name;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	
	private final HashMap<String, Integer> m_flags = new HashMap<>();
	
	private World m_world;
	private IPhysicsBody m_body;
	
	private final IItem m_item;
	
	private final ISceneModel m_model;
	
	public ItemDrop(IItem item) {
		this(ItemDrop.class.getName() + m_unnamedEntityCount.getAndIncrement(), item);
	}
	
	public ItemDrop(String name, IItem item) {
		m_name = name;
		m_item = item;
		
		m_model = new ItemSceneModel(item);
		
		m_body = new NullPhysicsBody();
		m_bridge = new EntityBridge(this);
	}

	public IItem getItem() {
		return m_item;
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

	@Override
	public final void dispose() {
		m_observers.clear();

		if (m_world != null) {
			m_world.removeEntity(this);
		}
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
	public void update(int delta) { }

	@Override
	public void interactedWith(IRpgCharacter subject) {
		IItemStore inventory = subject.getInventory();
		
		IItemSlot emptySlot = inventory.getEmptySlot();
		
		if(emptySlot != null)
		{
			emptySlot.setItem(m_item);
			dispose();
		}
	}

	@Override
	public void interactWith(IRpgCharacter subject, String interaction) {
		interactedWith(subject);
	}

	@Override
	public String[] getInteractions() {
		return new String[0];
	}
}
