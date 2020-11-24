import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static jade.lang.acl.MessageTemplate.MatchConversationId;
import static jade.lang.acl.MessageTemplate.MatchPerformative;

public class Server extends Agent {
    AID[] manAgents;

    @Override
    protected void setup()
    {
        //регистрация в df
        registerDF();
        //ждать присоединения клиента
        this.addBehaviour(new SimpleBehaviour() {
            boolean isDone = false;

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
                        manAgents = findDF("Man");

                        //отправляем сообщение мужчинам
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        for (AID man : manAgents)
                            msg.addReceiver(man);
                        msg.setConversationId("Server-Man");
                        send(msg);
                        addBehaviour(new CalculateAverageBehaviour());
                        isDone = true;
                    } catch (FIPAException e)
                    {
                        e.printStackTrace();
                    }
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

    //Поиск мужчин
    public AID[] findDF(String type) throws FIPAException
    {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        template.addServices(sd);
        DFAgentDescription[] result = DFService.search(this, template);
        AID[] agents = new AID[result.length];

        for (int i = 0; i < result.length; ++i)
            agents[i] = result[i].getName();

        return agents;
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

    @Override
    protected void takeDown()
    {
        try
        {
            DFService.deregister(this);
        } catch (FIPAException fe)
        {
            fe.printStackTrace();
        }
    }

    //Ждем сообщений от мужчин для вычисления среднего
    class CalculateAverageBehaviour extends SimpleBehaviour {
        boolean isDone = false;
        int total = 0;
        int replies = 0;
        int manCount = 0;
        boolean stage1 = true; //сбор со всех мужчин
        boolean stage2 = false; // сбор с мужчин выше среднего
        boolean stage3 = false; // сбор с мужчин ниже среднего

        @Override
        public void action()
        {
            //Собираем сообщения от мужчин с разницей в росте и вычисляем среднее
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Server-Man"), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage reply = myAgent.receive();
            if (reply != null)
            {
                int r = Integer.parseInt(reply.getContent());
                if (r > 0)
                {
                    manCount++;
                    total += r;
                }
                replies++;

                if (replies >= manAgents.length && stage1)
                {
                    System.out.println("Stage 1 end, average: " + Double.toString((double) total / manCount));
                    try //обновляем информацию о мужчинах на случай, если кто-то из старых не нашел пары
                    {
                        manAgents = findDF("Man");
                    }catch (FIPAException e)
                    {
                        e.printStackTrace();
                    }

                    //Отправляем мужчинам сообщение со средним значением роста
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(Double.toString((double) total / manCount));
                    for (AID man : manAgents)
                        msg.addReceiver(man);
                    msg.setConversationId("Server-Man");
                    send(msg);

                    stage1 = false;
                    stage2 = true;
                    total = replies = manCount = 0;
                    doWait(1000);
                    try
                    {
                        manAgents = findDF("AboveAverageMan");
                    }catch (FIPAException e)
                    {
                        e.printStackTrace();
                    }
                }

                else if (replies >= manAgents.length && stage2)
                {
                    System.out.println("Stage 2 end");
                    //Отправляем мужчинам с разницей ниже среднего сообщение для завершения
                    try
                    {
                        manAgents = findDF("BelowAverageMan");
                    }catch (FIPAException e)
                    {
                        e.printStackTrace();
                    }
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    for (AID man : manAgents)
                        msg.addReceiver(man);
                    msg.setConversationId("Server-Man");
                    send(msg);

                    stage2 = false;
                    stage3 = true;
                    replies = 0;
                }

                else if (replies >= manAgents.length && stage3)
                {
                    System.out.println("Stage 3 end, average: " + Double.toString((double) total / manCount));
                    try
                    {
                        FileWriter fileWriter = new FileWriter("output.txt", true);
                        fileWriter.write("Средняя разница роста: " + Double.toString((double) total / manCount));
                        fileWriter.close();
                    }catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                    }
                    isDone = true;
                }
            } else
                block();
        }

        @Override
        public boolean done()
        {
            return isDone;
        }
    }
}
