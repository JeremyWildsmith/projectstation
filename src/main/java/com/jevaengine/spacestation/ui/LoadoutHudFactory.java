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
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.rpg.item.usr.UsrWieldTarget;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class LoadoutHudFactory {

	private static final URI LOADOUT_WINDOW = URI.create("file:///ui/windows/loadout/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public LoadoutHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public LoadoutHud create(ILoadout loadout, IItemStore inventory) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(LOADOUT_WINDOW, new LoadoutFactoryBehaviourInjector(observers, loadout, inventory));
		m_windowManager.addWindow(window);

		window.center();

		return new LoadoutHud(window, observers);
	}

	public static class LoadoutHud implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		public LoadoutHud(Window window, Observers observers) {
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

	private class LoadoutFactoryBehaviourInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final ILoadout m_loadout;
		private final IItemStore m_inventory;
		
		public LoadoutFactoryBehaviourInjector(final Observers observers, final ILoadout loadout, IItemStore inventory) {
			m_observers = observers;
			m_loadout = loadout;
			m_inventory = inventory;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final SimpleItemContainer uniform = getControl(SimpleItemContainer.class, "uniform");
			final SimpleItemContainer shoes = getControl(SimpleItemContainer.class, "shoes");
			final SimpleItemContainer gloves = getControl(SimpleItemContainer.class, "gloves");
			final SimpleItemContainer glasses = getControl(SimpleItemContainer.class, "glasses");
			final SimpleItemContainer ears = getControl(SimpleItemContainer.class, "ears");
			final SimpleItemContainer head = getControl(SimpleItemContainer.class, "head");
			
			uniform.setSlot(m_loadout.getSlot(UsrWieldTarget.Uniform));
			shoes.setSlot(m_loadout.getSlot(UsrWieldTarget.Feet));
			gloves.setSlot(m_loadout.getSlot(UsrWieldTarget.Hands));
			glasses.setSlot(m_loadout.getSlot(UsrWieldTarget.Eyes));
			ears.setSlot(m_loadout.getSlot(UsrWieldTarget.Ears));
			head.setSlot(m_loadout.getSlot(UsrWieldTarget.Mask));
		
			uniform.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Uniform));
			shoes.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Feet));
			gloves.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Hands));
			glasses.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Eyes));
			ears.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Ears));
			head.getObservers().add(new MoveToInventoryObserver(UsrWieldTarget.Mask));
		}
		
		private class MoveToInventoryObserver implements ISimpleItemContainerObserver {
			private final IWieldTarget m_wieldTarget;
			
			public MoveToInventoryObserver(IWieldTarget wieldTarget) {
				m_wieldTarget = wieldTarget;
			}
			
			@Override
			public void selected() {
				IImmutableItemSlot slot = m_loadout.getSlot(m_wieldTarget);
				if(slot == null || slot.isEmpty())
					return;
				
				if(m_inventory.addItem(slot.getItem())) {
					m_loadout.unequip(m_wieldTarget);
				}
			}

			@Override
			public void alternateSelected() { }
		}
	}
	
	public interface ILoadoutHudObserver {
		void slotSelected(IWieldTarget target);
	}
}
