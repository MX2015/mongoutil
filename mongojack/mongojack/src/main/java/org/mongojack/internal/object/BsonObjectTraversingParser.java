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
import java.math.BigDecimal;
import java.math.BigInteger;

import org.bson.BSONObject;
import org.mongojack.JacksonDBCollection;
import org.mongojack.internal.JacksonDBCollectionProvider;
import org.mongojack.internal.util.VersionUtils;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.mongodb.BasicDBObject;

/**
 * Parses a BSONObject by traversing it. This class was copied from
 * {@link com.fasterxml.jackson.databind.node.TreeTraversingParser} and then
 * adapted to be for BSONObject's, rather than JsonNode's.
 * 
 * While decoding by the cursor uses DBDecoderBsonParser, there are still things
 * that need to be decoded from the DBObjects, including the result of
 * findAndModify, and saved objects after saving.
 * 
 * @author James Roper
 * @since 1.0
 */
public class BsonObjectTraversingParser extends ParserMinimalBase implements
        JacksonDBCollectionProvider {

    private final JacksonDBCollection dbCollection;

    /*
     * /********************************************************** /*
     * Configuration /**********************************************************
     */

    protected ObjectCodec objectCodec;

    /**
     * Traversal context within tree
     */
    protected BsonObjectCursor nodeCursor;

    /*
     * /********************************************************** /* State
     * /**********************************************************
     */

    /**
     * Sometimes parser needs to buffer a single look-ahead token; if so, it'll
     * be stored here. This is currently used for handling
     */
    protected JsonToken nextToken;

    /**
     * Flag needed to handle recursion into contents of child Array/Object
     * nodes.
     */
    protected boolean startContainer;

    /**
     * Flag that indicates whether parser is closed or not. Gets set when parser
     * is either closed by explicit call ({@link #close}) or when end-of-input
     * is reached.
     */
    protected boolean closed;

    /*
     * /********************************************************** /* Life-cycle
     * /**********************************************************
     */
    public BsonObjectTraversingParser(JacksonDBCollection dbCollection,
            Object rootValue, ObjectCodec codec) {
        this(dbCollection, new BasicDBObject("root", rootValue), null);
        try {
            nextToken();
            nextToken();
            nextToken();
        } catch (IOException e) {
            // Ignore
        }
    }

    public BsonObjectTraversingParser(JacksonDBCollection dbCollection,
            BSONObject o, ObjectCodec codec) {
        super(0);
        this.dbCollection = dbCollection;
        objectCodec = codec;
        if (o instanceof Iterable) {
            nextToken = JsonToken.START_ARRAY;
            nodeCursor = new BsonObjectCursor.ArrayCursor((Iterable) o, null);
        } else {
            nextToken = JsonToken.START_OBJECT;
            nodeCursor = new BsonObjectCursor.ObjectCursor(o, null);
        }
    }

    
    public Version version() {
        return VersionUtils.VERSION;
    }

    
    public void setCodec(ObjectCodec c) {
        objectCodec = c;
    }

    
    public ObjectCodec getCodec() {
        return objectCodec;
    }

    /*
     * /********************************************************** /* Closeable
     * implementation
     * /**********************************************************
     */

    
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            nodeCursor = null;
            _currToken = null;
        }
    }

    /*
     * /********************************************************** /* Public
     * API, traversal
     * /**********************************************************
     */

    
    public JsonToken nextToken() throws IOException {
        if (nextToken != null) {
            _currToken = nextToken;
            nextToken = null;
            return _currToken;
        }
        // are we to descend to a container child?
        if (startContainer) {
            startContainer = false;
            // minor optimization: empty containers can be skipped
            if (!nodeCursor.currentHasChildren()) {
                _currToken = (_currToken == JsonToken.START_OBJECT) ? JsonToken.END_OBJECT
                        : JsonToken.END_ARRAY;
                return _currToken;
            }
            nodeCursor = nodeCursor.iterateChildren();
            _currToken = nodeCursor.nextToken();
            if (_currToken == JsonToken.START_OBJECT
                    || _currToken == JsonToken.START_ARRAY) {
                startContainer = true;
            }
            return _currToken;
        }
        // No more content?
        if (nodeCursor == null) {
            closed = true; // if not already set
            return null;
        }
        // Otherwise, next entry from currentFieldName cursor
        _currToken = nodeCursor.nextToken();
        if (_currToken != null) {
            if (_currToken == JsonToken.START_OBJECT
                    || _currToken == JsonToken.START_ARRAY) {
                startContainer = true;
            }
            return _currToken;
        }
        // null means no more children; need to return end marker
        _currToken = nodeCursor.endToken();
        nodeCursor = nodeCursor.getParent();
        return _currToken;
    }

    
    public JsonParser skipChildren() throws IOException {
        if (_currToken == JsonToken.START_OBJECT) {
            startContainer = false;
            _currToken = JsonToken.END_OBJECT;
        } else if (_currToken == JsonToken.START_ARRAY) {
            startContainer = false;
            _currToken = JsonToken.END_ARRAY;
        }
        return this;
    }

    
    public boolean isClosed() {
        return closed;
    }

    /*
     * /********************************************************** /* Public
     * API, token accessors
     * /**********************************************************
     */

    
    public String getCurrentName() {
        return (nodeCursor == null) ? null : nodeCursor.getCurrentName();
    }

    
    public JsonStreamContext getParsingContext() {
        return nodeCursor;
    }

    
    public JsonLocation getTokenLocation() {
        return JsonLocation.NA;
    }

    
    public JsonLocation getCurrentLocation() {
        return JsonLocation.NA;
    }

    /*
     * /********************************************************** /* Public
     * API, access to textual content
     * /**********************************************************
     */

    
    public String getText() {
        if (closed) {
            return null;
        }
        // need to separate handling a bit...
        switch (_currToken) {
            case FIELD_NAME:
                return nodeCursor.getCurrentName();
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_EMBEDDED_OBJECT:
                return currentNode().toString();
        }

        return _currToken.asString();
    }

    
    public char[] getTextCharacters() throws IOException {
        return getText().toCharArray();
    }

    
    public int getTextLength() throws IOException {
        return getText().length();
    }

    
    public int getTextOffset() throws IOException {
        return 0;
    }

    
    public boolean hasTextCharacters() {
        // generally we do not have efficient access as char[], hence:
        return false;
    }

    /*
     * /********************************************************** /* Public
     * API, typed non-text access
     * /**********************************************************
     */

    // public byte getByteValue() throws IOException

    
    public NumberType getNumberType() throws IOException {
        Object n = currentNode();
        if (n instanceof Integer) {
            return NumberType.INT;
        } else if (n instanceof BigInteger) {
            return NumberType.BIG_INTEGER;
        } else if (n instanceof BigDecimal) {
            return NumberType.BIG_DECIMAL;
        } else if (n instanceof Double) {
            return NumberType.DOUBLE;
        } else if (n instanceof Float) {
            return NumberType.FLOAT;
        } else if (n instanceof Long) {
            return NumberType.LONG;
        } else {
            throw _constructError(n + " is not a number");
        }
    }

    
    public BigInteger getBigIntegerValue() throws IOException {
        Number n = currentNumericNode();
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        } else {
            return BigInteger.valueOf(n.longValue());
        }
    }

    
    public BigDecimal getDecimalValue() throws IOException {
        Number n = currentNumericNode();
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        } else {
            return BigDecimal.valueOf(n.doubleValue());
        }
    }

    
    public double getDoubleValue() throws IOException {
        return currentNumericNode().doubleValue();
    }

    
    public float getFloatValue() throws IOException {
        return currentNumericNode().floatValue();
    }

    
    public long getLongValue() throws IOException {
        return currentNumericNode().longValue();
    }

    
    public int getIntValue() throws IOException {

        return currentNumericNode().intValue();
    }

    
    public Number getNumberValue() throws IOException {
        return currentNumericNode();
    }

    private Number currentNumericNode() throws JsonParseException {
        Object n = currentNode();
        if (n instanceof Number) {
            return (Number) n;
        } else {
            throw _constructError(n + " is not a number");
        }
    }

    /*
     * /********************************************************** /* Public
     * API, typed binary (base64) access
     * /**********************************************************
     */

    
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        Object n = currentNode();
        if (n instanceof byte[]) {
            return (byte[]) n;
        } else if (n instanceof org.bson.types.ObjectId) {
            return ((org.bson.types.ObjectId) n).toByteArray();
        }
        return null;
    }

    
    public Object getEmbeddedObject() throws IOException {
        return currentNode();
    }

    
    protected void _handleEOF() throws JsonParseException {
        // There is no EOF?
    }

    
    public void overrideCurrentName(String name) {
        // Hmm... do nothing?
    }

    /*
     * /********************************************************** /* Internal
     * methods /**********************************************************
     */

    protected Object currentNode() {
        if (closed || nodeCursor == null) {
            return null;
        }
        return nodeCursor.currentNode();
    }

    
    public JacksonDBCollection getDBCollection() {
        return dbCollection;
    }
}
