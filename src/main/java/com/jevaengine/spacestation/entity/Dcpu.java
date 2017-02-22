/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.entity.*;
import de.codesourcery.jasm16.Address;
import de.codesourcery.jasm16.emulator.Emulator;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import de.codesourcery.jasm16.emulator.devices.impl.DefaultClock;
import de.codesourcery.jasm16.emulator.exceptions.EmulationErrorException;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Jeremy
 */
public final class Dcpu extends BasicDevice implements INetworkDevice, IPowerDevice, IInteractableEntity {

	private static final int CYCLES_PER_MS = 100;
	private static final int POWER_USEAGE_WATTS = 250;
	private static final int ON_POWER_USAGE_SECONDS = 1000; //1 seconds worth of power required to boot DCPU.

	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger(0);

	private final IAnimationSceneModel m_model;

	private final Map<IDcpuCompatibleDevice, List<IDcpuHardware>> m_hardwareConnections = new HashMap<>();

	private final Emulator m_dcpu = new Emulator();
	private final DefaultClock m_clock = new DefaultClock();

	private boolean m_isOn = false;
	private boolean m_hasCrashed = false;

	private final byte[] m_firmware;
	
	private final String m_nodeName;

	public Dcpu(IAnimationSceneModel model, byte[] firmware, boolean isOn, String nodeName) {
		this(Dcpu.class.getClass().getName() + m_unnamedEntityCount.getAndIncrement(), model, firmware, isOn, nodeName);
	}

	public Dcpu(String name, IAnimationSceneModel model, byte[] firmware, boolean isOn, String nodeName) {
		super(name, false);
		m_model = model;
		m_firmware = firmware;
		m_dcpu.addDevice(m_clock);
		m_nodeName = nodeName;

		if (isOn) {
			turnOn();
		} else {
			turnOff();
		}
	}

	private AreaPowerController getAreaPowerController() {
		List<AreaPowerController> controller = getConnections(AreaPowerController.class);

		return controller.isEmpty() ? null : controller.get(0);
	}

	private boolean drawEnergy(int timeDelta) {
		AreaPowerController c = getAreaPowerController();

		List<IDevice> requested = new ArrayList<>();
		requested.add(this);

		if (c == null) {
			return false;
		}

		int requiredEnergy = (int) Math.ceil((((float) timeDelta) / 1000) * POWER_USEAGE_WATTS);

		return c.drawEnergy(requested, requiredEnergy) >= requiredEnergy;
	}

	public void reset() {
		turnOff();
		turnOn();
		m_clock.reset();
	}

	public void turnOn() {
		if (m_isOn || !drawEnergy(ON_POWER_USAGE_SECONDS)) {
			return;
		}

		m_isOn = true;
		m_model.getAnimation("on").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);

		m_dcpu.reset(true);
		m_dcpu.loadMemory(Address.ZERO, m_firmware);
	}

	public void turnOff() {

		m_hasCrashed = false;
		m_isOn = false;
		m_model.getAnimation("off").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);

		clearDcpuConnections();
	}

	public void crash() {
		m_hasCrashed = true;
		m_model.getAnimation("crash").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
	}

	@Override
	protected void connectionChanged() {
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return true;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	private void addNewDcpuConnection(IDcpuCompatibleDevice d) {
		if (m_hardwareConnections.containsKey(d)) {
			return;
		}

		List<IDcpuHardware> hardware = new ArrayList<>();

		for (IDcpuHardware h : d.getHardware()) {
			hardware.add(h);
			m_dcpu.addDevice(h);
		}

		m_hardwareConnections.put(d, hardware);
	}

	private void removeOldDcpuConnection(IDcpuCompatibleDevice d) {
		if (!m_hardwareConnections.containsKey(d)) {
			return;
		}

		for (IDcpuHardware h : m_hardwareConnections.get(d)) {
			m_dcpu.removeDevice(h);
		}

		m_hardwareConnections.remove(d);
	}

	private void clearDcpuConnections() {
		ArrayList<IDcpuCompatibleDevice> devices = new ArrayList<>(m_hardwareConnections.keySet());

		for (IDcpuCompatibleDevice d : devices) {
			for (IDcpuHardware h : m_hardwareConnections.get(d)) {
				m_dcpu.removeDevice(h);
			}
		}

		m_hardwareConnections.clear();
	}

	private List<IDcpuCompatibleDevice> getConnectedDevices() {
		List<IDcpuCompatibleDevice> devices = new ArrayList<>();
		
		for (IDcpuCompatibleDevice d : getConnections(IDcpuCompatibleDevice.class)) {
			devices.add(d);
		}

		return devices;
	}

	@Override
	public void update(int delta) {
		List<IDcpuCompatibleDevice> connectedDevices = getConnectedDevices();

		List<IDcpuCompatibleDevice> currentConnected = new ArrayList<>(m_hardwareConnections.keySet());
		for (IDcpuCompatibleDevice d : currentConnected) {
			if (!connectedDevices.contains(d)) {
				removeOldDcpuConnection(d);
			}
		}

		if (!m_isOn) {
			return;
		}

		if (!drawEnergy(delta)) {
			turnOff();
			return;
		}

		m_clock.update(delta);
		for (IDcpuCompatibleDevice d : connectedDevices) {
			if (!m_hardwareConnections.containsKey(d)) {
				addNewDcpuConnection(d);
			}
		}

		for (int i = delta * CYCLES_PER_MS; i > 0; i--) {
			if (!m_hasCrashed) {
				try {
					m_dcpu.executeOneInstruction();
				} catch (EmulationErrorException e) {
					crash();
				}
			}
		}

	}

	@Override
	public void interactedWith(IRpgCharacter subject) {
		if (m_isOn && !m_hasCrashed) {
			turnOff();
		} else {
			reset();
		}
	}

	@Override
	public void interactWith(IRpgCharacter subject, String interaction) {
		interactedWith(subject);
	}

	@Override
	public String[] getInteractions() {
		return new String[0];
	}

	@Override
	public <T extends INetworkDevice> List<T> getConnected(List<INetworkDevice> requested, Class<T> device) {
		return new ArrayList<>();
	}

	@Override
	public boolean isConnected(List<INetworkDevice> requested, INetworkDevice device) {
		return false;
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		return 0;
	}

	@Override
	public String getNodeName() {
		return m_nodeName;
	}
}