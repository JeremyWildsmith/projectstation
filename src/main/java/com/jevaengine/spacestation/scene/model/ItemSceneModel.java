/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.scene.model;

import io.github.jevaengine.graphics.IImmutableGraphic;
import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class ItemSceneModel implements ISceneModel {
	private final IImmutableGraphic m_graphic;
	private final Rect3F m_aabb;
	
	public ItemSceneModel(IItem item) {
		this(item.getIcon(), new Rect3F(1, 1, 0.01F));
	}
	
	private ItemSceneModel(IImmutableGraphic graphic, Rect3F bounds) {
		m_graphic = graphic;
		m_aabb = new Rect3F(1, 1, 0);
	}
	
	@Override
	public ISceneModel clone() throws SceneModelNotCloneableException {
		return new ItemSceneModel(m_graphic, m_aabb);
	}

	@Override
	public Collection<ISceneModelComponent> getComponents(Matrix3X3 projection) {
		ISceneModelComponent component = new ISceneModelComponent() {
			@Override
			public String getName() {
				return "icon";
			}

			@Override
			public boolean testPick(int x, int y, float scale) {
				return m_graphic.pickTest(x, y);
			}

			@Override
			public Rect3F getBounds() {
				return m_aabb;
			}

			@Override
			public Vector3F getOrigin() {
				return new Vector3F(-0.5F, -0.5F, 0);
			}

			@Override
			public void render(Graphics2D g, int x, int y, float scale) {
				int offX = 0;//-m_graphic.getBounds().width / 2;
				int offY = 0;//-m_graphic.getBounds().height / 2;
				m_graphic.render(g, x + (int)(offX * scale), (int)(y + offY * scale), scale);
			}
		};
		
		List<ISceneModelComponent> components = new ArrayList<>();
		components.add(component);
		
		return components;
	}

	@Override
	public Rect3F getAABB() {
		return new Rect3F(m_aabb);
	}

	@Override
	public Direction getDirection() {
		return Direction.Zero;
	}

	@Override
	public IObserverRegistry getObservers() {
		return new Observers();
	}

	@Override
	public PhysicsBodyShape getBodyShape() {
		return new PhysicsBodyShape(PhysicsBodyShape.PhysicsBodyShapeType.Box, m_aabb);
	}

	@Override
	public void update(int deltaTime) {
	}

	@Override
	public void setDirection(Direction direction) { }

	@Override
	public void dispose() { }
	
}
