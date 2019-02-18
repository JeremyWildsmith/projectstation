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
import com.jevaengine.spacestation.gas.IGasSimulationCluster;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.entity.IEntity;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.net.URI;
import java.util.*;
import java.util.List;

public final class GasClusterMapDebugFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/debug/gas/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public GasClusterMapDebugFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public GasClusterMapDebug create(IEntity follow, GasSimulationNetwork network) throws WindowConstructionException {
		Observers observers = new Observers();

		Window window = m_windowFactory.create(HUD_WINDOW, new GasClusterMapDebugBehaviorInjector(network, observers, follow));
		m_windowManager.addWindow(window);

		window.center();

		return new GasClusterMapDebug(window, observers);
	}

	public static class GasClusterMapDebug implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private GasClusterMapDebug(Window window, Observers observers) {
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

	private class GasClusterMapDebugBehaviorInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final IEntity m_follow;
		private final GasSimulationNetwork m_network;

		private static final int UPDATE_INTERVAL = 2000;
		private int lastUpdate = 0;
		Map<IGasSimulationCluster, Color> colouring = new HashMap<>();

		public GasClusterMapDebugBehaviorInjector(final GasSimulationNetwork network, final Observers observers, IEntity follow) {
			m_observers = observers;
			m_follow = follow;
			m_network = network;
		}

		private Map<IGasSimulationCluster, Color> getClusterColouring() {
			Map<IGasSimulationCluster, Color> colouring = new HashMap<>();
			Color[] possible = new Color[] {
					Color.RED,
					Color.GREEN,
					Color.BLUE,
					Color.YELLOW,
					Color.MAGENTA,
					Color.ORANGE,
					Color.LIGHT_GRAY,
					Color.DARK_GRAY,
					Color.PINK
			};

			GasSimulationEntity sim = m_follow.getWorld().getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
			Collection<IGasSimulationCluster> clusters = sim.getClusters(m_network);

			for(IGasSimulationCluster c : clusters) {
				if(colouring.containsKey(c))
					continue;

				List<Color> avail  = new ArrayList<>();
				avail.addAll(Arrays.asList(possible));

				for(IGasSimulationCluster child : c.getConnections()) {
					if(colouring.containsKey(child))
						avail.remove(colouring.get(child));
				}

				Color clusterColour = Color.WHITE;
				if(!avail.isEmpty()) {
					clusterColour = avail.get(0);
				}

				colouring.put(c, clusterColour);
			}

			return colouring;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final Viewport displayView = getControl(Viewport.class, "displayView");
			final Label desc = getControl(Label.class, "description");
			final Timer timer = new Timer();
			desc.setText("Network: " + m_network.name());

			addControl(timer);
			timer.getObservers().add(new Timer.ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					lastUpdate += deltaTime;

					if(lastUpdate > UPDATE_INTERVAL) {
						colouring = getClusterColouring();
						lastUpdate = 0;
					}
				}
			});

			displayView.setView(new IRenderable() {
				@Override
				public void render(Graphics2D g, int x, int y, float scale) {
					if(m_follow.getWorld() == null)
						return;

					GasSimulationEntity sim = m_follow.getWorld().getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);

					if(sim == null)
						return;

					Rect2D renderBounds = displayView.getBounds();
					Vector2D displayPos = displayView.getAbsoluteLocation();

					final int BOUNDS = (int)(60 * scale);
					Vector2D offset = m_follow.getBody().getLocation().getXy().round().multiply(BOUNDS).difference(
							new Vector2D(renderBounds.width / 2, renderBounds.height / 2)
					);


					Shape oldClip = g.getClip();
					g.clipRect(displayPos.x, displayPos.y, renderBounds.width, renderBounds.height);
					g.translate(-offset.x, -offset.y);

					for(Map.Entry<IGasSimulationCluster, Color> cluster : colouring.entrySet()) {
						g.setColor(cluster.getValue());

						for(Vector2D v : cluster.getKey().getLocations()) {
							int locX = (int)(v.x * BOUNDS + x);
							int locY = (int)(v.y * BOUNDS + y);

							g.fillRect(locX, locY,  BOUNDS,  BOUNDS);

							if(v.equals(m_follow.getBody().getLocation().getXy().round())) {
								g.setColor(Color.WHITE);
								g.fillRect(locX + 2, locY + 2, BOUNDS - 4, BOUNDS - 4);
								g.setColor(cluster.getValue());
							}

						}
					}

					g.setColor(Color.black);
					for(Map.Entry<IGasSimulationCluster, Color> cluster : colouring.entrySet()) {
						if(cluster.getKey().getLocations().length > 0) {
							Vector2D start = cluster.getKey().getLocations()[0];
							start = start.multiply(BOUNDS).add(new Vector2D(x, y)).add(new Vector2D((int)(BOUNDS / 2), (int)(BOUNDS / 2)));

							g.drawString(String.format("%.2f", cluster.getKey().getVolume()), start.x, start.y);

							for(IGasSimulationCluster child : cluster.getKey().getConnections()) {
								if(child.getLocations().length <= 0 || !colouring.containsKey(child))
									continue;

								Vector2D end = child.getLocations()[0];
								end = end.multiply(BOUNDS).add(new Vector2D(x, y)).add(new Vector2D((int)(BOUNDS / 2), (int)(BOUNDS / 2)));
								g.drawLine(start.x, start.y, end.x, end.y);
							}
						}
					}

					g.translate(offset.x, offset.y);
					g.setClip(oldClip);
				}
			});
		}
	}

	public interface IHudObserver {

		void movementSpeedChanged(boolean isRunning);

		void inventoryViewChanged(boolean isVisible);
	}
}
