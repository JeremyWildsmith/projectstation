package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkDevice extends WiredDevice implements INetworkDataCarrier {
    private int m_ipAddress = 0;

    public NetworkDevice(String name, boolean isTraversable, int ipAddress) {
        super(name, isTraversable);
        m_ipAddress = ipAddress;
    }

    public final int getIp() {
        return m_ipAddress;
    }

    public void setIp(int ipAddress) {
        m_ipAddress = ipAddress;
    }

    @Override
    protected boolean canConnectTo(IDevice d) {
        if(!super.canConnectTo(d))
            return false;

        return d instanceof INetworkDataCarrier;
    }

    protected abstract void processPacket(NetworkPacket p);

    @Override
    public final void carry(List<INetworkDataCarrier> carried, NetworkPacket packet) {
        if (carried.contains(this)) {
            return;
        }

        carried.add(this);

        if (packet.RecieverAddress != getIp()) {
            return;
        }

        processPacket(packet);
    }

    protected void transmitMessage(NetworkPacket packet) {
        packet.SenderAddress = getIp();
        for(INetworkDataCarrier c : getConnections(INetworkDataCarrier.class)) {
            List<INetworkDataCarrier> carriers = new ArrayList<>();
            carriers.add(this);

            c.carry(carriers, packet);
        }
    }


}
