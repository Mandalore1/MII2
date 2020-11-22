package PingPong;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;

public class SecondAgent extends Agent {
    @Override
    protected void setup(){
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null){
                    System.out.println(msg.getContent());
                } else block();
                block();

                msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Pong");
                msg.addReceiver(new AID("first", AID.ISLOCALNAME));
                send(msg);
            }
        });
    }
}
