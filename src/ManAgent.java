import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.FileWriter;
import java.io.IOException;

import static jade.lang.acl.MessageTemplate.MatchConversationId;
import static jade.lang.acl.MessageTemplate.MatchPerformative;
import static java.lang.Math.abs;

public class ManAgent extends DancerAgent {
    private AID[] manAgents;
    private AID[] womanAgents;
    private AID myWoman;
    private AID server;
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
                    server = reply.getSender();
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
        System.out.println(this.getLocalName() + ": завершает работу");
    }

    //Запись в файл
    private void writeAgent()
    {
        try
        {
            FileWriter fileWriter = new FileWriter("output.txt", true);
            fileWriter.write(String.format("Мужчина [Имя] %s, [Рост]%d, [Характеристика]%s, [Желаемая характеристика]%s\nЖенщина [Имя] %s, [Рост]%s, [Характеристика]%s, [Желаемая характеристика]%s\n",
                    name, height, selfCharacteristic, wantedCharacteristic, myWomanParams[0], myWomanParams[1], myWomanParams[2], myWomanParams[3]));
            fileWriter.write("Разница роста: " + abs(height - Integer.parseInt(myWomanParams[1])) + "\n\n");
            fileWriter.close();
        }catch (IOException ioe)
        {
            System.err.println("Can't open output.txt");
            ioe.printStackTrace();
        }
    }

    // Отправление запроса женщинам на создание пары
    private class SendPairRequestBehaviour extends SimpleBehaviour {
        private int step = 1;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        private boolean isDone = false;

        @Override
        public void action()
        {
            switch (step)
            {
                case 1: // поиск женщин
                    System.out.println(getLocalName() + ": начинает поиск женщин");
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
                        System.out.println(getLocalName() + ": не найдена ни одна женщина");
                    }
                    step = 2;
                    break;

                case 2: // рассылка запросов
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

                case 3: // прием ответов от женщин
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
                        System.out.println(getLocalName() + ": не нашел женщин (все отказали)");
                    } else
                    {
                        ACLMessage message = new ACLMessage(ACLMessage.CONFIRM);
                        message.addReceiver(myWoman);
                        message.setConversationId("Men-Women");
                        send(message);
                        System.out.println(getLocalName() + ": нашел женщину " + myWoman.getLocalName());
                    }
                    step = 5;
                case 5: //отправка на сервер
                    //writeAgent();
                    //отправляем информацию с разницей в росте на сервер, если не найдена женщина, отправим -1
                    int myDiff;
                    if (myWoman != null)
                        myDiff = abs(height - Integer.parseInt(myWomanParams[1]));
                    else
                        myDiff = -1;

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(Integer.toString(myDiff));
                    msg.addReceiver(server);
                    msg.setConversationId("Server-Man");
                    send(msg);

                    //Удалим мужчин без женщин
                    if (myWoman == null)
                        doDelete();
                    else
                    {
                        //для обмена меняем посылаемую информацию
                        infoString = name + "," + height + "," + selfCharacteristic.toString() + "," + wantedCharacteristic.toString() +
                                ";" + myWomanParams[0] + "," + myWomanParams[1] + "," + myWomanParams[2] + "," + myWomanParams[3] + "," + myWoman.getName();

                        //ждать сообщения от сервера со средним значением
                        reply = blockingReceive();
                        double average = Double.parseDouble(reply.getContent());
                        if (myDiff > average) //если разница выше средней
                        {
                            registerDFAs("AboveAverageMan");
                            doWait(1000);
                            //System.out.println(getLocalName() + String.format(": %d > %f", myDiff, average));
                            addBehaviour(new SendExchangeRequestBehaviour(average));
                        } else //если разница ниже средней
                        {
                            registerDFAs("BelowAverageMan");
                            //System.out.println(getLocalName() + String.format(": %d <= %f", myDiff, average));
                            addBehaviour(new ReceiveExchangeRequestBehaviour(average));
                        }
                    }
                    isDone = true;
                    break;
            }
        }

        @Override
        public boolean done()
        {
            return isDone;
        }
    }

    // Отправка запроса мужчинам на обмен парой
    private class SendExchangeRequestBehaviour extends SimpleBehaviour {
        int step = 1;
        double average;
        int repliesCnt = 0;
        MessageTemplate mt;
        boolean isDone = false;

        SendExchangeRequestBehaviour(double average)
        {
            this.average = average;
        }

        @Override
        public void action()
        {
            switch (step)
            {
                case 1: // поиск мужчин для обмена
                    System.out.println(getLocalName() + ": начинает посылку запросов на обмен");
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("BelowAverageMan");
                    template.addServices(sd);
                    try
                    {
                        DFAgentDescription[] result = DFService.search(this.myAgent, template);
                        manAgents = new AID[result.length];

                        for (int i = 0; i < result.length; ++i)
                            manAgents[i] = result[i].getName();
                    } catch (FIPAException e)
                    {
                        e.printStackTrace();
                        System.out.println(getLocalName() + ": не найден ни один мужчина");
                        isDone = true;
                    }
                    step = 2;
                    block();
                    break;

                case 2: // рассылка запросов
                    mt = MessageTemplate.MatchConversationId("Men-Men"); //шаблон для ответов

                    boolean accepted = false; //ответил ли хотя бы один мужчина
                    for (AID manAgent : manAgents)
                    {
                        ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
                        cfp.addReceiver(manAgent); // добавляем в получатели очередного мужчину
                        cfp.setConversationId("Men-Men"); // это задаем для поиска сообщений
                        cfp.setContent(infoString);
                        myAgent.send(cfp); // отправляем сообщение про нас

                        ACLMessage reply = blockingReceive(); //принимаем ответ
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                        {
                            accepted = true;
                            String[] buff;
                            buff = reply.getContent().split(";");
                            buff = buff[1].split(",");
                            String[] womanParams = {buff[0], buff[1], buff[2], buff[3]};

                            myWoman = new AID(buff[4], true);
                            myWomanParams = womanParams;

                            //для обмена меняем посылаемую информацию
                            infoString = name + "," + height + "," + selfCharacteristic.toString() + "," + wantedCharacteristic.toString() +
                                    ";" + myWomanParams[0] + "," + myWomanParams[1] + "," + myWomanParams[2] + "," + myWomanParams[3] + "," + myWoman.getName();
                        }
                    }

                    if (!accepted)//если никто не ответил, заканчиваем
                    {
                        //отправим сообщение серверу с разницей
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setContent(Integer.toString(abs(height - Integer.parseInt(myWomanParams[1]))));
                        msg.addReceiver(server);
                        msg.setConversationId("Server-Man");
                        send(msg);

                        isDone = true;
                        writeAgent();
                    }
                    break;
            }
        }

        @Override
        public boolean done()
        {
            return isDone;
        }
    }

    // Прием запроса от мужчин на обмен парой
    private class ReceiveExchangeRequestBehaviour extends SimpleBehaviour {
        int step = 1;
        double average;
        MessageTemplate mt;
        ACLMessage reply;
        boolean isDone = false;

        ReceiveExchangeRequestBehaviour(double average)
        {
            this.average = average;
        }

        private boolean acceptExchange(String[] otherWomanParams, String[] otherManParams)
        {
            int otherManHeight = Integer.parseInt(otherManParams[1]);
            DancerСharacteristic otherManCharacteristic = DancerСharacteristic.valueOf(otherManParams[2]);
            DancerСharacteristic otherManWantedCharacteristic = DancerСharacteristic.valueOf(otherManParams[3]);

            int otherWomanHeight = Integer.parseInt(otherWomanParams[1]);
            DancerСharacteristic otherWomanCharacteristic = DancerСharacteristic.valueOf(otherWomanParams[2]);
            DancerСharacteristic otherWomanWantedCharacteristic = DancerСharacteristic.valueOf(otherWomanParams[3]);

            int myWomanHeight = Integer.parseInt(myWomanParams[1]);
            DancerСharacteristic myWomanCharacteristic = DancerСharacteristic.valueOf(myWomanParams[2]);
            DancerСharacteristic myWomanWantedCharacteristic = DancerСharacteristic.valueOf(myWomanParams[3]);

            //в первую очередь проверим совместимость
            boolean p1 = (otherWomanCharacteristic == wantedCharacteristic || otherWomanWantedCharacteristic == selfCharacteristic)
                    && (myWomanCharacteristic == otherManWantedCharacteristic || myWomanWantedCharacteristic == otherManCharacteristic);
            //затем проверим, уменьшится ли разница в росте
            boolean p2 = (abs(otherManHeight - otherWomanHeight) + abs(height - myWomanHeight)) > (abs(otherManHeight - myWomanHeight) + abs(height - otherWomanHeight));

            return p1 && p2;
        }

        @Override
        public void action()
        {
            switch (step)
            {
                case 1:
                    System.out.println(getLocalName() + ": начинает прием запросов на обмен");
                    step = 2;
                    block();
                    break;
                case 2: //принимаем сообщение от мужчины
                    mt = MessageTemplate.and(MatchConversationId("Men-Men"), MatchPerformative(ACLMessage.PROPOSE));
                    mt = MessageTemplate.or(mt, MatchConversationId("Server-Man"));
                    reply = myAgent.receive(mt);
                    if (reply != null && reply.getConversationId().equals("Men-Men"))
                    {
                        String[] buff;
                        buff = reply.getContent().split(";");

                        String[] buff2 = buff[1].split(",");
                        AID otherWoman = new AID(buff2[4], true);
                        String[] otherWomanParams = {buff2[0], buff2[1], buff2[2], buff2[3]};

                        buff2 = buff[0].split(",");
                        String[] otherManParams = {buff2[0], buff2[1], buff2[2], buff2[3]};

                        if (acceptExchange(otherWomanParams, otherManParams)) //если подходит, меняемся
                        {
                            System.out.println(getLocalName() + ": обмен с мужчиной " + reply.getSender().getLocalName());
                            ACLMessage cfp = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            cfp.addReceiver(reply.getSender());
                            cfp.setConversationId("Men-Men");
                            cfp.setContent(infoString);
                            myAgent.send(cfp);

                            myWoman = otherWoman;
                            myWomanParams = otherWomanParams;

                            //для обмена меняем посылаемую информацию
                            infoString = name + "," + height + "," + selfCharacteristic.toString() + "," + wantedCharacteristic.toString() +
                                    ";" + myWomanParams[0] + "," + myWomanParams[1] + "," + myWomanParams[2] + "," + myWomanParams[3] + "," + myWoman.getName();
                        }
                        else //если не подходит, отправляем отказ
                        {
                            ACLMessage cfp = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            cfp.addReceiver(reply.getSender());
                            cfp.setConversationId("Men-Men");
                            myAgent.send(cfp);
                        }
                    }
                    else if (reply != null && reply.getConversationId().equals("Server-Man"))
                    {
                        //отправим сообщение серверу с разницей
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setContent(Integer.toString(abs(height - Integer.parseInt(myWomanParams[1]))));
                        msg.addReceiver(server);
                        msg.setConversationId("Server-Man");
                        send(msg);

                        isDone = true;
                        writeAgent();
                    }
                    else
                        block();
                    break;
            }
        }

        @Override
        public boolean done()
        {
            return isDone;
        }
    }
}
