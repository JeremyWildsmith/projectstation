/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.pathfinding.RoomRestrictedDevicePathFinder;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.pathfinding.IncompleteRouteException;
import io.github.jevaengine.world.pathfinding.Route;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class AreaPowerController extends BasicDevice implements IPowerDevice {

	private static final int SEARCH_RADIUS = 1000;

	private final IRouteFactory m_routeFactory;

	private final ISceneModel m_model;

	private boolean m_scannedForPowerDevices = false;

	private final List<IPowerDevice> m_powerDevices = new ArrayList<>();

	public AreaPowerController(String name, ISceneModel model, IRouteFactory routeFactory) {
		super(name, false);
		m_model = model;
		m_routeFactory = routeFactory;
	}

	@Override
	protected void connectionChanged() {
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	private boolean canReachDevice(IPowerDevice d) {
		Vector2F start = this.getBody().getLocation().getXy().add(new Vector2F(0, 1));
		try {
			Route route = m_routeFactory.create(new RoomRestrictedDevicePathFinder(), this.getWorld(), start, d.getBody().getLocation().getXy(), 0.2F);
		} catch (IncompleteRouteException ex) {
			return false;
		}
		
		return true;
	}

	public void scanForPowerDevices() {
		m_powerDevices.clear();

		RadialSearchFilter<IPowerDevice> searchFilter = new RadialSearchFilter<>(this.getBody().getLocation().getXy(), SEARCH_RADIUS);
		IPowerDevice[] powerDevices = this.getWorld().getEntities().search(IPowerDevice.class, searchFilter);

		for (IPowerDevice d : powerDevices) {
			if(d == this)
				continue;
			
			if(canReachDevice(d))
				m_powerDevices.add(d);
		}

	}

	@Override
	public void update(int delta) {
		if (!m_scannedForPowerDevices) {
			scanForPowerDevices();
			m_scannedForPowerDevices = true;
		}
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof IPowerDevice);
	}
}
