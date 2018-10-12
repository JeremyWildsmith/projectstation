package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkDevice extends WiredDevice implements INetworkDataCarrier {
    private boolean m_scanForNetworkDevices = true;

    private int m_ipAddress = 0;

    public NetworkDevice(String name, int ipAddress) {
        super(name, false);
        m_ipAddress = ipAddress;
    }

    public final int getIp() {
        return m_ipAddress;
    }

    public final boolean hasIp() {
        return m_ipAddress != 0;
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

    protected abstract void recievePacket(NetworkPacket p);

    @Override
    public final void carry(List<INetworkDataCarrier> carried, NetworkPacket packet) {
        if (carried.contains(this)) {
            return;
        }

        carried.add(this);

        //recieve message here...
        if (!hasIp() || packet.RecieverAddress != getIp()) {
            return;
        }

        recievePacket(packet);
    }

    protected void transmitMessage(NetworkPacket packet) {
        if (!hasIp()) {
            return;
        }

        packet.SenderAddress = getIp();
        for(INetworkDataCarrier c : getConnections(INetworkDataCarrier.class)) {
            List<INetworkDataCarrier> carriers = new ArrayList<>();
            carriers.add(this);

            c.carry(carriers, packet);
        }
    }


}
