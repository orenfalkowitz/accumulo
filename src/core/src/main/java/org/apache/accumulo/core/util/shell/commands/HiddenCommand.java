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
package org.apache.accumulo.core.util.shell.commands;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.accumulo.core.util.shell.Shell;
import org.apache.accumulo.core.util.shell.ShellCommandException;
import org.apache.accumulo.core.util.shell.Shell.Command;
import org.apache.accumulo.core.util.shell.ShellCommandException.ErrorCode;
import org.apache.commons.cli.CommandLine;

public class HiddenCommand extends Command {
  private static Random rand = new SecureRandom();
  
  @Override
  public String description() {
    return "The first rule of Accumulus is: \"You don't talk about Accumulus.\"";
  }
  
  @Override
  public int execute(String fullCommand, CommandLine cl, Shell shellState) throws Exception {
    if (rand.nextInt(10) == 0) {
      shellState.getReader().beep();
      shellState.getReader().printNewline();
      shellState.getReader().printString("Sortacus lives!\n");
      shellState.getReader().printNewline();
    } else throw new ShellCommandException(ErrorCode.UNRECOGNIZED_COMMAND, getName());
    
    return 0;
  }
  
  @Override
  public int numArgs() {
    return Shell.NO_FIXED_ARG_LENGTH_CHECK;
  }
  
  @Override
  public String getName() {
    return "\0\0\0\0";
  }
}