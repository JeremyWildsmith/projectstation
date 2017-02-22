/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.pathfinding.RoomRestrictedDevicePathFinder;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.pathfinding.IncompleteRouteException;
import io.github.jevaengine.world.pathfinding.Route;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jeremy
 */
public class AreaNetworkController extends WiredDevice implements INetworkDevice {

	private static final String NODE_SET_SEPERATOR = ";";
	private static final String NODE_SEPERATOR = ",";

	private static final int MAX_WIRED_CONNECTIONS = 1;

	private static final int SEARCH_RADIUS = 200;

	private final IRouteFactory m_routeFactory;

	private final ISceneModel m_model;

	private boolean m_scanForNetworkDevices = true;

	private String m_netlist;

	private List<INetworkDevice> m_managedDevices = new ArrayList<>();

	public AreaNetworkController(String name, ISceneModel model, IRouteFactory routeFactory, String netlist) {
		super(name, false);
		m_model = model;
		m_routeFactory = routeFactory;
		m_netlist = netlist;
	}

	@Override
	protected void connectionChanged() {
	}

	private Map<String, Set<String>> getNodeConnectionSets() {
		Map<String, Set<String>> nodeConnections = new HashMap<>();

		for (String set : m_netlist.split(NODE_SET_SEPERATOR)) {
			String nodes[] = set.split(NODE_SEPERATOR);

			if (nodes.length == 0) {
				continue;
			}

			Set<String> childrenConnections = new HashSet<>();

			for (int i = 1; i < nodes.length; i++) {
				if (!nodes[i].trim().isEmpty()) {
					childrenConnections.add(nodes[i].trim());
				}
			}

			String master = nodes[0].trim();

			if (nodeConnections.containsKey(master)) {
				nodeConnections.get(master).addAll(childrenConnections);
			} else {
				nodeConnections.put(master, childrenConnections);
			}
		}

		return nodeConnections;
	}

	@Nullable
	private INetworkDevice getManagedDevice(String nodeName) {
		for (INetworkDevice d : m_managedDevices) {
			if (d.getNodeName().compareTo(nodeName) == 0) {
				return d;
			}
		}

		return null;
	}

	private void connectManagedDevices() {
		for (Map.Entry<String, Set<String>> connectionSet : getNodeConnectionSets().entrySet()) {
			INetworkDevice master = getManagedDevice(connectionSet.getKey());
			
			if(master == null)
				continue;
			
			for (String connection : connectionSet.getValue()) {
				INetworkDevice slave = getManagedDevice(connection);
				
				if(slave != null)
					master.addConnection(slave);
			}
		}
	}

	private void clearManagedConnections() {
		for (INetworkDevice d : m_managedDevices) {
			d.clearConnections();
		}

		m_managedDevices.clear();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	public void reset() {
		m_scanForNetworkDevices = true;
		clearManagedConnections();
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	private boolean canReachDevice(INetworkDevice d) {
		Vector2F start = this.getBody().getLocation().getXy().add(new Vector2F(0, 1));
		try {
			Route route = m_routeFactory.create(new RoomRestrictedDevicePathFinder(), this.getWorld(), start, d.getBody().getLocation().getXy(), 0.2F);
		} catch (IncompleteRouteException ex) {
			return false;
		}

		return true;
	}

	private void scanForNetworkDevices() {
		clearConnections();

		RadialSearchFilter<INetworkDevice> searchFilter = new RadialSearchFilter<>(this.getBody().getLocation().getXy(), SEARCH_RADIUS);
		INetworkDevice[] networkDevices = this.getWorld().getEntities().search(INetworkDevice.class, searchFilter);

		for (INetworkDevice d : networkDevices) {
			if (d == this) {
				continue;
			}

			if (canReachDevice(d)) {
				m_managedDevices.add(d);
			}
		}

	}

	@Override
	public void update(int delta) {
		if (m_scanForNetworkDevices) {
			scanForNetworkDevices();
			connectManagedDevices();
			m_scanForNetworkDevices = false;
		}
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if (d instanceof WiredDevice) {
			if (getConnections(WiredDevice.class).size() >= MAX_WIRED_CONNECTIONS) {
				return (d instanceof INetworkDevice);
			}
		}

		return false;
	}

	@Override
	public <T extends INetworkDevice> List<T> getConnected(List<INetworkDevice> requested, Class<T> device) {
		ArrayList<T> devices = new ArrayList<>();

		if (requested.contains(this)) {
			return devices;
		}

		requested.add(this);

		List<INetworkDevice> connections = getConnections(INetworkDevice.class);

		for (INetworkDevice w : connections) {
			if (device.isAssignableFrom(w.getClass())) {
				devices.add((T) w);
			}

			devices.addAll(w.getConnected(requested, device));
		}

		return devices;
	}

	@Override
	public boolean isConnected(List<INetworkDevice> requested, INetworkDevice device) {
		if (requested.contains(this)) {
			return false;
		}

		requested.add(this);

		if (this == device) {
			return true;
		}

		List<INetworkDevice> connections = getConnections(INetworkDevice.class);

		for (INetworkDevice w : connections) {
			if (!requested.contains(w)) {
				if (w.isConnected(requested, device)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public String getNodeName() {
		return null;
	}
}
