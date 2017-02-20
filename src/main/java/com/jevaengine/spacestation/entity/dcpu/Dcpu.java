/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.dcpu;

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
public final class Dcpu extends BasicDevice implements INetworkDevice, IInteractableEntity {

	private static final int CYCLES_PER_MS = 100;

	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger(0);

	private final IAnimationSceneModel m_model;

	private final Map<IDcpuCompatibleDevice, List<IDcpuHardware>> m_hardwareConnections = new HashMap<>();

	private final Emulator m_dcpu = new Emulator();

	private int timeSinceStep = 0;

	private boolean m_isOn = false;
	private boolean m_hasCrashed = false;

	public Dcpu(IAnimationSceneModel model, byte[] firmware, boolean isOn) {
		this(Dcpu.class.getClass().getName() + m_unnamedEntityCount.getAndIncrement(), model, firmware, isOn);
	}
	
	public Dcpu(String name, IAnimationSceneModel model, byte[] firmware, boolean isOn) {
		super(name, false);
		m_model = model;
		m_dcpu.loadMemory(Address.ZERO, firmware);
		m_dcpu.addDevice(new DefaultClock());
		
		if (isOn) {
			turnOn();
		} else {
			turnOff();
		}
	}


	public void reset() {
		turnOff();
		turnOn();
	}

	public void turnOn() {
		if (m_isOn) {
			return;
		}

		m_isOn = true;
		m_model.getAnimation("on").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);

		m_dcpu.reset(false);
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
		return (d instanceof INetworkDevice);
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
		
		for (IDcpuCompatibleDevice d : devices)
		{
			for (IDcpuHardware h : m_hardwareConnections.get(d)) {
				m_dcpu.removeDevice(h);
			}
		}

		m_hardwareConnections.clear();
	}

	private <T extends INetworkDevice> List<T> getConnectedDevices(Class<T> clazz, List<INetworkDevice> requested) {
		List<T> devices = new ArrayList<>();
		for (INetworkDevice d : getConnections(INetworkDevice.class)) {
			devices.addAll(d.getConnected(requested, clazz));
		}

		return devices;
	}

	@Override
	public void update(int delta) {
		List<IDcpuCompatibleDevice> connectedDevices = getConnectedDevices(IDcpuCompatibleDevice.class, new ArrayList<INetworkDevice>());

		List<IDcpuCompatibleDevice> currentConnected = new ArrayList<>(m_hardwareConnections.keySet());
		for (IDcpuCompatibleDevice d : currentConnected) {
			if (!connectedDevices.contains(d)) {
				removeOldDcpuConnection(d);
			}
		}

		if (!m_isOn) {
			return;
		}

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
}
