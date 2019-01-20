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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.maas.utils.ui.*; 


public class CustomerGUI  extends Agent {

    private VisualisationMain guiWindow;
    public int counter;

	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Visualization-agent "+getAID().getName()+" is ready.");

        this.counter = 0;
        // launch the gui window in another thread
        new Thread() {
            @Override
            public void run() {
                VisualisationMain.run(new String[] {});
            }
        }.start();
        guiWindow = VisualisationMain.waitForInstance();
		//addBehaviour(new MessageServer());
	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

    // private class MessageServer extends CyclicBehaviour {
    //     public void action() {
    //         ACLMessage msg = myAgent.receive();
    //         if (msg != null) {
    //           
    //         }
    //         else {
    //             block();
    //         }
    //     }
    // }
}