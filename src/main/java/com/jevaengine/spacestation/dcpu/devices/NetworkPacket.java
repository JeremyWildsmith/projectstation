/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.dcpu.devices;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public final class NetworkPacket {
	public int SenderAddress;
	public int RecieverAddress;
	public int Port;
	public final int data[] = new int[45];

	public byte[] getBytes() {
		List<Byte> bytes = new ArrayList<>();
		bytes.add((byte)(SenderAddress & 0xFF));
		bytes.add((byte)(SenderAddress & 0xFF00));

		bytes.add((byte)(RecieverAddress & 0xFF));
		bytes.add((byte)(RecieverAddress & 0xFF00));

		bytes.add((byte)(Port & 0xFF));
		bytes.add((byte)(Port & 0xFF00));

		for(int d : data) {
			bytes.add((byte)(d & 0xFF));
			bytes.add((byte)(d & 0xFF00));
		}

		byte b[] = new byte[bytes.size()];
		for(int i = 0; i < b.length; i++)
			b[i] = bytes.get(i);

		return b;
	}
}
