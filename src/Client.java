import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.*;
import java.util.logging.Level;

//Класс, считывающий мужчин и женщин из файла и создающий агентов
public class Client extends Agent {

    @Override
    protected void setup()
    {
        System.out.println(getLocalName() + ": готов к созданию агентов");
        Object[] args = this.getArguments();
        if (args != null && args.length == 2)
        {
            String filenameMen = (String) args[0];//сначала men.txt
            String filenameWomen = (String) args[1];//потом women.txt

            AgentContainer container = getContainerController();

            //Создание мужчин
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(filenameMen), "cp1251")))
            {
                String s;
                int count = 1;
                while ((s = input.readLine()) != null)
                {
                    String[] params = s.split(",");

                    String name = "Man" + count;
                    try
                    {
                        AgentController controller = container.createNewAgent(name, "ManAgent",
                                new Object[]{params[0], params[1], params[2], params[3]});
                        controller.start();
                    } catch (StaleProxyException e)
                    {
                        e.printStackTrace();
                    }
                    count++;
                }
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }

            //Создание женщин
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(filenameWomen), "cp1251")))
            {
                String s;
                int count = 1;
                while ((s = input.readLine()) != null)
                {
                    String[] params = s.split(",");

                    String name = "Woman" + count;
                    try
                    {
                        AgentController controller = container.createNewAgent(name, "WomanAgent",
                                new Object[]{params[0], params[1], params[2], params[3]});
                        controller.start();
                    } catch (StaleProxyException e)
                    {
                        e.printStackTrace();
                    }
                    count++;
                }
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }

            //очищаем файл
            try
            {
                FileWriter fileWriter = new FileWriter("output.txt");
                fileWriter.write("");
                fileWriter.close();
            } catch (IOException ioe)
            {
                System.err.println("Can't open output.txt");
                ioe.printStackTrace();
            }
        } else
        {
            System.out.println(getLocalName() + ": неверно заданы аргументы");
        }

        //ждем инициализации
        this.doWait(1000);

        //отправляем серверу сообщение о готовности
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Server");
        template.addServices(sd);
        try
        {
            DFAgentDescription[] result = DFService.search(this, template);
            AID server = result[0].getName();

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(server);
            msg.setConversationId("Client-Server");
            send(msg);
        } catch (FIPAException e)
        {
            e.printStackTrace();
        }

        doDelete();
    }
}
