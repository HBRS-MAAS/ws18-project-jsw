package org.maas.agents;

import jade.core.Agent;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Random;

import org.maas.agents.TimeKeeper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.maas.utils.ui.*; 


public class CustomerGUI extends BaseAgent{

    private VisualisationMain gui = new VisualisationMain();
    public int counter;

	protected void setup() {
        super.setup();
		System.out.println(getAID().getLocalName()+" is ready.");
  
        this.counter = 0;
        register("CustomerGUI","gui");
        
        System.out.println("i registered");
        
        // launch the gui window in another thread
        new Thread() {
            @Override
            public void run() {
                gui.run(new String[] {});
            }
        }.start();
        //gui = VisualisationMain.waitForInstance();
		addBehaviour(new MessageServer());
		addBehaviour(new KeepAlive());
	}
	
	protected void takeDown() {
        deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
    }
    
    private class MessageServer extends Behaviour {
        private MessageTemplate mt;

		public void action() {
			if(!getAllowAction()) {
                return;
            }
			
			//System.out.println("message server");
			mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			//System.out.println("message server");
			
            if (msg != null) {
            	JSONObject incomingOrder = new JSONObject();
            	incomingOrder = new JSONObject(msg.getContent());
            	
            	System.out.println("Customer GUI get " + incomingOrder.toString());
              
            }
            else {
                block();
            }
            
            finished();
           	myAgent.addBehaviour(new MessageServer());
        }
		
		public boolean done() {
            return false;
        }
    }
    
    private class KeepAlive extends Behaviour {
    	private boolean close = false;
    	
		public void action() {
			close = gui.getWindowState();
			//System.out.println("close = " + close);
        }

		@Override
		public boolean done() {
			if (close == true ) {
				System.out.println("shutdown");
				addBehaviour(new shutdown());
				return true;
			}
			
			return false;
		}
		
    }
    
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