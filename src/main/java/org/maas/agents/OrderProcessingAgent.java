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
import org.maas.utils.Data;

@SuppressWarnings("serial")
public class OrderProcessingAgent extends BaseAgent {
	private List<String> productType;
	private List<String> productPrice;
	private String bakeryName;
	private Data seller = new Data();
	private int n = 0;
	
    protected void setup() {
    	super.setup();
    	bakeryName = getAID().getLocalName();
    	
        System.out.println("Bakery " + bakeryName + " is ready.");
    	
    	register("OrderProcessing", bakeryName);
    	
    	seller.retrieve("src/main/resources/config/small/bakeries.json");
    	
    	Map<String,List<String>> map = new HashMap();
    	map = seller.getProduct(bakeryName);
    	
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
    						
    						proposal = seller.checkAvailability(incomingOrder, bakeryName, productType, productPrice);
    						
    					} catch (JSONException e) {
    						System.out.println("fail to get content");
    						e.printStackTrace();
    					}
    				} else {
    					System.out.println("message is not JSON");
    				}
                	
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
    			}
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
        System.out.println("Bakery " + bakeryName + " receive " + n + " order");
        System.out.println("\t"+getAID().getLocalName()+" terminating.");
    }
}