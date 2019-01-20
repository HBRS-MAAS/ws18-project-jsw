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


public class CustomerGUI  extends BaseAgent{

    private VisualisationMain gui;
    public int counter;

	protected void setup() {
        super.setup();
		System.out.println("Hello! CustomerGUI "+getAID().getName()+" is ready.");
  
        this.counter = 0;
        register("CustomerGUI","CustomerGUI");
        // launch the gui window in another thread
        new Thread() {
            @Override
            public void run() {
                VisualisationMain.run(new String[] {});
            }
        }.start();
        gui = VisualisationMain.waitForInstance();
		addBehaviour(new MessageServer());
	}
	protected void takeDown() {
        deRegister();
		System.out.println(getAID().getLocalName() + ": Terminating.");
    }
    

    private class MessageServer extends CyclicBehaviour {
        private MessageTemplate mt;

		public void action() {
			mt = MessageTemplate.and(MessageTemplate.MatchSender(new AID("CustomerAgent", AID.ISLOCALNAME)),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);
    
            if (msg != null) {

              
            }
            else {
                block();
            }
        }
    }
}