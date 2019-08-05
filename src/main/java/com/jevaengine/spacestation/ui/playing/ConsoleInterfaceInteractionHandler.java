/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import com.jevaengine.spacestation.entity.power.Dcpu;
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
public class ConsoleInterfaceInteractionHandler implements IInteractionHandler {

	private final LemDisplayFactory m_lemDisplayFactory;
	private IEntity m_activeInteraction;
	private LemDisplay m_activeDisplay;
	
	private void startInteraction(IEntity activeInteraction, LemDisplay activeDisplay) {
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
	
	public ConsoleInterfaceInteractionHandler(LemDisplayFactory lemDisplayFactory) {
		m_lemDisplayFactory = lemDisplayFactory;
	}
	
	@Override
	public Class<?> getHandleSubject() {
		return Dcpu.class;
	}

	@Override
	public void handle(IEntity subject, boolean isSecondary, float interactionReach) {
		try {
			Dcpu entity = (Dcpu)subject;
			
			LemDisplay lem = m_lemDisplayFactory.create(entity);
			lem.setTopMost(true);
			startInteraction(entity, lem);

		} catch (IWindowFactory.WindowConstructionException ex) {
			Logger.getLogger(ConsoleInterfaceInteractionHandler.class.getName()).log(Level.SEVERE, "Unable to create lem display interface handler.", ex);
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
		return "Use Computer Interface";
	}
}
