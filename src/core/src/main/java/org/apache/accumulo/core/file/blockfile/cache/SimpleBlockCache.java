/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.accumulo.core.file.blockfile.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple one RFile soft reference cache.
 */
public class SimpleBlockCache implements BlockCache {
  private static class Ref extends SoftReference<byte[]> {
    public String blockId;
    
    public Ref(String blockId, byte buf[], ReferenceQueue<byte[]> q) {
      super(buf, q);
      this.blockId = blockId;
    }
  }
  
  private Map<String,Ref> cache = new HashMap<String,Ref>();
  
  private ReferenceQueue<byte[]> q = new ReferenceQueue<byte[]>();
  public int dumps = 0;
  
  /**
   * Constructor
   */
  public SimpleBlockCache() {
    super();
  }
  
  void processQueue() {
    Ref r;
    while ((r = (Ref) q.poll()) != null) {
      cache.remove(r.blockId);
      dumps++;
    }
  }
  
  /**
   * @return the size
   */
  public synchronized int size() {
    processQueue();
    return cache.size();
  }
  
  public synchronized byte[] getBlock(String blockName) {
    processQueue(); // clear out some crap.
    Ref ref = cache.get(blockName);
    if (ref == null) return null;
    return ref.get();
  }
  
  public synchronized void cacheBlock(String blockName, byte buf[]) {
    cache.put(blockName, new Ref(blockName, buf, q));
  }
  
  public synchronized void cacheBlock(String blockName, byte buf[], boolean inMemory) {
    cache.put(blockName, new Ref(blockName, buf, q));
  }
  
  public void shutdown() {
    // noop
  }
}
