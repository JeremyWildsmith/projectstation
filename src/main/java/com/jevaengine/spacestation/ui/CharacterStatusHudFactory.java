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

import com.jevaengine.spacestation.entity.character.SpaceCharacterAttribute;
import com.jevaengine.spacestation.item.SpaceCharacterWieldTarget;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class CharacterStatusHudFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/statusHud/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public CharacterStatusHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public StatusHud create(IRpgCharacter owner) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(HUD_WINDOW, new StatusHudBehaviourInjector(observers, owner));
		m_windowManager.addWindow(window);

		window.center();

		return new StatusHud(window, observers);
	}

	public static class StatusHud implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		public StatusHud(Window window, Observers observers) {
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

	private class StatusHudBehaviourInjector extends WindowBehaviourInjector {

		private final IRpgCharacter m_owner;

		private final Observers m_observers;
		
		public StatusHudBehaviourInjector(final Observers observers, final IRpgCharacter owner) {
			m_observers = observers;
			m_owner = owner;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final ValueGuage guage = getControl(ValueGuage.class, "health");
			final Timer timer = new Timer();
			addControl(timer);
			timer.getObservers().add(new Timer.ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					IImmutableAttributeSet set = m_owner.getAttributes();
					float ratio = 0;

					if(set.has(SpaceCharacterAttribute.MaxHitpoints))
						ratio = set.get(SpaceCharacterAttribute.EffectiveHitpoints).get() / set.get(SpaceCharacterAttribute.MaxHitpoints).get();

					guage.setValue(ratio);
				}
			});
		}

	}
}
