/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.scene.camera.ICamera;

/**
 *
 * @author Jeremy
 */
public class CameraBehaviorInjector extends WindowBehaviourInjector {
	private final ICamera m_camera;

	public CameraBehaviorInjector(ICamera camera) {
		m_camera = camera;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");
		demoWorldView.setCamera(m_camera);
	}
}
