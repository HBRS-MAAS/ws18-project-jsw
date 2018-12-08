package org.maas;

import java.util.List;
import java.util.Vector;
import org.maas.utils.*;

public class Start {
	private static List<String> customerName;
	private static List<String> bakeryName;
	
	public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	
    	agents.add("TimeKeeper:org.maas.agents.TimeKeeper");
    	
    	Data customer = new Data();
    	customer.retrieve("src/main/resources/config/small/clients.json");
    	customerName = customer.getName();
    	
    	int n = 0;
    	for (String name : customerName) {
    		agents.add(name + ":org.maas.agents.CustomerAgent");
    		
    		//n++;
    		//if (n > 2) {break;}
    		//break;
    	}
    	
    	Data bakery = new Data();
    	bakery.retrieve("src/main/resources/config/small/bakeries.json");
    	bakeryName = bakery.getName();
    	
    	for (String name : bakeryName) {
    		agents.add(name + ":org.maas.agents.OrderProcessingAgent");
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
