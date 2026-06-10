/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.aem.analyser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class LogLevelSuppressor implements AutoCloseable{

    private static final org.slf4j.Logger selfLogger = LoggerFactory.getLogger(LogLevelSuppressor.class);
    
    private final Level level;
    private final Set<AutoCloseable> delegates = new HashSet<>(); 
    
    public LogLevelSuppressor(Level level, String... loggers){
      
        this.level = level;
        try{
            for(String logger: loggers){
                delegates.add(new Delegate(logger));
            }

        }catch (Exception ex){
            selfLogger.warn("Failed to suppress repoinit logs. {}", ex.getMessage());
        }
    }
    
    @Override
    public void close() throws Exception {
        for(AutoCloseable delegate : delegates){
            delegate.close();
        }
    }
    
    class Delegate implements AutoCloseable {
        private final Level savedLoggerLevel;
        private final Logger loggerObject;
        
        public Delegate(String targetLogger) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            loggerObject = (Logger) LoggerFactory.getLogger(targetLogger);
            savedLoggerLevel = getLoggerLevel();
            setLoggerLevel(level);
        }

        private Level getLoggerLevel() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            return (Level) loggerObject.getClass().getMethod("getLevel").invoke(loggerObject);
        }

        private void setLoggerLevel( Level level) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Class<?> levelClass = Level.class;
            loggerObject.getClass().getMethod("setLevel", levelClass).invoke(loggerObject, level);

        }

        @Override
        public void close() {
            try{
                setLoggerLevel(savedLoggerLevel);
            } catch (Exception ex){
                selfLogger.error("Failed to reset repoinit loggers. {}", ex.getMessage());
            }
           
        }
    }
}
