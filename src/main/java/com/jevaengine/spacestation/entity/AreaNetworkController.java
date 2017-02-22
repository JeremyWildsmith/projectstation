/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.dcpu.devices.NetworkIoTerminal;
import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.pathfinding.RoomRestrictedDevicePathFinder;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.pathfinding.IncompleteRouteException;
import io.github.jevaengine.world.pathfinding.Route;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.AnimationSceneModelAnimationState;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.IAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
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
public class AreaNetworkController extends WiredDevice implements INetworkNode, INetworkDataCarrier, IDcpuCompatibleDevice {

	private static final String NODE_NAME = "apc";

	private static final String NODE_SET_SEPERATOR = ";";
	private static final String NODE_SEPERATOR = ",";

	private static final int MAX_WIRED_CONNECTIONS = 1;

	private static final int SEARCH_RADIUS = 200;

	private final IRouteFactory m_routeFactory;

	private final IAnimationSceneModel m_model;

	private boolean m_scanForNetworkDevices = true;
	private boolean m_scanForIpConflict = true;

	private final List<INetworkNode> m_managedDevices = new ArrayList<>();

	private final String m_netlist;

	private int m_ipAddress = 0;
	
	private final NetworkIoTerminal m_networkIoTerminal;

	public AreaNetworkController(String name, IAnimationSceneModel model, IRouteFactory routeFactory, String netlist, int ipAddress) {
		super(name, false);
		m_model = model;
		m_routeFactory = routeFactory;
		m_netlist = netlist;
		m_ipAddress = ipAddress;
		
		m_networkIoTerminal = new NetworkIoTerminal(this);
	}

	@Override
	protected void connectionChanged() {
		m_scanForIpConflict = true;
	}

	@Nullable
	private INetworkDataCarrier getNetworkDataCarrier() {
		List<INetworkDataCarrier> carrier = getConnections(INetworkDataCarrier.class);

		if (carrier.isEmpty()) {
			return null;
		}

		return carrier.get(0);
	}

	@Override
	public List<AreaNetworkController> getAreaNetworkControllers(List<INetworkDataCarrier> requested) {
		if (requested.contains(this)) {
			return new ArrayList<>();
		}

		requested.add(this);

		List<AreaNetworkController> controllers = new ArrayList<>();
		controllers.add(this);

		return controllers;
	}

	private List<AreaNetworkController> getAreaNetworkControllers() {
		List<INetworkDataCarrier> requested = new ArrayList<>();
		requested.add(this);

		INetworkDataCarrier carrier = getNetworkDataCarrier();

		if (carrier != null) {
			return carrier.getAreaNetworkControllers(requested);
		}

		return new ArrayList<>();
	}

	private boolean isAnIpConflict() {
		for (AreaNetworkController a : getAreaNetworkControllers()) {
			if (a == this) {
				continue;
			}

			if (a.getIp() == this.getIp()) {
				return true;
			}
		}

		return false;
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
	private INetworkNode getManagedDevice(String nodeName) {
		for (INetworkNode d : m_managedDevices) {
			if (d.getNodeName().compareTo(nodeName) == 0) {
				return d;
			}
		}

		return null;
	}

	private void connectManagedDevices() {
		for (Map.Entry<String, Set<String>> connectionSet : getNodeConnectionSets().entrySet()) {
			INetworkNode master = getManagedDevice(connectionSet.getKey());

			if (master == null) {
				continue;
			}

			for (String connection : connectionSet.getValue()) {
				INetworkNode slave = getManagedDevice(connection);

				if (slave != null) {
					master.addConnection(slave);
				}
			}
		}
	}

	private void clearManagedConnections() {
		for (INetworkNode d : m_managedDevices) {
			d.clearConnections();
		}

		m_managedDevices.clear();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	public int getIp() {
		return m_ipAddress;
	}

	public boolean hasIp() {
		return m_ipAddress == 0;
	}

	public void reset() {
		m_scanForNetworkDevices = true;
		m_scanForIpConflict = true;
		clearManagedConnections();
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	private boolean canReachDevice(INetworkNode d) {
		Vector2F start = this.getBody().getLocation().getXy().add(new Vector2F(0, 1));
		try {
			Route route = m_routeFactory.create(new RoomRestrictedDevicePathFinder(), this.getWorld(), start, d.getBody().getLocation().getXy(), 0.2F);
		} catch (IncompleteRouteException ex) {
			return false;
		}

		return true;
	}

	private void clearUnwiredConnections() {
		for (IDevice d : getConnections()) {
			if (d instanceof WiredDevice) {
				continue;
			}

			removeConnection(d);
		}
	}

	private void scanForNetworkDevices() {
		clearUnwiredConnections();
		clearManagedConnections();

		RadialSearchFilter<INetworkNode> searchFilter = new RadialSearchFilter<>(this.getBody().getLocation().getXy(), SEARCH_RADIUS);
		INetworkNode[] networkDevices = this.getWorld().getEntities().search(INetworkNode.class, searchFilter);

		for (INetworkNode d : networkDevices) {
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

		if (m_scanForIpConflict) {
			if (isAnIpConflict()) {
				m_ipAddress = 0;
			}

			m_scanForIpConflict = false;
		}

		IAnimationSceneModelAnimation error = m_model.getAnimation("error");
		IAnimationSceneModelAnimation on = m_model.getAnimation("on");

		if (hasIp()) {
			error.setState(AnimationSceneModelAnimationState.Play);
		} else {
			on.setState(AnimationSceneModelAnimationState.Play);
		}
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if (d instanceof WiredDevice) {
			if (getConnections(WiredDevice.class).size() < MAX_WIRED_CONNECTIONS) {
				return (d instanceof INetworkDataCarrier);
			}
		}

		return false;
	}

	@Override
	public String getNodeName() {
		return NODE_NAME;
	}

	@Override
	public void carry(List<INetworkDataCarrier> carried, NetworkPacket packet) {
		if (carried.contains(this)) {
			return;
		}

		carried.add(this);

		//recieve message here...
		if (!hasIp() || packet.RecieverAddress != getIp()) {
			return;
		}
	}

	public void transmitMessage(NetworkPacket packet) {
		if (!hasIp()) {
			return;
		}

		INetworkDataCarrier carrier = getNetworkDataCarrier();

		if (!hasIp() || carrier == null) {
			return;
		}

		List<INetworkDataCarrier> carriers = new ArrayList<>();
		carriers.add(this);

		carrier.carry(carriers, packet);

		packet.SenderAddress = getIp();
	}

	@Override
	public IDcpuHardware[] getHardware() {
		return new IDcpuHardware[] {m_networkIoTerminal};
	}
}
