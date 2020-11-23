import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.logging.Level;

import static jade.lang.acl.MessageTemplate.MatchConversationId;
import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class WomanAgent extends DancerAgent {
    @Override
    protected void setup()
    {
        // Женщины отправляют все данные
        type = "Woman";
        super.setup();
        infoString = name + "," + height + "," + selfCharacteristic.toString() + "," + wantedCharacteristic.toString();

        addBehaviour(new ReceivePairRequestBehaviour());
    }

    @Override
    protected void takeDown()
    {
        super.takeDown();
        System.out.println(this.getLocalName() + ": завершает работу");
    }

    // Принятие запроса от мужчин на создание пары
    private class ReceivePairRequestBehaviour extends CyclicBehaviour {
        private int step = 0;
        private MessageTemplate mt;
        private ACLMessage reply;
        private boolean manFound = false;

        @Override
        public void action()
        {
            switch (step)
            {
                case 0:
                    System.out.println(getLocalName() + ": начинает принимать запросы");
                    step = 1;
                    break;
                case 1: //принимаем сообщение от мужчины
                    mt = MessageTemplate.and(MatchConversationId("Men-Women"), MatchPerformative(ACLMessage.PROPOSE));
                    reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        String[] replyFromMan = reply.getContent().split(",");
                        DancerСharacteristic manCharacteristic = DancerСharacteristic.valueOf(replyFromMan[0]);
                        DancerСharacteristic manWantedCharacteristic = DancerСharacteristic.valueOf(replyFromMan[1]);
                        //проверка мужчины на соответствие
                        if (!manFound && (manCharacteristic == wantedCharacteristic || manWantedCharacteristic == selfCharacteristic))
                        {
                            System.out.println(getLocalName() + ": согласие мужчине " + reply.getSender().getLocalName());
                            //если соответствует - отправляем согласие и переходим на следующий шаг
                            ACLMessage cfp = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            cfp.addReceiver(reply.getSender());
                            cfp.setConversationId("Men-Women");
                            cfp.setContent(infoString);
                            myAgent.send(cfp);
                            step = 2;
                        } else
                        {
                            System.out.println(getLocalName() + ": отказ мужчине " + reply.getSender().getLocalName());
                            //если не соответствует - отправляем отказ
                            ACLMessage cfp = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            cfp.addReceiver(reply.getSender());
                            cfp.setConversationId("Men-Women");
                            myAgent.send(cfp);
                        }
                    } else
                        block();
                    break;
                case 2: //принимаем согласие или несогласие от мужчины
                    mt = MessageTemplate.and(MatchConversationId("Men-Women"),
                            MessageTemplate.or(MatchPerformative(ACLMessage.CONFIRM), MatchPerformative(ACLMessage.DISCONFIRM)));
                    reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        //если мужчина согласен
                        if (reply.getPerformative() == ACLMessage.CONFIRM)
                        {
                            manFound = true;
                            System.out.println(getLocalName() + ": нашла мужчину " + reply.getSender().getLocalName());
                        }
                        step = 0;
                    } else
                        block();
                    break;
            }
        }
    }
}
