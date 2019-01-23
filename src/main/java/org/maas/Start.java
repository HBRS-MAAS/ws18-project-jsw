package org.maas;

import java.util.List;
import java.util.Vector;
import org.maas.utils.*;

import org.maas.utils.ui.*;

public class Start {
	private static List<String> customerID;
	private static List<String> bakeryID;
	
	private static String scenario = "large-30-days";
	private static String scenarioPath = "src/main/resources/config/";
	
	public static void main(String[] args) {
    	List<String> agents = new Vector<>();
    	
    	VisualisationController ctrl = new VisualisationController();
    	ctrl.setScenario(scenario);
    	
		agents.add("TimeKeeper:org.maas.agents.TimeKeeper");
		agents.add("CustomerGUI:org.maas.agents.CustomerGUI");
    	
    	Data customer = new Data();
    	customer.retrieve(scenarioPath + scenario + "/clients.json");
    	customerID = customer.getID();
    	
    	int n = 0;
    	for (String id : customerID) {
    		agents.add(id + ":org.maas.agents.CustomerAgent");
    		
    		//n++;
    		//if (n > 2) {break;}
    		//break;
    	}
    	
    	Data bakery = new Data();
    	bakery.retrieve(scenarioPath + scenario + "/bakeries.json");
    	bakeryID = bakery.getID();
    	
    	for (String id : bakeryID) {
    		agents.add(id + ":org.maas.agents.OrderProcessingAgent");
    		//break;
    	}   	
    	
    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a + "(" + scenario + ");");
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
