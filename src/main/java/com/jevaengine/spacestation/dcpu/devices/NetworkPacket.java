/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.dcpu.devices;

/**
 *
 * @author Jeremy
 */
public final class NetworkPacket {
	public int SenderAddress;
	public int RecieverAddress;
	public int Port;
	public final int data[] = new int[45];
}
