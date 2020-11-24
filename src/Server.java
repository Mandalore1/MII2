import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static jade.lang.acl.MessageTemplate.MatchConversationId;
import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class Server extends Agent {
    @Override
    protected void setup()
    {
        //регистрация в df
        registerDF();
        //ждать присоединения клиента
        this.addBehaviour(new CyclicBehaviour() {
            @Override
            public void action()
            {
                MessageTemplate mt = MessageTemplate.and(MatchConversationId("Client-Server"), MatchPerformative(ACLMessage.INFORM));
                ACLMessage reply = myAgent.receive(mt);
                //отправляем мужчинам запрос на начало поиска женщин
                if (reply != null)
                {
                    //ищем мужчин
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Man");
                    template.addServices(sd);
                    try
                    {
                        DFAgentDescription[] result = DFService.search(this.myAgent, template);
                        AID[] manAgents = new AID[result.length];

                        for (int i = 0; i < result.length; ++i)
                            manAgents[i] = result[i].getName();


                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        for (AID man : manAgents)
                            msg.addReceiver(man);
                        msg.setConversationId("Server-Man");
                        send(msg);
                    } catch (FIPAException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                    block();
            }
        });
    }

    //Регистрация в DF
    protected void registerDF()
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Server");
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        try
        {
            DFService.register(this, dfd);
            System.out.println(this.getLocalName() + ": зарегистрирован в DF");
        } catch (FIPAException e)
        {
            System.out.println(this.getLocalName() + ": невозможно зарегистрировать в DF");
            e.printStackTrace();
            doDelete();
        }
    }
}
