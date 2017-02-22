/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.pathfinding.RoomRestrictedDevicePathFinder;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.pathfinding.IncompleteRouteException;
import io.github.jevaengine.world.pathfinding.Route;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class AreaPowerController extends WiredDevice implements IPowerDevice {

	private static final int MAX_WIRED_CONNECTIONS = 1;
	
	private static final int SEARCH_RADIUS = 200;

	private final IRouteFactory m_routeFactory;

	private final ISceneModel m_model;

	private boolean m_scanForPowerDevices = true;
	
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

	public void reset() {
		m_scanForPowerDevices = true;
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

	private void scanForPowerDevices() {
		clearConnections();

		RadialSearchFilter<IPowerDevice> searchFilter = new RadialSearchFilter<>(this.getBody().getLocation().getXy(), SEARCH_RADIUS);
		IPowerDevice[] powerDevices = this.getWorld().getEntities().search(IPowerDevice.class, searchFilter);

		for (IPowerDevice d : powerDevices) {
			if(d == this)
				continue;
			
			if(canReachDevice(d))
				addConnection(d);
		}

	}

	@Override
	public void update(int delta) {
		if (m_scanForPowerDevices) {
			scanForPowerDevices();
			m_scanForPowerDevices = false;
		}
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		requested.add(this);
		
		int drawn = 0;
		
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			drawn += d.drawEnergy(requested, joules);
			
			if(drawn >= joules)
				break;
		}
		
		return drawn;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(d instanceof WiredDevice) {
			if(getConnections(WiredDevice.class).size() >= MAX_WIRED_CONNECTIONS)
				return false;
		}
		
		return (d instanceof IPowerDevice);
	}
}
