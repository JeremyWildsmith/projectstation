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

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.rpg.item.usr.UsrWieldTarget;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class HudFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/hud/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public HudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public Hud create(IRpgCharacter owner, IItemStore invetory, ILoadout loadout) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(HUD_WINDOW, new HudBehaviourInjector(observers, loadout, invetory, owner));
		m_windowManager.addWindow(window);

		window.center();

		return new Hud(window, observers);
	}

	public static class Hud implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		public Hud(Window window, Observers observers) {
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

	private class HudBehaviourInjector extends WindowBehaviourInjector {

		private final ILoadout m_loadout;
		private final IItemStore m_inventory;
		private final IRpgCharacter m_owner;

		private final Observers m_observers;
		
		public HudBehaviourInjector(final Observers observers, final ILoadout loadout, final IItemStore inventory, final IRpgCharacter owner) {
			m_observers = observers;
			m_inventory = inventory;
			m_loadout = loadout;
			m_owner = owner;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final ToggleIcon toggleInventory = getControl(ToggleIcon.class, "toggleInventory");

			final SimpleItemContainer hand = getControl(SimpleItemContainer.class, "toggleHand");

			IItemSlot handSlot = m_loadout.getSlot(UsrWieldTarget.LeftHand);
			hand.setSlot(handSlot);
			hand.getObservers().add(new HandUse(handSlot));
			toggleInventory.getObservers().add(new ToggleIcon.IToggleIconObserver() {
				@Override
				public void toggled() {
					m_observers.raise(IHudObserver.class).inventoryViewChanged(toggleInventory.isActive());
				}
			});
		}


		private class HandUse implements SimpleItemContainer.ISimpleItemContainerObserver {
			private final IItemSlot m_slot;

			public HandUse(IItemSlot slot) {
				m_slot = slot;
			}

			private void tryUseItem() {
				IItem item = m_slot.getItem();
				IItem.ItemUseAbilityTestResults result = item.getFunction().testUseAbility(m_owner, new IItem.ItemTarget(m_owner), item.getAttributes());

				if(result.isUseable()) {
					item.getFunction().use(m_owner, new IItem.ItemTarget(m_owner), item.getAttributes(), item);
				}
			}


			@Override
			public void selected() {
				if(m_slot.isEmpty())
					return;

				tryUseItem();
			}

			@Override
			public void alternateSelected() {
				if(m_slot.isEmpty() || m_owner.getWorld() == null || m_inventory.isFull())
					return;

				IItem item = m_slot.clear();

				m_inventory.addItem(item);
			}
		}
	}



	public interface IHudObserver {
		void movementSpeedChanged(boolean isRunning);
		void inventoryViewChanged(boolean isVisible);
	}
}
