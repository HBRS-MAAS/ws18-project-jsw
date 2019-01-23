package org.maas.agents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class OrderProcessingAgent extends BaseAgent {
	private JSONArray dataArray = new JSONArray();
	private List<String> productType;
	private List<String> productPrice;
	private String bakeryID;
	private int n = 0;
	
    protected void setup() {
    	super.setup();
    	
    	bakeryID = getAID().getLocalName();
    	
        System.out.println(bakeryID + " is ready.");
    	
    	register("OrderProcessing", bakeryID);
    	
    	String scenarioPath = "src/main/resources/config/";
    	
    	Object[] args = getArguments();
		String scenario = "small";
		
        if (args != null && args.length > 0) {
            scenario = (String) args[0];
        }
    	
		retrieve(scenarioPath + scenario + "/bakeries.json");
    	
    	Map<String,List<String>> map = new HashMap();
    	map = getProduct(bakeryID);
    	
    	productType = map.get("productType");
    	productPrice = map.get("productPrice");
    	
    	//System.out.println("productType: " + productType);
    	//System.out.println("productPrice: " + productPrice);
        
        //addBehaviour(new OfferRequestsServer());
    	addBehaviour(new ManageCustomerOrder());
        
    }
    
    private class ManageCustomerOrder extends Behaviour {
        boolean isDone = false;
        
        public void action() {
            if(!getAllowAction()) {
                return;
            }
           	           	
           	//Receive order from a customer
    		MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    		ACLMessage message = myAgent.receive(mt);
    		
    		if (message != null) {
    			if (message.getPerformative() == ACLMessage.CFP) {
                	JSONObject incomingOrder = new JSONObject();
                	JSONObject proposal = new JSONObject();
                	
                	if (message.getLanguage().equals("JSON")) {
    					try {
    						//System.out.println(bakeryName + " receive request: " + message.getContent());
    						incomingOrder = new JSONObject(message.getContent());
    						
    						proposal = checkAvailability(incomingOrder, bakeryID, productType, productPrice);
    				
    						//Reply the CFP with a proposal
    	                	ACLMessage reply = message.createReply();
    	                    reply.setPerformative(ACLMessage.PROPOSE);
    	                    //reply.setContent("Got your order.");
    	    				reply.setLanguage("JSON");
    	    				reply.setContent(proposal.toString());
    	    				sendMessage(reply);
    	    				//System.out.println("sent reply");
    	    				
    	    				//System.out.println(bakeryName + " send proposal: " + proposal.toString());
    	    				
    	    				n++;
    					} catch (JSONException e) {
    						System.out.println("fail to get content");
    						e.printStackTrace();
    					}
    				} else {
    					System.out.println("message is not JSON");
    				}
    			}
    		} else {
    			block();
    		}
    		
    		finished();
           	//System.out.println("currentHour: " + getCurrentHour());
           	myAgent.addBehaviour(new ManageCustomerOrder());
           	isDone = true;
           
        }

        
        public boolean done() {
            return isDone;
        }
    }
    
    protected void takeDown() {
        deRegister();
        System.out.println(bakeryID + " receive " + n + " order");
        System.out.println("\t"+bakeryID+" terminating.");
    }
    
    public void retrieve(String fileName) {
		File file = new File(fileName);
		String filePath = file.getAbsolutePath();
		String fileContent = "";	
		
		try {
			fileContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
			dataArray = new JSONArray(fileContent);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public JSONObject checkAvailability(JSONObject order, String bakeryName, 
    		List<String> sellType, List<String> sellPrice) {
    		JSONObject orderProduct = new JSONObject();
    		JSONObject orderPrice = new JSONObject();
    		JSONObject bakeryPrice = new JSONObject();
    		List<String> orderType = new ArrayList();
    		String orderID = "";		
    		
    		//Get All Order Type
    		try {
    			orderID = order.getString("guid");
    			bakeryPrice.put("guid", orderID);
    			
    			orderProduct = order.getJSONObject("products");
    			
    			Iterator iter = orderProduct.keys();
    			while(iter.hasNext()){
    				String key = (String)iter.next();
    				orderType.add(key);
    			}
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    		
    		//Compare Order Type with Sell Type
    		try {
    			for (int i = 0; i < orderType.size(); i++) {
    				for (int j = 0; j < sellType.size(); j++) {
    					if (orderType.get(i).equals(sellType.get(j))) {
    						orderPrice.put(sellType.get(j), sellPrice.get(j));
    						break;
    					}
    				}
    			}
    			bakeryPrice.put("products", orderPrice);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    		
    		//System.out.println("Bakery Price: " + bakeryPrice);
    		
    		return bakeryPrice;		
    	}
    	
    public Map<String, List<String>> getProduct(String id) {
    	String bakeryID = "";
		JSONArray products = new JSONArray();
		
		List<String> productType = new ArrayList();
		List<String> productPrice = new ArrayList();
		
		//Get Product List
		try {
			for (int i = 0; i < dataArray.length(); i++) {
				bakeryID = dataArray.getJSONObject(i).getString("guid");
				
				if (bakeryID.equals(id)) {
					products = dataArray.getJSONObject(i).getJSONArray("products");
					
					for (int j = 0; j < products.length(); j++) {
						productType.add(products.getJSONObject(j).getString("guid"));
						//productPrice.add(BigDecimal.valueOf(products.getJSONObject(i).getDouble("salesPrice")).floatValue());
						productPrice.add(Double.toString(products.getJSONObject(j).getDouble("salesPrice")));
					}
					
					Map<String,List<String>> map = new HashMap();
					map.put("productType", productType);
					map.put("productPrice", productPrice);
					return map;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
    }	
}
