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
package org.mongojack.internal.object;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.mongojack.internal.util.VersionUtils;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * JSON generator that actually generates a BSON object
 *
 * @author James Roper
 * @since 1.0
 */
public class BsonObjectGenerator extends JsonGenerator {
    private Node rootNode;
    private ObjectCodec objectCodec;
    private Node currentNode;
    private boolean closed = false;

    public DBObject getDBObject() {
        if (rootNode instanceof ObjectNode) {
            return ((ObjectNode) rootNode).get();
        } else {
            throw new IllegalStateException("Object node was not generated");
        }
    }

    public Object getValue() {
        return rootNode.get();
    }

    
    public Version version() {
        return VersionUtils.VERSION;
    }

    
    public JsonGenerator enable(Feature f) {
        return this;
    }

    
    public JsonGenerator disable(Feature f) {
        return this;
    }

    
    public boolean isEnabled(Feature f) {
        return false;
    }

    
    public int getFeatureMask() {
        return JsonGenerator.Feature.collectDefaults();
    }

    
    public JsonGenerator setFeatureMask(int i) { return this; }

    
    public JsonGenerator setCodec(ObjectCodec oc) {
        objectCodec = oc;
        return this;
    }

    
    public ObjectCodec getCodec() {
        return objectCodec;
    }

    
    public JsonGenerator useDefaultPrettyPrinter() {
        return this;
    }

    
    public void writeStartArray() throws IOException {
        if (rootNode == null) {
            rootNode = new ArrayNode(null);
            currentNode = rootNode;
        } else {
            currentNode = new ArrayNode(currentNode);
        }
    }

    
    public void writeEndArray() throws IOException {
        Object array = currentNode.get();
        currentNode = currentNode.getParent();
        if (currentNode != null) {
            currentNode.set(array);
        }
    }

    
    public void writeStartObject() throws IOException {
        if (rootNode == null) {
            rootNode = new ObjectNode(null);
            currentNode = rootNode;
        } else {
            currentNode = new ObjectNode(currentNode);
        }
    }

    
    public void writeEndObject() throws IOException {
        Object object = currentNode.get();
        currentNode = currentNode.getParent();
        if (currentNode != null) {
            currentNode.set(object);
        }
    }

    
    public void writeFieldName(String name) throws IOException {
        currentNode.setName(name);
    }

    
    public void writeString(String text) throws IOException {
        setValue(text);
    }

    
    public void writeString(char[] text, int offset, int len)
            throws IOException {
        setValue(new String(text, offset, len));
    }

    
    public void writeRawUTF8String(byte[] text, int offset, int length)
            throws IOException {
        setValue(new String(text, offset, length, "UTF-8"));
    }

    
    public void writeUTF8String(byte[] text, int offset, int length)
            throws IOException {
        setValue(new String(text, offset, length, "UTF-8"));
    }

    
    public void writeRaw(String text) throws IOException {
        throw new UnsupportedOperationException("Writing raw not supported");
    }

    
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException("Writing raw not supported");
    }

    
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException("Writing raw not supported");
    }

    
    public void writeRaw(char c) throws IOException {
        throw new UnsupportedOperationException("Writing raw not supported");
    }

    
    public void writeRawValue(String text) throws IOException {
        setValue(text);
    }

    
    public void writeRawValue(String text, int offset, int len)
            throws IOException {
        setValue(text.substring(offset, offset + len));
    }

    
    public void writeRawValue(char[] text, int offset, int len)
            throws IOException {
        setValue(new String(text, offset, len));
    }

    
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset,
            int len) throws IOException {
        if (offset != 0 || len != data.length) {
            byte[] subset = new byte[len];
            System.arraycopy(data, offset, subset, 0, len);
            data = subset;
        }
        setValue(data);
    }

    
    public int writeBinary(Base64Variant b64variant, InputStream data,
            int dataLength) throws IOException, JsonGenerationException {
        throw new UnsupportedOperationException();
    }

    
    public void writeNumber(int v) throws IOException {
        setValue(v);
    }

    
    public void writeNumber(long v) throws IOException {
        setValue(v);
    }

    
    public void writeNumber(BigInteger v) throws IOException {
        setValue(v);
    }

    
    public void writeNumber(double d) throws IOException {
        setValue(d);
    }

    
    public void writeNumber(float f) throws IOException {
        setValue(f);
    }

    
    public void writeNumber(BigDecimal dec) throws IOException {
        setValue(dec);
    }

    
    public void writeNumber(String encodedValue) throws IOException,
            UnsupportedOperationException {
        setValue(encodedValue);
    }

    
    public void writeBoolean(boolean state) throws IOException {
        setValue(state);
    }

    
    public void writeNull() throws IOException {
        setValue(null);
    }

    
    public void writeObject(Object pojo) throws IOException {
        setValue(pojo);
    }

    
    public void writeFieldName(SerializableString name) throws IOException,
            JsonGenerationException {
        writeFieldName(name.getValue());
    }

    
    public void writeString(SerializableString text) throws IOException,
            JsonGenerationException {
        setValue(text.getValue());
    }

    
    public void writeTree(TreeNode rootNode) throws IOException,
            JsonProcessingException {
        throw new UnsupportedClassVersionError(
                "Writing JSON nodes not supported");
    }

    
    public void copyCurrentEvent(JsonParser jp) throws IOException {
        JsonToken t = jp.getCurrentToken();
        switch (t) {
            case START_OBJECT:
                writeStartObject();
                break;
            case END_OBJECT:
                writeEndObject();
                break;
            case START_ARRAY:
                writeStartArray();
                break;
            case END_ARRAY:
                writeEndArray();
                break;
            case FIELD_NAME:
                writeFieldName(jp.getCurrentName());
                break;
            case VALUE_STRING:
                if (jp.hasTextCharacters()) {
                    writeString(jp.getTextCharacters(), jp.getTextOffset(),
                            jp.getTextLength());
                } else {
                    writeString(jp.getText());
                }
                break;
            case VALUE_NUMBER_INT:
                switch (jp.getNumberType()) {
                    case INT:
                        writeNumber(jp.getIntValue());
                        break;
                    case BIG_INTEGER:
                        writeNumber(jp.getBigIntegerValue());
                        break;
                    default:
                        writeNumber(jp.getLongValue());
                }
                break;
            case VALUE_NUMBER_FLOAT:
                switch (jp.getNumberType()) {
                    case BIG_DECIMAL:
                        writeNumber(jp.getDecimalValue());
                        break;
                    case FLOAT:
                        writeNumber(jp.getFloatValue());
                        break;
                    default:
                        writeNumber(jp.getDoubleValue());
                }
                break;
            case VALUE_TRUE:
                writeBoolean(true);
                break;
            case VALUE_FALSE:
                writeBoolean(false);
                break;
            case VALUE_NULL:
                writeNull();
                break;
            case VALUE_EMBEDDED_OBJECT:
                writeObject(jp.getEmbeddedObject());
                break;
        }
    }

    
    public void copyCurrentStructure(JsonParser jp) throws IOException {
        JsonToken t = jp.getCurrentToken();

        // Let'string handle field-name separately first
        if (t == JsonToken.FIELD_NAME) {
            writeFieldName(jp.getCurrentName());
            t = jp.nextToken();
            // fall-through to copy the associated value
        }

        switch (t) {
            case START_ARRAY:
                writeStartArray();
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    copyCurrentStructure(jp);
                }
                writeEndArray();
                break;
            case START_OBJECT:
                writeStartObject();
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    copyCurrentStructure(jp);
                }
                writeEndObject();
                break;
            default: // others are simple:
                copyCurrentEvent(jp);
        }
    }

    
    public JsonStreamContext getOutputContext() {
        return currentNode;
    }

    
    public void flush() throws IOException {
    }

    
    public boolean isClosed() {
        return closed;
    }

    
    public void close() throws IOException {
        closed = true;
    }

    private void setValue(Object value) {
        if (rootNode == null) {
            rootNode = new RootValueNode(value);
        } else {
            currentNode.set(value);
        }
    }

    /**
     * A node that we are currently building from
     */
    private abstract class Node extends JsonStreamContext {
        private final Node parent;
        private String name;

        private Node(Node parent, int contextType) {
            this.parent = parent;
            _type = contextType;
            _index = -1;
        }

        
        public Node getParent() {
            return parent;
        }

        public void setName(String name) {
            this.name = name;
        }

        
        public String getCurrentName() {
            return name;
        }

        abstract void set(Object value);

        abstract Object get();
    }

    /**
     * A node that represents an object
     */
    private class ObjectNode extends Node {
        private final BasicDBObject object;

        private ObjectNode(Node parent) {
            super(parent, JsonStreamContext.TYPE_OBJECT);
            object = new BasicDBObject();
        }

         void set(Object value) {
            object.put(getCurrentName(), value);
        }

         DBObject get() {
            return object;
        }
    }

    /**
     * A node that represents an array
     */
    private class ArrayNode extends Node {
        private final List<Object> array = new ArrayList<Object>();

        private ArrayNode(Node parent) {
            super(parent, JsonStreamContext.TYPE_ARRAY);
        }

         void set(Object value) {
            array.add(value);
        }

         List<Object> get() {
            return array;
        }
    }

    /**
     * A node that represents a root value, so for example if a String is
     * serialised, it will just be a String
     */
    private class RootValueNode extends Node {
        private final Object rootValue;

        private RootValueNode(Object rootValue) {
            super(null, JsonStreamContext.TYPE_ROOT);
            this.rootValue = rootValue;
        }

         void set(Object value) {
            throw new IllegalStateException(
                    "Cannot write multiple values to a root value node");
        }

         Object get() {
            return rootValue;
        }
    }
}
