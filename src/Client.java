import jade.core.Agent;
import jade.util.Logger;

import java.util.logging.Level;

//Класс, считывающий мужчин и женщин из файла и создающий агентов
public class Client extends Agent {
    protected static Logger myLogger = Logger.getMyLogger(DancerAgent.class.getName());

    @Override
    protected void setup()
    {
        myLogger.log(Level.INFO, "Client" + getName() + " is ready to create agents");
        Object[] args = this.getArguments();
        if (args != null && args.length == 2)
        {
            String filenameTasks=(String)args[0];//сначала men.txt
            String filenameDev=(String)args[1];
        }
    }
}
