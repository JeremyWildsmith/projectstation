/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import com.jevaengine.spacestation.entity.network.NetworkDevice;
import com.jevaengine.spacestation.entity.power.Dcpu;
import com.jevaengine.spacestation.ui.ConfigureNetworkDisplayFactory;
import com.jevaengine.spacestation.ui.LemDisplayFactory;
import com.jevaengine.spacestation.ui.LemDisplayFactory.LemDisplay;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector.IInteractionHandler;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.world.entity.IEntity;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jeremy
 */
public class NetworkDeviceInteractionHandler implements IInteractionHandler {

	private final ConfigureNetworkDisplayFactory m_displayFactory;
	private IEntity m_activeInteraction;
	private ConfigureNetworkDisplayFactory.ConfigureNetworkDisplay m_activeDisplay;

	private void startInteraction(IEntity activeInteraction, ConfigureNetworkDisplayFactory.ConfigureNetworkDisplay activeDisplay) {
		cancelInteraction();
		m_activeInteraction = activeInteraction;
		m_activeDisplay = activeDisplay;
	}

	private void cancelInteraction() {
		if(m_activeDisplay != null)
			m_activeDisplay.dispose();

		m_activeInteraction = null;
		m_activeDisplay = null;
	}

	public NetworkDeviceInteractionHandler(ConfigureNetworkDisplayFactory displayFactory) {
		m_displayFactory = displayFactory;
	}
	
	@Override
	public Class<?> getHandleSubject() {
		return NetworkDevice.class;
	}

	@Override
	public void handle(IEntity subject, boolean isSecondary, float interactionReach) {
		try {
			NetworkDevice entity = (NetworkDevice)subject;

			ConfigureNetworkDisplayFactory.ConfigureNetworkDisplay display = m_displayFactory.create(entity.getIp());
			display.setTopMost(true);
			display.getObservers().add(new ConfigureNetworkDisplayFactory.IConfigureNetworkDisplayObserver() {
				@Override
				public void assignIp(int ip) {
					entity.setIp(ip);
				}
			});

			startInteraction(entity, display);

		} catch (IWindowFactory.WindowConstructionException ex) {
			Logger.getLogger(NetworkDeviceInteractionHandler.class.getName()).log(Level.SEVERE, "Unable to create net config display interface handler.", ex);
		}
	}

	@Override
	public IEntity getActiveInteraction() {
		return m_activeInteraction;
	}

	@Override
	public void outOfReach() {
		cancelInteraction();
	}

	@Override
	public String getInteractionName() {
		return "Configure";
	}
}
