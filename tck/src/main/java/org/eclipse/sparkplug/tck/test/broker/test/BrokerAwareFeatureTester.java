/*******************************************************************************
 * Copyright (c) 2022 Anja Helmbrecht-Schaar HiveMQ
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Anja Helmbrecht-Schaar HiveMQ - initial implementation and documentation
 *******************************************************************************/
package org.eclipse.sparkplug.tck.test.broker.test;

import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import org.eclipse.sparkplug.tck.test.broker.test.results.AwareTestResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient.Mqtt3Publishes;
import static org.eclipse.sparkplug.tck.test.common.TopicConstants.TOPIC_ROOT_SP_BV_1_0;

public class BrokerAwareFeatureTester {

    private static Logger Logger = LoggerFactory.getLogger("Sparkplug");
    private final String host;
    private final int port;
    private final String username;
    private final ByteBuffer password;
    private final MqttClientSslConfig sslConfig;

    public BrokerAwareFeatureTester(final @NotNull String host,
                                    final @NotNull Integer port,
                                    final @Nullable String username,
                                    final @Nullable ByteBuffer password,
                                    final @Nullable MqttClientSslConfig sslConfig,
                                    final int timeOut) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.sslConfig = sslConfig;
    }

    public void finish(Mqtt3Client subscriber) {
        disconnectIfConnected(subscriber);
    }

    // Helpers

    public @NotNull Mqtt3ClientBuilder getClientBuilder(String identifier) {
        return Mqtt3Client.builder()
                .identifier(identifier)
                .serverHost(host)
                .serverPort(port)
                .simpleAuth(buildAuth())
                .sslConfig(sslConfig);
    }

    private @Nullable Mqtt3SimpleAuth buildAuth() {
        if (username != null && password != null) {
            return Mqtt3SimpleAuth.builder()
                    .username(username)
                    .password(password)
                    .build();
        } else if (username != null) {
            Mqtt5SimpleAuth.builder()
                    .username(username)
                    .build();
        } else if (password != null) {
            throw new IllegalArgumentException("Password-Only Authentication is not allowed in MQTT 3");
        }
        return null;
    }

    private void disconnectIfConnected(final @NotNull Mqtt3Client... clients) {
        for (Mqtt3Client client : clients) {
            if (client.getState().isConnected()) {
                client.toBlocking().disconnect();
            }
        }
    }
}


