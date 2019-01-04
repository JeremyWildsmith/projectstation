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
package com.jevaengine.spacestation.ui;

import com.jevaengine.spacestation.entity.power.Dcpu;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.util.Observers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

public final class LemDisplayFactory {

	private final URI m_layout;

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public LemDisplayFactory(WindowManager windowManager, IWindowFactory windowFactory, URI layout) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_layout = layout;
	}

	public LemDisplay create(Dcpu consoleInterface) throws WindowConstructionException {
		Observers observers = new Observers();

		Window window = m_windowFactory.create(m_layout, new LemDisplayBehaviorInjector(observers, consoleInterface));
		m_windowManager.addWindow(window);

		window.center();

		return new LemDisplay(window, observers);
	}

	public static class LemDisplay implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private LemDisplay(Window window, Observers observers) {
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

	private class LemDisplayBehaviorInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final Dcpu m_consoleInterface;

		public LemDisplayBehaviorInjector(final Observers observers, Dcpu consoleInterface) {
			m_observers = observers;
			m_consoleInterface = consoleInterface;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final Viewport displayView = getControl(Viewport.class, "displayView");
			final ToggleIcon togglePower = getControl(ToggleIcon.class, "togglePower");
			
			final Timer timer = new Timer();

			addControl(timer);
			
			displayView.setView(new IRenderable() {
				@Override
				public void render(Graphics2D g, int x, int y, float scale) {
					BufferedImage screen = m_consoleInterface.getScreen();

					if (!m_consoleInterface.isOn()) {
						g.setColor(Color.darkGray);
						g.fillRect(x, y, displayView.getBounds().width, displayView.getBounds().height);	
					} else if (screen == null) {
						g.setColor(Color.black);
						g.fillRect(x, y, displayView.getBounds().width, displayView.getBounds().height);
					} else {
						RenderingHints rh = new RenderingHints(
								RenderingHints.KEY_TEXT_ANTIALIASING,
								RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
						RenderingHints old = g.getRenderingHints();
						g.setRenderingHints(rh);
						g.drawImage(screen, x, y, displayView.getBounds().width, displayView.getBounds().height, null);
						g.setRenderingHints(old);
					}
				}
			});

			getObservers().add(new Window.IWindowInputObserver() {
				@Override
				public void onKeyEvent(InputKeyEvent event) {
					if (event.type == InputKeyEvent.KeyEventType.KeyTyped) {
						m_consoleInterface.simulateKeyTyped(event.keyCode, event.keyChar);
					}
				}

				@Override
				public void onMouseEvent(InputMouseEvent event) {
				}
			});
			
			timer.getObservers().add(new Timer.ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					togglePower.setActive(m_consoleInterface.isOn());
				}
			});
			
			togglePower.getObservers().add(new ToggleIcon.IToggleIconObserver() {
				@Override
				public void toggled() {
					if(togglePower.isActive())
						m_consoleInterface.turnOn();
					else
						m_consoleInterface.turnOff();
				}
			});
		}
	}

	public interface IHudObserver {

		void movementSpeedChanged(boolean isRunning);

		void inventoryViewChanged(boolean isVisible);
	}
}
