/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.server.monitor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.TTimeoutTransport;
import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.server.conf.ServerConfiguration;
import org.apache.log4j.Logger;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class ZooKeeperStatus implements Runnable {
  
  private static final Logger log = Logger.getLogger(ZooKeeperStatus.class);
  
  private volatile boolean stop = false;
  
  public static class ZooKeeperState {
    public final String keeper;
    public final String mode;
    public final int clients;
    
    public ZooKeeperState(String keeper, String mode, int clients) {
      this.keeper = keeper;
      this.mode = mode;
      this.clients = clients;
    }
  }
  
  private static Collection<ZooKeeperState> status = Collections.emptyList();
  
  public static Collection<ZooKeeperState> getZooKeeperStatus() {
    return status;
  }
  
  public void stop() {
    this.stop = true;
  }
  
  @Override
  public void run() {
    
    while (!stop) {
      
      List<ZooKeeperState> update = new ArrayList<ZooKeeperState>();
      
      String zookeepers[] = ServerConfiguration.getSystemConfiguration().get(Property.INSTANCE_ZK_HOST).split(",");
      for (String keeper : zookeepers) {
        int clients = 0;
        String mode = "unknown";
        
        String[] parts = keeper.split(":");
        TTransport transport = null;
        try {
          InetSocketAddress addr;
          if (parts.length > 1) addr = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
          else addr = new InetSocketAddress(parts[0], 2181);
          
          transport = TTimeoutTransport.create(addr, 10 * 1000l);
          transport.write("stat\n".getBytes(), 0, 5);
          StringBuilder response = new StringBuilder();
          try {
            transport.flush();
            byte[] buffer = new byte[1024 * 100];
            int n = 0;
            while ((n = transport.read(buffer, 0, buffer.length)) > 0) {
              response.append(new String(buffer, 0, n));
            }
          } catch (TTransportException ex) {
            // happens at EOF
          }
          for (String line : response.toString().split("\n")) {
            if (line.startsWith(" ")) clients++;
            if (line.startsWith("Mode")) mode = line.split(":")[1];
          }
          update.add(new ZooKeeperState(keeper, mode, clients));
        } catch (Exception ex) {
          log.info("Exception talking to zookeeper " + keeper, ex);
          update.add(new ZooKeeperState(keeper, "Down", -1));
        } finally {
          if (transport != null) {
            try {
              transport.close();
            } catch (Exception ex) {
              log.error(ex, ex);
            }
          }
        }
      }
      status = update;
      UtilWaitThread.sleep(1000);
    }
  }
  
}
