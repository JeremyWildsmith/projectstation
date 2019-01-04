package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.IInteractableEntity;
import com.jevaengine.spacestation.entity.network.protocols.BinarySignalProtocol;
import com.jevaengine.spacestation.entity.network.protocols.PingProtocol;
import com.jevaengine.spacestation.entity.power.IDevice;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

public class NetworkToggleControl extends NetworkDevice implements IInteractableEntity {

    private final IAnimationSceneModel m_model;

    private int m_destAddress;
    private boolean m_isOn;

    public NetworkToggleControl(String name, IAnimationSceneModel model, int ipAddress, int destAddress, boolean isOn) {
        super(name, true, ipAddress);
        m_model = model;
        m_isOn = isOn;
        m_destAddress = destAddress;
    }

    @Override
    protected void connectionChanged() { }

    @Override
    protected boolean canConnectTo(IDevice d) {
        if(!super.canConnectTo(d))
            return false;

        if(!(d instanceof NetworkWire))
            return false;

        return true;
    }

    @Override
    protected void processPacket(NetworkPacket p) {
        if(PingProtocol.decode(p)) {
            transmitStateSignal(p.SenderAddress);
        } else {
            BinarySignalProtocol.BinarySignal signal =BinarySignalProtocol.decode(p);
            if(signal != null && signal.signal != m_isOn)
            {
                m_isOn = signal.signal;
                transmitStateSignal(m_destAddress);
            }
        }
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    private void transmitStateSignal(int reciever) {
        boolean transmitSignal = m_isOn;

        NetworkPacket packet = BinarySignalProtocol.encode(new BinarySignalProtocol.BinarySignal(transmitSignal));
        packet.RecieverAddress = reciever;

        transmitMessage(packet);
    }

    @Override
    public void update(int delta) {

        String animation = m_isOn ? "on" : "off";

        m_model.getAnimation(animation).setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);

        m_model.setDirection(this.getBody().getDirection());
    }

    @Override
    public void interactedWith(IRpgCharacter subject) {
        m_isOn = !m_isOn;
        transmitStateSignal(m_destAddress);
    }

    @Override
    public void interactWith(IRpgCharacter subject, String interaction) {
        interactedWith(subject);
    }

    @Override
    public String[] getInteractions() {
        return new String[0];
    }
}
