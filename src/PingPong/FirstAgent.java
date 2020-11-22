package PingPong;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class FirstAgent extends Agent{
    @Override
    protected void setup(){
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Ping");
                msg.addReceiver(new AID("second", AID.ISLOCALNAME));
                send(msg);

                msg = receive();
                if (msg != null){
                    System.out.println(msg.getContent());
                } else block();
            }
        });
    }
}
