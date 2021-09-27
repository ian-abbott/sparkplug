/********************************************************************************
 * Copyright (c) 2014, 2021 Cirrus Link Solutions and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Cirrus Link Solutions - initial implementation
 *   Ian Craggs - add checks for Sparkplug TCK
 ********************************************************************************/

package org.eclipse.sparkplug.tck.test;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.message.model.Topic;
import org.eclipse.tahu.util.TopicUtil;

import org.eclipse.sparkplug.tck.sparkplug.Sections;
import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecVersion;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

@SpecVersion(
        spec = "sparkplug",
        version = "3.0.0-SNAPSHOT")
public class MQTTListener implements MqttCallbackExtended {

	// Configuration
	private String serverUrl = "tcp://localhost:1883";
	private String clientId = "Sparkplug MQTT Listener";
	private String username = "admin";
	private String password = "changeme";
	private String log_topic_name = "SPARKPLUG_TCK/LOG";
	private MqttTopic log_topic = null;
	private MqttClient client;
	
	private String primary_host_application_id = null;
	
    private HashMap testResults = new HashMap<String, String>();
    String[] testIds = {
    	"intro-secondary-host-state"
    };
	
	public void log(String message) {
		try {
			MqttMessage mqttmessage = new MqttMessage(message.getBytes());
			log_topic.publish(mqttmessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		MQTTListener listener = new MQTTListener();
		listener.run(args);
	}
	
	public void run(String[] args) {
		System.out.println("*** Sparkplug TCK MQTT Listener ***");
		
        if (args.length < 1) {
        	System.out.println("Parameter must be: primary_host_application_id");
        	return;
        }
        
        primary_host_application_id = args[0];
        System.out.println("Primary host application id is "+primary_host_application_id);
        
        testResults = new HashMap<String, String>();
        
        for (int i = 0; i < testIds.length; ++i) {
            testResults.put(testIds[i], "PASS");
        }
		
		try {
			// Connect to the MQTT Server
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(30);
			options.setKeepAliveInterval(30);
			//options.setUserName(username);
			//options.setPassword(password.toCharArray());
			client = new MqttClient(serverUrl, clientId);
			client.setTimeToWait(5000);						// short timeout on failure to connect
			client.setCallback(this);
			log_topic = client.getTopic(log_topic_name);
			client.connect(options);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		System.out.println("Connected!");
		
		try {
			// Just listen to all DDATA messages on spBv1.0 topics and wait for inbound messages
			client.subscribe(new String[]{"spBv1.0/#", "STATE/#", log_topic_name}, new int[]{2, 2, 2});
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("The MQTT Connection was lost! - will auto-reconnect");
    }

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			if (topic.startsWith("STATE/") ) {
				System.out.println("Sparkplug message: "+ topic + " " + new String(message.getPayload()));
				checkState(topic, message);
			} else if (topic.equals(log_topic_name)) {
				System.out.println("TCK log: "+ new String(message.getPayload()));
			} else {
				Topic sparkplugTopic = TopicUtil.parseTopic(topic);
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(Include.NON_NULL);

				System.out.println("Message arrived on Sparkplug topic " + sparkplugTopic.toString());

				SparkplugBPayloadDecoder decoder = new SparkplugBPayloadDecoder();
				SparkplugBPayload inboundPayload = decoder.buildFromByteArray(message.getPayload());

				// Convert the message to JSON and print to system.out
				String payloadString = mapper.writeValueAsString(inboundPayload);
				System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inboundPayload));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("Published message: " + token);
	}
	
	@SpecAssertion(
    		section = Sections.INTRODUCTION_SECONDARY_HOST_APPLICATION,
    		id = "intro-secondary-host-state")
	public void checkState(String topic, MqttMessage message) {
		String[] words = topic.split("/");
		if (words.length == 2) {
			if (!words[1].equals(primary_host_application_id)) {
				testResults.put("intro-secondary-host-state", "FAIL");
			}
		} else {
			log("STATE message with wrong topic "+topic);
		}
	}
	
    @SpecAssertion(
    		section = Sections.TOPICS_SPARKPLUG_TOPIC_NAMESPACE_ELEMENTS,
    		id = "topic-structure")
    @SpecAssertion(
    		section = Sections.INTRODUCTION_SPARKPLUG_IDS,
    		id = "intro-group-id-string")
	public void checkTopic(String[] elements) {
		if (elements[0].equals("STATE")) {
			
		} else {
			if (elements.length < 4) {
				log("topic-structure: FAIL (too few topic elements)");
			} else {
				String namespace = elements[0];
				String group_id = elements[1];
				String message_type = elements[2];
				String edge_node_id = elements[3];
				String device_id = elements[4];
			}
		}
	}
    
    public void checkMQTTChars(String string) {
    	
    }
    
}