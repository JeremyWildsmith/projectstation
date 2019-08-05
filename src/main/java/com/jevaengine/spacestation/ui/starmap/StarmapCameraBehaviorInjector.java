/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.starmap;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.ui.ToggleIcon;
import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import io.github.jevaengine.world.scene.camera.ICamera;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jeremy
 */
public class StarmapCameraBehaviorInjector extends WindowBehaviourInjector {
	private final FollowCamera m_camera;

	public StarmapCameraBehaviorInjector(FollowCamera camera) {
		m_camera = camera;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");

		demoWorldView.setCamera(m_camera);


		getControl(ToggleIcon.class, "zoomIn").getObservers().add(new ToggleIcon.IToggleIconObserver() {
			@Override
			public void toggled() {
				m_camera.setZoom(Math.min(2, m_camera.getZoom() + 0.1f));
			}
		});
		getControl(ToggleIcon.class, "zoomOut").getObservers().add(new ToggleIcon.IToggleIconObserver() {
			@Override
			public void toggled() {
				m_camera.setZoom(Math.max(0.1f, m_camera.getZoom() - 0.1f));
			}
		});
	}
}
