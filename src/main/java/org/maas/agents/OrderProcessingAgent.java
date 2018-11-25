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
	
    protected void setup() {
    	super.setup();
    	bakeryName = getAID().getLocalName();
    	
        System.out.println(bakeryName + " is ready.");
    	
    	register("Bakery-Seller", "Bakery");
    	
    	seller.retrieve("src/main/resources/config/small/bakeries.json");
    	
    	Map<String,List<String>> map = new HashMap();
    	map = seller.getProduct(bakeryName);
    	
    	productType = map.get("productType");
    	productPrice = map.get("productPrice");
    	
    	//System.out.println("productType: " + productType);
    	//System.out.println("productPrice: " + productPrice);
        
        addBehaviour(new OfferRequestsServer());
    }
      
    protected void takeDown() {
        deRegister();
        System.out.println("\t"+getAID().getLocalName()+" terminating.");
    }
    
    private class OfferRequestsServer extends CyclicBehaviour {
    	public void action() {
    		//Receive order request from a customer
        	MessageTemplate mr = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage request = myAgent.receive(mr);
            
            MessageTemplate mc = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage confirm = myAgent.receive(mc);
            
            //Process the request if it is not empty and it is in JSON language
            if (request != null) {
            	JSONObject incomingRequest = new JSONObject();
            	JSONObject proposal = new JSONObject();
            	
            	if (request.getLanguage().equals("JSON")) {
					try {
						//System.out.println("Received Request: " + request.getContent());
						incomingRequest = new JSONObject(request.getContent());
						
						proposal = seller.checkAvailability(incomingRequest, bakeryName, productType, productPrice);
						
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
            	
            	//Reply the request with a proposal
                ACLMessage reply = request.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                //reply.setContent("Got your order.");
				reply.setLanguage("JSON");
				reply.setContent(proposal.toString());
				sendMessage(reply);
            } else if (confirm != null) {
            	JSONObject confirmation = new JSONObject();
    			
                //Process the request if it is not empty and it is in JSON language
                if (confirm.getLanguage().equals("JSON")) {
    					System.out.println(bakeryName + " received confirm: " + confirm.getContent());
                }
            } else {
                //System.out.println("Could not read order");
                block();
            }
        }
    }
    
    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
					shutdownMessage.addReceiver(myAgent.getAMS());
					shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
					shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
				myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				myAgent.send(shutdownMessage);
			} catch (Exception e) {
				//LOGGER.error(e);
			}
		}
	}
}