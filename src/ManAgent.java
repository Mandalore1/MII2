import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.logging.Level;

import static java.lang.Math.abs;

public class ManAgent extends DancerAgent {
    private AID[] manAgents;
    private AID[] womanAgents;
    private AID myWoman;
    private String[] myWomanParams;

    @Override
    protected void setup()
    {
        //Мужчины отправляют свою характеристику и желаемую характеристику
        type = "Man";
        super.setup();
        infoString = selfCharacteristic.toString() + "," + wantedCharacteristic.toString();

        //Ждать указания сервера для начала работы
        this.addBehaviour(new SimpleBehaviour() {
            boolean isDone = false;

            @Override
            public void action()
            {
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Server-Man"), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                ACLMessage reply = myAgent.receive();
                if (reply != null)
                {
                    addBehaviour(new SendPairRequestBehaviour());
                    isDone = true;
                } else
                    block();
            }

            @Override
            public boolean done()
            {
                return isDone;
            }
        });
    }

    @Override
    protected void takeDown()
    {
        super.takeDown();
        myLogger.log(Level.INFO, "ManAgent " + getAID().getName() + " terminating");
    }

    // Отправление запроса женщинам на создание пары
    private class SendPairRequestBehaviour extends SimpleBehaviour {
        private int step = 1;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        private boolean womanFound = false;

        @Override
        public void action()
        {
            switch (step)
            {
                case 1: // поиск женщин
                    myLogger.log(Level.INFO, "ManAgent " + getAID().getName() + " start searching for woman");
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Woman");
                    template.addServices(sd);
                    try
                    {
                        DFAgentDescription[] result = DFService.search(this.myAgent, template);
                        womanAgents = new AID[result.length];

                        for (int i = 0; i < result.length; ++i)
                            womanAgents[i] = result[i].getName(); // берем имя и складываем
                    } catch (FIPAException e)
                    {
                        e.printStackTrace();
                        myLogger.log(Level.SEVERE, myAgent.getName() + ": WomanAgents not found");
                    }
                    step = 2;
                    break;

                case 2: // рассылка запросов
                    //myLogger.log(Level.INFO, "ManAgent " + getAID().getName() + " pending requests to woman");
                    ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
                    for (AID womanAgent : womanAgents)
                        cfp.addReceiver(womanAgent); // добавляем всех женщин в получатели
                    cfp.setConversationId("Men-Women"); // это задаем для поиска сообщений
                    cfp.setContent(infoString);
                    myAgent.send(cfp); // отправляем всем сообщение про нас

                    mt = MessageTemplate.MatchConversationId("Men-Women"); //шаблон для ответов
                    step = 3; // переходим на следующий этап отношений
                    block(); // замораживаем отношения до тех пор, пока не придет ответ
                    break;

                case 3: // принятие ответов от женщин
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) //положительный ответ
                        {
                            String replyFromWoman = reply.getContent(); //ответ женщины
                            String[] womanParams = replyFromWoman.split(",");

                            //если ответила первая женщина, запомним
                            if (myWoman == null)
                            {
                                myWoman = reply.getSender();
                                myWomanParams = womanParams;
                            }

                            //сравним эту женщину с лучшей на данный момент
                            else if (abs(Integer.valueOf(womanParams[1]) - height) < abs(Integer.valueOf(myWomanParams[1]) - height))
                            {
                                //старой женщине отправим отказ
                                ACLMessage message = new ACLMessage(ACLMessage.DISCONFIRM);
                                message.addReceiver(myWoman);
                                message.setConversationId("Men-Women");
                                send(message);
                                //новую женщину запомним
                                myWoman = reply.getSender();
                                myWomanParams = womanParams;
                            } else
                            {
                                //если женщина хуже текущей, отправим отказ
                                ACLMessage message = new ACLMessage(ACLMessage.DISCONFIRM);
                                message.addReceiver(reply.getSender());
                                message.setConversationId("Men-Women");
                                send(message);
                            }
                        }

                        repliesCnt++;
                        if (repliesCnt >= womanAgents.length)
                        {
                            repliesCnt = 0;
                            step = 4;
                        }
                    } else
                        block();
                    break;
                case 4: //ответ лучшей женщине
                    if (myWoman == null)
                    {
                        myLogger.log(Level.INFO, myAgent.getName() + ": didn't find any woman (all rejected)");
                    } else
                    {
                        ACLMessage message = new ACLMessage(ACLMessage.CONFIRM);
                        message.addReceiver(myWoman);
                        message.setConversationId("Men-Women");
                        send(message);
                        myLogger.log(Level.INFO, myAgent.getName() + ": found woman " + myWoman.getName());
                        womanFound = true;
                    }
                    break;
            }
        }

        @Override
        public boolean done()
        {
            return womanFound;
        }
    }

    // Отправление запроса мужчинам на обмен парой
    private class SendExchangeRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action()
        {

        }
    }

    // Принятие запроса от мужчин на обмен парой
    private class ReceiveExchangeRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action()
        {

        }
    }
}

