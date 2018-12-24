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

import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.entity.IEntity;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.net.URI;

public final class GasDebugFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/debug/gas/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public GasDebugFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public GasDebug create(IEntity follow, GasSimulationNetwork network) throws WindowConstructionException {
		Observers observers = new Observers();

		Window window = m_windowFactory.create(HUD_WINDOW, new GasDebugBehaviorInjector(network, observers, follow));
		m_windowManager.addWindow(window);

		window.center();

		return new GasDebug(window, observers);
	}

	public static class GasDebug implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private GasDebug(Window window, Observers observers) {
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
			m_window.setMovable(isMovable);
		}

		public void setTopMost(boolean isTopMost) {
			m_window.setTopMost(isTopMost);
		}

		public Rect2D getBounds() {
			return m_window.getBounds();
		}
	}

	private class GasDebugBehaviorInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final IEntity m_follow;
		private final GasSimulationNetwork m_network;

		public GasDebugBehaviorInjector(final GasSimulationNetwork network, final Observers observers, IEntity follow) {
			m_observers = observers;
			m_follow = follow;
			m_network = network;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final Viewport displayView = getControl(Viewport.class, "displayView");
			final Label desc = getControl(Label.class, "description");
			desc.setText("Network: " + m_network.name());

			displayView.setView(new IRenderable() {
				@Override
				public void render(Graphics2D g, int x, int y, float scale) {
					if(m_follow.getWorld() == null)
						return;

					GasSimulationEntity sim = m_follow.getWorld().getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);

					g.setColor(Color.black);
					g.fillRect(x, y, displayView.getBounds().width, displayView.getBounds().height);
					Rect2D bounds = displayView.getBounds();

					g.setColor(Color.WHITE);

					final int SPACING = 40;

					for(int xSide = 1; xSide >= -1; xSide -=2){
						for(int ySide = 1; ySide >= -1; ySide -=2){
							for(int xOffset = 0; xOffset * SPACING < bounds.width/2; xOffset++){
								for(int yOffset = 0; yOffset * SPACING < bounds.height/2; yOffset++){
									Vector2D origin = new Vector2D(bounds.width / 2, bounds.height / 2);

									Vector2D offset = new Vector2D(xOffset * xSide,yOffset* ySide);

									if(offset.isZero())
										g.setColor(Color.red);
									else
										g.setColor(Color.white);

									Vector2D renderLocation = origin.add(offset.multiply(SPACING));
									Vector2D testLocation = m_follow.getBody().getLocation().getXy().round().add(offset);

									float volume = sim.getVolume(m_network, testLocation);
									//Pressure
									String s = String.format("%.3f", sim.sample(m_network, testLocation).calculatePressure(volume) / 1000);
									//Mols
									//String s = String.format("%.3f", sim.sample(m_network, testLocation).getTotalMols());
									//Temperature
									//String s = String.format("%.3f", sim.sample(m_network, testLocation).temperature);
									AffineTransform t = g.getTransform();
									g.setTransform(AffineTransform.getScaleInstance(0.8f, 0.8));
									g.drawString(s, (x + renderLocation.x) / .8F, (y + renderLocation.y) / .8F);
									g.setTransform(t);
								}
							}
						}
					}
					g.setColor(Color.white);
				}
			});
		}
	}

	public interface IHudObserver {

		void movementSpeedChanged(boolean isRunning);

		void inventoryViewChanged(boolean isVisible);
	}
}
