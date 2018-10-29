package com.jevaengine.spacestation.entity.network.protocols;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;

public class PingProtocol {
    private final static int PROTOCOL_HEADER = 0x5053; //PS
    private final static int PORT = 25;

    public static boolean decode(NetworkPacket p) {
        if(p.data[0] != PROTOCOL_HEADER || p.Port != PORT)
            return false;

        return true;
    }

    public static NetworkPacket encode() {
        NetworkPacket packet = new NetworkPacket();

        packet.Port = PORT;
        packet.data[0] = PROTOCOL_HEADER;

        return packet;
    }
}
