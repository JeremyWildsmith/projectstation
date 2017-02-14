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

import com.jevaengine.spacestation.ui.SimpleItemContainer.ISimpleItemContainerObserver;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.item.IImmutableItemSlot;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.util.Observers;
import java.net.URI;

public final class InventoryHudFactory {

	private final URI m_layout;

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public InventoryHudFactory(WindowManager windowManager, IWindowFactory windowFactory, URI layout) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_layout = layout;
	}

	public InventoryHud create(ILoadout loadout, IItemStore inventory) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(m_layout, new InventoryHudFactoryBehaviourInjector(observers, loadout, inventory));
		m_windowManager.addWindow(window);

		window.center();

		return new InventoryHud(window, observers);
	}

	public static class InventoryHud implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private InventoryHud(Window window, Observers observers) {
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

	private class InventoryHudFactoryBehaviourInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final ILoadout m_loadout;
		private final IItemStore m_inventory;
		
		public InventoryHudFactoryBehaviourInjector(final Observers observers, ILoadout loadout, IItemStore inventory) {
			m_observers = observers;
			m_loadout = loadout;
			m_inventory = inventory;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			IItemSlot slots[] = m_inventory.getSlots();
			
			for(int i = 0; i < slots.length && hasControl(SimpleItemContainer.class, String.valueOf(i)); i++) {
				SimpleItemContainer container = getControl(SimpleItemContainer.class, String.valueOf(i));
				container.setSlot(slots[i]);
				container.getObservers().add(new MoveItemToLoadout(slots[i]));
			}
		}
		
		private class MoveItemToLoadout implements ISimpleItemContainerObserver {
			private final IItemSlot m_slot;
			
			public MoveItemToLoadout(IItemSlot slot) {
				m_slot = slot;
			}
			
			@Override
			public void selected() {
				if(m_slot.isEmpty())
					return;
				
				for(IWieldTarget t : m_slot.getItem().getFunction().getWieldTargets()) {
					IItemSlot loadoutSlot = m_loadout.getSlot(t);
					
					if(loadoutSlot != null) {
						IItem removed = loadoutSlot.setItem(m_slot.getItem());
						
						if(removed == null)
							m_slot.clear();
						else
							m_slot.setItem(removed);
						
						return;
					}
				}
			}
		}
	}
}
