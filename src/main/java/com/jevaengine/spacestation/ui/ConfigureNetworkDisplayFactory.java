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

import com.jevaengine.spacestation.entity.network.NetworkDevice;
import com.jevaengine.spacestation.entity.power.Dcpu;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.TextArea;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

public final class ConfigureNetworkDisplayFactory {

	private final URI m_layout;

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ConfigureNetworkDisplayFactory(WindowManager windowManager, IWindowFactory windowFactory, URI layout) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_layout = layout;
	}

	public ConfigureNetworkDisplay create(int defaultIp) throws WindowConstructionException {
		Observers observers = new Observers();

		Window window = m_windowFactory.create(m_layout, new ConfigureNetworkDisplayBehaviorInjector(observers, defaultIp));
		m_windowManager.addWindow(window);

		window.center();

		return new ConfigureNetworkDisplay(window, observers);
	}

	public static class ConfigureNetworkDisplay implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private ConfigureNetworkDisplay(Window window, Observers observers) {
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

	private class ConfigureNetworkDisplayBehaviorInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final int m_defaultIp;

		public ConfigureNetworkDisplayBehaviorInjector(final Observers observers, int defaultIp) {
			m_observers = observers;
			m_defaultIp = defaultIp;
		}

		private String decodeIp(int ip) {
			int[] components = new int[4];

			for(int i = 0; i < components.length; i++) {
				components[components.length - 1 - i] = (ip & 0xF);
				ip >>= 4;
			}

			StringBuffer ipStr = new StringBuffer();

			for(int i = 0; i < components.length; i++) {
				ipStr.append(components[i] + (i == components.length - 1 ? "" : "."));
			}

			return ipStr.toString();
		}

		private int encodeIp(String ip) throws Exception {
			String[] cs = ip.split("\\.");

			if(cs.length != 4)
				throw new Exception("IP must consist of 4 compoents seperated by periods.");

			int encoded = 0;


			for(String c : cs) {
				try {
					int ci = Integer.parseInt(c);

					if(ci > 15 || ci < 0)
						throw new Exception(c + " is not a valid IP component. Must be between 0 and 15 inclusive.");

					encoded <<= 4;
					encoded |= ci;

				} catch (NumberFormatException ex) {
					throw new Exception(c + " is not a valid integer component.");
				}
			}

			return encoded;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final TextArea ipAddress = getControl(TextArea.class, "ipAddress");
			final Label errTxt = getControl(Label.class, "errTxt");
			final Button btnSave = getControl(Button.class, "save");

			btnSave.getObservers().add(new Button.IButtonPressObserver() {
				@Override
				public void onPress() {
					int ip = -1;
					try {
						ip = encodeIp(ipAddress.getText());
					} catch (Exception e) {
						errTxt.setText("Invalid IP!");
					}

					if (ip >= 0) {
						m_observers.raise(IConfigureNetworkDisplayObserver.class).assignIp(ip);
					}
				}
			});

			String ip = decodeIp(m_defaultIp);
			ipAddress.setText(ip);
		}
	}

	public interface IConfigureNetworkDisplayObserver {
		void assignIp(int ip);
	}
}
