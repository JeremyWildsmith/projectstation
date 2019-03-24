/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.math.*;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.camera.ICamera;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jeremy
 */
public class CameraBehaviorInjector extends WindowBehaviourInjector {
	private final ICamera m_camera;
	private final IEntity m_player;

	public CameraBehaviorInjector(IEntity player, ICamera camera) {
		m_camera = camera;
		m_player = player;
	}

	private Set<Vector2D> getObstructionMap() {

		if(m_player.getWorld() == null)
			return new HashSet<>();

		Set<Vector2D> obstructionMap = new HashSet<>();
		Infrastructure[] entities = m_player.getWorld().getEntities().search(Infrastructure.class, new RadialSearchFilter<>(m_player.getBody().getLocation().getXy(), 50f));
		Door[] doors = m_player.getWorld().getEntities().search(Door.class, new RadialSearchFilter<>(m_player.getBody().getLocation().getXy(), 50f));

		for(Infrastructure e : entities) {
			if(e.getBody().isCollidable() && !e.isTransparent())
				obstructionMap.add(e.getBody().getLocation().getXy().round());
		}

		for(Door d : doors) {
			if(!d.isOpen())
				obstructionMap.add(d.getBody().getLocation().getXy().round());
		}

		return obstructionMap;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");
		final Timer lightingUpdateTimer = new Timer();
		final HashSet<Vector2F> visible = new HashSet<>();

		lightingUpdateTimer.getObservers().add(new Timer.ITimerObserver() {
			private static final int refreshInterval = 250;
			private int lastRefresh = 250;
			@Override
			public void update(int deltaTime) {
				lastRefresh += deltaTime;

				if(lastRefresh >= refreshInterval) {
					lastRefresh = 0;
					visible.clear();
					Vector2F playerLocation = m_player.getBody().getLocation().getXy();

					Set<Vector2D> obstructionMap = getObstructionMap();

					visible.add(playerLocation);
					for(float rot = 0; rot <= 2.01 * Math.PI; rot += (float)Math.PI / 16.0f) {
						Vector2F unitVector = new Vector2F(1, 0).rotate(rot);
						Vector2F addition = unitVector.multiply(0.5f);

						for(int i = 0; i < 30; i++) {
							Vector2F test = new Vector2F(playerLocation);
							test = test.add(unitVector.multiply(0.5f * Math.max(0, i - 1)));
							test = test.add(addition);

							if(obstructionMap.contains(test.round()))
								break;

							visible.add(test);
						}
					}
				}
			}
		});

		addControl(lightingUpdateTimer);
		demoWorldView.setCamera(m_camera);
		m_camera.addEffect(new LightingMap(visible));
	}

	private static final class LightingMap implements ISceneBuffer.ISceneBufferEffect {
		private final Set<Vector2F> m_visible;

		public LightingMap(Set<Vector2F> visible) {
			m_visible = visible;
		}

		@Override
		public ISceneBuffer.ISceneComponentEffect[] getComponentEffect(Graphics2D g, int offsetX, int offsetY, float scale, Vector2D renderLocation, Matrix3X3 projection, ISceneBuffer.ISceneBufferEntry subject, Collection<ISceneBuffer.ISceneBufferEntry> beneath) {
			boolean found = false;

			for(Vector2F v : m_visible) {
				float diff = v.difference(subject.getDispatcher().getBody().getLocation().getXy()).getLength();

				if (diff <= 1.5f) {
					found = true;
				}
			}

			if(found)
				return new ISceneBuffer.ISceneComponentEffect[0];

			if(subject.getDispatcher() instanceof Infrastructure || subject.getDispatcher() instanceof Door) {
				return new ISceneBuffer.ISceneComponentEffect[] { new PartiallyHideInfrastructure(g, projection, renderLocation, offsetX, offsetY, scale)};
			}

			return new ISceneBuffer.ISceneComponentEffect[] { new HideInvisibleComponents() } ;
		}
	}

	private static final class HideInvisibleComponents implements ISceneBuffer.ISceneComponentEffect {

		@Override
		public void prerender() {

		}

		@Override
		public void postrender() {

		}

		@Override
		public boolean ignore(IEntity dispatcher, IImmutableSceneModel.ISceneModelComponent c) {
			return true;
		}
	}


	private static final class PartiallyHideInfrastructure implements ISceneBuffer.ISceneComponentEffect {
		private Graphics2D g;
		private Matrix3X3 projection;
		private Vector2D renderLocation;
		private int offsetX;
		private int offsetY;
		private float scale;

		public PartiallyHideInfrastructure(Graphics2D g, Matrix3X3 projection, Vector2D renderLocation, int offsetX, int offsetY, float scale) {
			this.g = g;
			this.projection = projection;
			this.renderLocation = renderLocation;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.scale = scale;
		}

		@Override
		public void prerender() {

		}

		@Override
		public void postrender() {
			Vector3F sz = projection.dot(new Vector3F(1, 1, 0)).multiply(scale);
			Vector3F offset = projection.dot(new Vector3F(-0.5f, -.5f, 0)).multiply(scale);
			g.setColor(new Color(0,0,0,100));
			g.fillRect(renderLocation.x + offsetX + (int)offset.x, renderLocation.y + offsetY + (int)offset.y, (int)sz.x, (int)sz.y);
		}

		@Override
		public boolean ignore(IEntity dispatcher, IImmutableSceneModel.ISceneModelComponent c) {
			return false;
		}
	}

}
