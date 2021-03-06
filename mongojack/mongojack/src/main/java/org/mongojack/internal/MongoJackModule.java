/*
 * Copyright 2011 VZ Netzwerke Ltd
 * Copyright 2014 devbliss GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mongojack.internal;

import org.mongojack.internal.stream.ServerErrorProblemHandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The ObjectID serialising module
 * 
 * @author James Roper
 * @since 1.0
 */
public class MongoJackModule extends Module {
    public static final Module INSTANCE = new MongoJackModule();

    /**
     * Configure the given object mapper to be used with MongoJack. Please call
     * this method rather than calling
     * objectMapper.with(MongoJacksonMapperModule.INSTANCE), because Jacksons
     * module system doesn't allow MongoJack to do all the configuration it
     * needs to do. This method will do that configuration though.
     * 
     * @param objectMapper
     *            The object mapper to configure
     * @return This object mapper (for chaining)
     */
    public static ObjectMapper configure(ObjectMapper objectMapper) {
        objectMapper.registerModule(INSTANCE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    
    public String getModuleName() {
        return "Object ID Module";
    }

    
    public Version version() {
        return new Version(1, 0, 0, null, "org.mongojack", "mongojack");
    }

    
    public void setupModule(SetupContext context) {
        MongoAnnotationIntrospector annotationIntrospector = new MongoAnnotationIntrospector(
                context.getTypeFactory());
        context.insertAnnotationIntrospector(annotationIntrospector);
        // Only include non null properties, this makes it possible to use
        // object templates for querying and
        // partial object retrieving
        context.addDeserializationProblemHandler(new ServerErrorProblemHandler());
        context.addSerializers(new MongoJackSerializers());
        context.addDeserializers(new MongoJackDeserializers());
    }
}
