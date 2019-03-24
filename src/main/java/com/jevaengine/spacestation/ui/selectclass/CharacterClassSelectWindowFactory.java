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
package com.jevaengine.spacestation.ui.selectclass;

import com.jevaengine.spacestation.StationProjectionFactory;
import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.NullVariable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.TiledEffectMapFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.NullEntityFactory;
import io.github.jevaengine.world.physics.NullPhysicsWorldFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CharacterClassSelectWindowFactory {

	private static final URI SELECT_CLASS_VIEW_WINDOW = URI.create("file:///ui/windows/selectClass.jwl");

	private static final float CAMERA_ZOOM = 2.5f;

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public CharacterClassSelectWindowFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public CharacterSelectClassWindow create(IEntityFactory entityFactory, CharacterClassDescription descriptions[]) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(SELECT_CLASS_VIEW_WINDOW);
		m_windowManager.addWindow(window);

		try {
			Map<CharacterClassDescription, IEntity> desc = new HashMap<>();

			for(CharacterClassDescription d : descriptions) {
				IEntity e = entityFactory.create("character", "demo", d.demo);
				desc.put(d, e);
			}

			new SelectClassBehaviorInjector(desc, observers).inject(window);
		} catch (NoSuchControlException | IEntityFactory.EntityConstructionException ex) {
			throw new WindowConstructionException(SELECT_CLASS_VIEW_WINDOW, ex);
		}
		

		window.center();

		return new CharacterSelectClassWindow(window, observers);
	}

	private class SelectClassBehaviorInjector extends WindowBehaviourInjector {
		private final Map<CharacterClassDescription, IEntity> m_descriptions;
		private final ControlledCamera m_camera = new ControlledCamera(new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create()));
		private final World m_world = new World(10, 10, 1, 1,  1, new IWeatherFactory.NullWeather(), new NullPhysicsWorldFactory(), new TiledEffectMapFactory(), new NullEntityFactory(), null);
		private Iterator<Map.Entry<CharacterClassDescription, IEntity>> m_displayIndex;
		private CharacterClassDescription m_displayed = null;
		private TextArea m_desc;
		private final Observers m_observers;

		public SelectClassBehaviorInjector(Map<CharacterClassDescription, IEntity> descriptions, Observers observers) {
			m_descriptions = descriptions;
			m_displayIndex = descriptions.entrySet().iterator();
			m_observers = observers;
		}

		private void displayClass(CharacterClassDescription d) {
			for(IEntity e : m_world.getEntities().all())
				m_world.removeEntity(e);

			IEntity e = m_descriptions.get(d);
			m_world.addEntity(e);
			e.getBody().setDirection(Direction.YPlus);
			m_desc.setText("Class: " + d.name + "\n\n" + d.description);

			m_displayed = d;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final WorldView worldView = getControl(WorldView.class, "worldView");
			m_camera.attach(m_world);
			m_camera.setZoom(CAMERA_ZOOM);
			worldView.setCamera(m_camera);
			m_desc = getControl(TextArea.class, "description");

			displayNextClass();
			getControl(Button.class, "btnNext").getObservers().add(new Button.IButtonPressObserver() {
				@Override
				public void onPress() {
					displayNextClass();
				}
			});

			getControl(Button.class, "btnConfirm").getObservers().add(new Button.IButtonPressObserver() {
				@Override
				public void onPress() {
					if(m_displayed == null)
						return;

					m_observers.raise(ICharacterClassSelectObserver.class).selectedClass(m_displayed);

				}
			});
		}

		private void displayNextClass() {
			if(m_descriptions.isEmpty())
				return;

			if(!m_displayIndex.hasNext()) {
				m_displayIndex = m_descriptions.entrySet().iterator();
			}

			displayClass(m_displayIndex.next().getKey());
		}
	}

	public static class CharacterSelectClassWindow implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private CharacterSelectClassWindow(Window window, Observers observers) {
			m_window = window;
			m_observers = observers;
		}

		@Override
		public void dispose() {
			m_window.dispose();
		}

		public Observers getObservers() {
			return m_observers;
		}
		
		public void setVisible(boolean isVisible) {
			m_window.setVisible(isVisible);
		}

		public boolean isVisible() {
			return m_window.isVisible();
		}

		public void setLocation(Vector2D location) {
			m_window.setLocation(location);
		}
		
		public Vector2D getLocation() {
			return m_window.getLocation();
		}

		public void center() {
			m_window.center();
		}

		public void focus() {
			m_window.focus();
		}

		public void setMovable(boolean isMovable) {
			m_window.setMovable(false);
		}

		public void setTopMost(boolean isTopMost) {
			m_window.setTopMost(isTopMost);
		}

		public Rect2D getBounds() {
			return m_window.getBounds();
		}
	}

	public interface ICharacterClassSelectObserver {
		void selectedClass(CharacterClassDescription desc);
	}
}
