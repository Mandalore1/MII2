import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.util.logging.Level;

//Общий класс для мужчин и женщин
public class DancerAgent extends Agent {

    protected String name;
    protected int height;
    protected DancerСharacteristic selfCharacteristic;
    protected DancerСharacteristic wantedCharacteristic;

    protected String infoString;
    protected String type;
    private boolean registered = false;

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

        registerDFAs(type);
    }

    //Регистрация в DF
    protected void registerDFAs(String type)
    {
        if (registered)
        {
            deregisterDF();
        }
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        try
        {
            DFService.register(this, dfd);
            registered = true;
            System.out.println(this.getLocalName() + ": зарегистрирован в DF как " + type);
        } catch (FIPAException e)
        {
            System.out.println(this.getLocalName() + ": невозможно зарегистрировать в DF");
            e.printStackTrace();
            doDelete();
        }
    }

    protected void deregisterDF()
    {
        try
        {
            DFService.deregister(this);
        } catch (FIPAException fe)
        {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown()
    {
        deregisterDF();
    }
}
