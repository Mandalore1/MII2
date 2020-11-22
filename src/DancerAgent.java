import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.util.logging.Level;

//Общий класс для мужчин и женщин
public class DancerAgent extends Agent {
    protected static Logger myLogger = Logger.getMyLogger(DancerAgent.class.getName());

    protected String name;
    protected int height;
    protected DancerСharacteristic selfCharacteristic;
    protected DancerСharacteristic wantedCharacteristic;

    protected String infoString;
    protected String type;

    //Перед вызовом setup в классах-потомках должно быть определено поле type
    @Override
    protected void setup()
    {
        //Аргументы в поля
        Object[] args = getArguments();
        if (args.length == 4)
        {
            name = (String) args[0];
            height = Integer.parseInt((String) args[1]);
            selfCharacteristic = DancerСharacteristic.valueOf((String) args[2]);
            wantedCharacteristic = DancerСharacteristic.valueOf((String) args[3]);
        }

        registerDF();
    }

    //Регистрация в DF
    protected void registerDF()
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        try
        {
            DFService.register(this, dfd);
            myLogger.log(Level.INFO, type + " " + this.getLocalName() + " was registered");
        } catch (FIPAException e)
        {
            myLogger.log(Level.SEVERE, type + " " + this.getLocalName() + " - Cannot register with DF", e);
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
}
