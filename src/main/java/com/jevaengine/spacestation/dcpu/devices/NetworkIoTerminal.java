/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.dcpu.devices;

import com.jevaengine.spacestation.entity.AreaNetworkController;
import de.codesourcery.jasm16.Register;
import de.codesourcery.jasm16.WordAddress;
import de.codesourcery.jasm16.emulator.ICPU;
import de.codesourcery.jasm16.emulator.IEmulator;
import de.codesourcery.jasm16.emulator.devices.DeviceDescriptor;
import de.codesourcery.jasm16.emulator.devices.HardwareInterrupt;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import de.codesourcery.jasm16.emulator.memory.IMemory;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Jeremy
 */
public class NetworkIoTerminal implements IDcpuHardware {

	private static final DeviceDescriptor DESC = new DeviceDescriptor("NetworkIoTerminal", "Can transmit network packets over a netowrk", 
																		0xAAA00000, 1, 0x59ACE);
	
	private static final int MAX_BACKLOG = 10;
	
	private final AreaNetworkController m_networkController;

	private IEmulator m_attachedEmulator;

	private final Queue<NetworkPacket> m_recieveQueue = new LinkedList<>();
	private int m_interruptMessage = 0;

	public NetworkIoTerminal(AreaNetworkController networkController) {
		m_networkController = networkController;
	}

	@Override
	public void afterAddDevice(IEmulator emulator) {
		m_attachedEmulator = emulator;
		reset();
	}

	@Override
	public void reset() {
		m_recieveQueue.clear();
		m_interruptMessage = 0;
	}

	@Override
	public boolean supportsMultipleInstances() {
		return true;
	}

	@Override
	public void beforeRemoveDevice(IEmulator emulator) {
		reset();
	}

	@Override
	public DeviceDescriptor getDeviceDescriptor() {
		return DESC;
	}
	
	public void recievedMessage(NetworkPacket packet) {
		if(m_attachedEmulator == null || m_recieveQueue.size() >= MAX_BACKLOG)
			return;
		
		m_recieveQueue.add(packet);
		
		if(m_interruptMessage != 0)
			m_attachedEmulator.triggerInterrupt(new HardwareInterrupt(this, m_interruptMessage));
	}

	private void transmitMessage(int memoryAddress, IMemory memory) {
		NetworkPacket packet = new NetworkPacket();

		packet.SenderAddress = memory.read(new WordAddress(memoryAddress)) << 16;
		packet.SenderAddress |= memory.read(new WordAddress(memoryAddress + 1));

		packet.RecieverAddress = memory.read(new WordAddress(memoryAddress + 2)) << 16;
		packet.RecieverAddress |= memory.read(new WordAddress(memoryAddress + 3));

		packet.Port = memory.read(new WordAddress(memoryAddress + 4));

		for (int i = 0; i < packet.data.length; i++) {
			packet.data[i] = memory.read(new WordAddress(memoryAddress + 5 + i));
		}
		
		m_networkController.transmitMessage(packet);
	}

	private void writePacketToMemory(NetworkPacket packet, int memoryAddress, IMemory memory) {
		memory.write(new WordAddress(memoryAddress), packet.SenderAddress & 0xFFFF0000);
		memory.write(new WordAddress(memoryAddress + 1), packet.SenderAddress & 0x0000FFFF);

		memory.write(new WordAddress(memoryAddress + 2), packet.RecieverAddress & 0xFFFF0000);
		memory.write(new WordAddress(memoryAddress + 3), packet.RecieverAddress & 0x0000FFFF);

		memory.write(new WordAddress(memoryAddress + 4), packet.Port & 0xFFFF);

		for (int i = 0; i < packet.data.length; i++) {
			memory.write(new WordAddress(memoryAddress + 5 + i), packet.data[i]);
		}
	}

	@Override
	public int handleInterrupt(IEmulator emulator, ICPU cpu, IMemory memory) {
		switch (cpu.getRegisterValue(Register.A)) {
			case 0:
				NetworkPacket packet = m_recieveQueue.poll();

				if (packet == null) {
					cpu.setRegisterValue(Register.X, 0);
				} else {
					writePacketToMemory(packet, cpu.getRegisterValue(Register.X), memory);
				}
				break;
			case 1:
				transmitMessage(cpu.getRegisterValue(Register.X), memory);
				break;
			case 2:
				m_interruptMessage = cpu.getRegisterValue(Register.X);
			case 3:
				m_recieveQueue.clear();
				break;
		}
		
		return 0;
	}
}
