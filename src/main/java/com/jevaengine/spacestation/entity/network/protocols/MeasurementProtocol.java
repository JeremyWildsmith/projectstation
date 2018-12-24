package com.jevaengine.spacestation.entity.network.protocols;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;

public class MeasurementProtocol {
    private final static int PROTOCOL_HEADER = 0x4D53; //MS
    private final static int PORT = 60;

    public static int[] getParts(float f) {
        String v = String.valueOf(f);
        String parts[] = v.split("\\.");

        int left = Integer.valueOf(parts[0]);
        int right = 0;

        if(parts.length > 1) {
            if(parts[1].length() > 5) {
                parts[1] = parts[1].substring(0, 5);
            }
            right = Integer.valueOf(parts[1]);
        }

        left = Math.min(0xFFFF, left);

        return new int[] {left, right};
    }

    public static MeasurementSignal decode(NetworkPacket p) {
        if(p.data[0] != PROTOCOL_HEADER || p.Port != PORT)
            return null;

        MeasurementSignal signal = new MeasurementSignal();

        for(int i = 0; i < 4; i++) {
            signal.measurement[i] = (char)p.data[1 + i];
        }

        signal.upper = p.data[5];
        signal.lower = p.data[6];

        return signal;
    }

    public static NetworkPacket encode(MeasurementSignal signal) {
        NetworkPacket packet = new NetworkPacket();

        packet.Port = PORT;
        packet.data[0] = PROTOCOL_HEADER;

        for(int i = 0; i < 4; i++) {
            packet.data[i + 1] = signal.measurement[i];
        }

        packet.data[5] = signal.upper;
        packet.data[6] = signal.lower;

        return packet;
    }


    public static class MeasurementSignal {
        public final char[] measurement = new char[4];
        public int upper;
        public int lower;

        public MeasurementSignal() {

        }

        public MeasurementSignal(String name, int upper, int lower) {
            for(int i = 0; i < 4; i++) {
                if(i >= name.length())
                    measurement[i] = 0;
                else
                    measurement[i] = name.charAt(i);
            }

            this.upper = upper;
            this.lower = lower;
        }
    }
}
