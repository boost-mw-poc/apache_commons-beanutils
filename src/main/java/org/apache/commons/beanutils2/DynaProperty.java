/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.beanutils2;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * The metadata describing an individual property of a DynaBean.
 * </p>
 *
 * <p>
 * The meta contains an <em>optional</em> content type property ({@link #getContentType}) for use by mapped and iterated properties. A mapped or iterated
 * property may choose to indicate the type it expects. The DynaBean implementation may choose to enforce this type on its entries. Alternatively, an
 * implementation may choose to ignore this property. All keys for maps must be of type String so no meta data is needed for map keys.
 * </p>
 */
public class DynaProperty {

    /*
     * There are issues with serializing primitive class types on certain JVM versions (including Java 1.3). This class uses a custom serialization
     * implementation that writes an integer for these primitive class. This list of constants are the ones used in serialization. If these values are changed,
     * then older versions will no longer be read correctly
     */
    private static final int BOOLEAN_TYPE = 1;
    private static final int BYTE_TYPE = 2;
    private static final int CHAR_TYPE = 3;
    private static final int DOUBLE_TYPE = 4;
    private static final int FLOAT_TYPE = 5;
    private static final int INT_TYPE = 6;
    private static final int LONG_TYPE = 7;
    private static final int SHORT_TYPE = 8;

    /**
     * Empty array.
     */
    public static final DynaProperty[] EMPTY_ARRAY = {};

    /** Property name */
    protected String name;

    /** Property type */
    protected transient Class<?> type;

    /** The <em>(optional)</em> type of content elements for indexed {@code DynaProperty} */
    protected transient Class<?> contentType;

    /**
     * Constructs a property that accepts any data type.
     *
     * @param name Name of the property being described
     */
    public DynaProperty(final String name) {
        this(name, Object.class);
    }

    /**
     * Constructs a property of the specified data type.
     *
     * @param name Name of the property being described
     * @param type Java class representing the property data type
     */
    public DynaProperty(final String name, final Class<?> type) {
        this.name = name;
        this.type = type;
        if (type != null && type.isArray()) {
            this.contentType = type.getComponentType();
        }
    }

    /**
     * Constructs an indexed or mapped {@code DynaProperty} that supports (pseudo)-introspection of the content type.
     *
     * @param name        Name of the property being described
     * @param type        Java class representing the property data type
     * @param contentType Class that all indexed or mapped elements are instances of
     */
    public DynaProperty(final String name, final Class<?> type, final Class<?> contentType) {
        this.name = name;
        this.type = type;
        this.contentType = contentType;
    }

    /**
     * Checks this instance against the specified Object for equality. Overrides the default reference test for equality provided by
     * {@link Object#equals(Object)}
     *
     * @param obj The object to compare to
     * @return {@code true} if object is a dyna property with the same name type and content type, otherwise {@code false}
     * @since 1.8.0
     */
    @Override
    public boolean equals(final Object obj) {
        boolean result;

        result = obj == this;

        if (!result && obj instanceof DynaProperty) {
            final DynaProperty that = (DynaProperty) obj;
            result = Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type) && Objects.equals(this.contentType, that.contentType);
        }

        return result;
    }

    /**
     * Gets the <em>(optional)</em> type of the indexed content for {@code DynaProperty}'s that support this feature.
     *
     * <p>
     * There are issues with serializing primitive class types on certain JVM versions (including Java 1.3). Therefore, this field <strong>must not be
     * serialized using the standard methods</strong>.
     * </p>
     *
     * @return the Class for the content type if this is an indexed {@code DynaProperty} and this feature is supported. Otherwise null.
     */
    public Class<?> getContentType() {
        return contentType;
    }

    /**
     * Gets the name of this property.
     *
     * @return the name of the property
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Gets the Java class representing the data type of the underlying property values.
     * </p>
     *
     * <p>
     * There are issues with serializing primitive class types on certain JVM versions (including Java 1.3). Therefore, this field <strong>must not be
     * serialized using the standard methods</strong>.
     * </p>
     *
     * <p>
     * <strong>Please leave this field as {@code transient}</strong>
     * </p>
     *
     * @return the property type
     */
    public Class<?> getType() {
        return this.type;
    }

    /**
     * @return the hash code for this dyna property
     * @see Object#hashCode
     * @since 1.8.0
     */
    @Override
    public int hashCode() {
        int result = 1;

        result = result * 31 + (name == null ? 0 : name.hashCode());
        result = result * 31 + (type == null ? 0 : type.hashCode());
        result = result * 31 + (contentType == null ? 0 : contentType.hashCode());

        return result;
    }

    /**
     * Does this property represent an indexed value (ie an array or List)?
     *
     * @return {@code true} if the property is indexed (i.e. is a List or array), otherwise {@code false}
     */
    public boolean isIndexed() {
        if (type == null) {
            return false;
        }
        if (type.isArray() || List.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    /**
     * Does this property represent a mapped value (ie a Map)?
     *
     * @return {@code true} if the property is a Map otherwise {@code false}
     */
    public boolean isMapped() {
        if (type == null) {
            return false;
        }
        return Map.class.isAssignableFrom(type);

    }

    /**
     * Gets a String representation of this Object.
     *
     * @return a String representation of the dyna property
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DynaProperty[name=");
        sb.append(this.name);
        sb.append(",type=");
        sb.append(this.type);
        if (isMapped() || isIndexed()) {
            sb.append(" <").append(this.contentType).append(">");
        }
        sb.append("]");
        return sb.toString();
    }

}
