package com.jevaengine.spacestation.entity.network.protocols;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;

public class BinarySignalProtocol {
    private final static int PROTOCOL_HEADER = 0x4253; //BS
    private final static int PORT = 55;

    public static BinarySignal decode(NetworkPacket p) {
        if(p.data[0] != PROTOCOL_HEADER || p.Port != PORT)
            return null;

        BinarySignal signal = new BinarySignal(p.data[1] != 0);

        return signal;
    }

    public static NetworkPacket encode(BinarySignal signal) {
        NetworkPacket packet = new NetworkPacket();

        packet.Port = PORT;
        packet.data[0] = PROTOCOL_HEADER;
        packet.data[1] = signal.signal ? 1 : 0;

        return packet;
    }


    public static class BinarySignal {
        public boolean signal;

        public BinarySignal(boolean signal) {
            this.signal = signal;
        }
    }
}
