package org.maas;

import java.util.List;
import java.util.Vector;
import org.maas.utils.*;

public class Start {
	private static List<String> customerID;
	private static List<String> bakeryID;
	
	public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	
    	agents.add("TimeKeeper:org.maas.agents.TimeKeeper");
    	
    	Data customer = new Data();
    	customer.retrieve("src/main/resources/config/small/clients.json");
    	customerID = customer.getID();
    	
    	int n = 0;
    	for (String id : customerID) {
    		agents.add(id + ":org.maas.agents.CustomerAgent");
    		
    		//n++;
    		//if (n > 2) {break;}
    		break;
    	}
    	
    	Data bakery = new Data();
    	bakery.retrieve("src/main/resources/config/small/bakeries.json");
    	bakeryID = bakery.getID();
    	
    	for (String id : bakeryID) {
    		agents.add(id + ":org.maas.agents.OrderProcessingAgent");
    		//break;
    	}   	
    	
    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a);
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
