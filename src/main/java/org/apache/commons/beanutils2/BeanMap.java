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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * An implementation of Map for JavaBeans which uses introspection to get and put properties in the bean.
 * <p>
 * If an exception occurs during attempts to get or set a property then the property is considered non existent in the Map
 * </p>
 */
public class BeanMap extends AbstractMap<String, Object> implements Cloneable {

    /**
     * Map entry used by {@link BeanMap}.
     */
    protected static class Entry extends AbstractMap.SimpleEntry<String, Object> {

        private static final long serialVersionUID = 1L;

        /**
         * The owner.
         */
        private final BeanMap owner;

        /**
         * Constructs a new {@code Entry}.
         *
         * @param owner the BeanMap this entry belongs to
         * @param key   the key for this entry
         * @param value the value for this entry
         */
        protected Entry(final BeanMap owner, final String key, final Object value) {
            super(key, value);
            this.owner = owner;
        }

        /**
         * Sets the value.
         *
         * @param value the new value for the entry
         * @return the old value for the entry
         */
        @Override
        public Object setValue(final Object value) {
            final String key = getKey();
            final Object oldValue = owner.get(key);

            owner.put(key, value);
            final Object newValue = owner.get(key);
            super.setValue(newValue);
            return oldValue;
        }
    }

    /**
     * An empty array. Used to invoke accessors via reflection.
     */
    public static final Object[] NULL_ARGUMENTS = {};

    /**
     * Maps primitive Class types to transformers. The transformer transform strings into the appropriate primitive wrapper.
     *
     * Private & unmodifiable replacement for the (public & static) defaultTransformers instance.
     */
    private static final Map<Class<? extends Object>, Function<?, ?>> typeTransformers = Collections.unmodifiableMap(createTypeTransformers());

    private static Map<Class<? extends Object>, Function<?, ?>> createTypeTransformers() {
        final Map<Class<? extends Object>, Function<?, ?>> defTransformers = new HashMap<>();
        defTransformers.put(Boolean.TYPE, input -> Boolean.valueOf(input.toString()));
        defTransformers.put(Character.TYPE, input -> Character.valueOf(input.toString().charAt(0)));
        defTransformers.put(Byte.TYPE, input -> Byte.valueOf(input.toString()));
        defTransformers.put(Short.TYPE, input -> Short.valueOf(input.toString()));
        defTransformers.put(Integer.TYPE, input -> Integer.valueOf(input.toString()));
        defTransformers.put(Long.TYPE, input -> Long.valueOf(input.toString()));
        defTransformers.put(Float.TYPE, input -> Float.valueOf(input.toString()));
        defTransformers.put(Double.TYPE, input -> Double.valueOf(input.toString()));
        return defTransformers;
    }

    private transient Object bean;

    private final transient HashMap<String, Method> readMethods = new HashMap<>();

    private final transient HashMap<String, Method> writeMethods = new HashMap<>();

    private final transient HashMap<String, Class<? extends Object>> types = new HashMap<>();

    /**
     * Constructs a new empty {@code BeanMap}.
     */
    public BeanMap() {
    }

    // Map interface

    /**
     * Constructs a new {@code BeanMap} that operates on the specified bean. If the given bean is {@code null}, then this map will be empty.
     *
     * @param bean the bean for this map to operate on
     */
    public BeanMap(final Object bean) {
        this.bean = bean;
        initialize();
    }

    /**
     * This method reinitializes the bean map to have default values for the bean's properties. This is accomplished by constructing a new instance of the bean
     * which the map uses as its underlying data source. This behavior for {@code clear()} differs from the Map contract in that the mappings are not actually
     * removed from the map (the mappings for a BeanMap are fixed).
     */
    @Override
    public void clear() {
        if (bean == null) {
            return;
        }
        Class<? extends Object> beanClass = null;
        try {
            beanClass = bean.getClass();
            bean = beanClass.newInstance();
        } catch (final Exception e) {
            throw new UnsupportedOperationException("Could not create new instance of class: " + beanClass, e);
        }
    }

    /**
     * Clone this bean map using the following process:
     *
     * <ul>
     * <li>If there is no underlying bean, return a cloned BeanMap without a bean.
     * <li>Since there is an underlying bean, try to instantiate a new bean of the same type using Class.newInstance().
     * <li>If the instantiation fails, throw a CloneNotSupportedException
     * <li>Clone the bean map and set the newly instantiated bean as the underlying bean for the bean map.
     * <li>Copy each property that is both readable and writable from the existing object to a cloned bean map.
     * <li>If anything fails along the way, throw a CloneNotSupportedException.
     * </ul>
     *
     * @return a cloned instance of this bean map
     * @throws CloneNotSupportedException if the underlying bean cannot be cloned
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final BeanMap newMap = (BeanMap) super.clone();
        if (bean == null) {
            // no bean, just an empty bean map at the moment. return a newly
            // cloned and empty bean map.
            return newMap;
        }
        Object newBean = null;
        final Class<? extends Object> beanClass = bean.getClass(); // Cannot throw Exception
        try {
            newBean = beanClass.newInstance();
        } catch (final Exception e) {
            // unable to instantiate
            final CloneNotSupportedException cnse = new CloneNotSupportedException(
                    "Unable to instantiate the underlying bean \"" + beanClass.getName() + "\": " + e);
            cnse.initCause(e);
            throw cnse;
        }
        try {
            newMap.setBean(newBean);
        } catch (final Exception e) {
            final CloneNotSupportedException cnse = new CloneNotSupportedException("Unable to set bean in the cloned bean map: " + e);
            cnse.initCause(e);
            throw cnse;
        }
        try {
            // copy only properties that are readable and writable. If its
            // not readable, we can't get the value from the old map. If
            // its not writable, we can't write a value into the new map.
            readMethods.keySet().forEach(key -> {
                if (getWriteMethod(key) != null) {
                    newMap.put(key, get(key));
                }
            });
        } catch (final Exception e) {
            final CloneNotSupportedException cnse = new CloneNotSupportedException("Unable to copy bean values to cloned bean map: " + e);
            cnse.initCause(e);
            throw cnse;
        }
        return newMap;
    }

    /**
     * Returns true if the bean defines a property with the given name.
     * <p>
     * The given name must be a {@code String}; if not, this method returns false. This method will also return false if the bean does not define a property
     * with that name.
     * </p>
     * <p>
     * Write-only properties will not be matched as the test operates against property read methods.
     * </p>
     *
     * @param name the name of the property to check
     * @return false if the given name is null or is not a {@code String}; false if the bean does not define a property with that name; or true if the bean does
     *         define a property with that name
     */
    @Override
    public boolean containsKey(final Object name) {
        return getReadMethod(name) != null;
    }

    /**
     * Converts the given value to the given type. First, reflection is used to find a public constructor declared by the given class that takes one argument,
     * which must be the precise type of the given value. If such a constructor is found, a new object is created by passing the given value to that
     * constructor, and the newly constructed object is returned.
     * <p>
     * If no such constructor exists, and the given type is a primitive type, then the given value is converted to a string using its {@link Object#toString()
     * toString()} method, and that string is parsed into the correct primitive type using, for instance, {@link Integer#valueOf(String)} to convert the string
     * into an {@code int}.
     * </p>
     * <p>
     * If no special constructor exists and the given type is not a primitive type, this method returns the original value.
     * </p>
     *
     * @param <R>     The return type.
     * @param newType the type to convert the value to
     * @param value   the value to convert
     * @return the converted value
     * @throws NumberFormatException     if newType is a primitive type, and the string representation of the given value cannot be converted to that type
     * @throws InstantiationException    if the constructor found with reflection raises it
     * @throws InvocationTargetException if the constructor found with reflection raises it
     * @throws IllegalAccessException    never
     * @throws IllegalArgumentException  never
     */
    protected <R> Object convertType(final Class<R> newType, final Object value)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // try call constructor
        try {
            final Constructor<R> constructor = newType.getConstructor(value.getClass());
            return constructor.newInstance(value);
        } catch (final NoSuchMethodException e) {
            // try using the transformers
            final Function<Object, R> transformer = getTypeTransformer(newType);
            if (transformer != null) {
                return transformer.apply(value);
            }
            return value;
        }
    }

    /**
     * Creates an array of parameters to pass to the given mutator method. If the given object is not the right type to pass to the method directly, it will be
     * converted using {@link #convertType(Class,Object)}.
     *
     * @param method the mutator method
     * @param value  the value to pass to the mutator method
     * @return an array containing one object that is either the given value or a transformed value
     * @throws IllegalAccessException   if {@link #convertType(Class,Object)} raises it
     * @throws IllegalArgumentException if any other exception is raised by {@link #convertType(Class,Object)}
     * @throws ClassCastException       if an error occurs creating the method args
     */
    protected Object[] createWriteMethodArguments(final Method method, Object value) throws IllegalAccessException, ClassCastException {
        try {
            if (value != null) {
                final Class<? extends Object>[] paramTypes = method.getParameterTypes();
                if (paramTypes != null && paramTypes.length > 0) {
                    final Class<? extends Object> paramType = paramTypes[0];
                    if (!paramType.isAssignableFrom(value.getClass())) {
                        value = convertType(paramType, value);
                    }
                }
            }

            return new Object[] { value };
        } catch (final InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Convenience method for getting an iterator over the entries.
     *
     * @return an iterator over the entries
     */
    public Iterator<Map.Entry<String, Object>> entryIterator() {
        final Iterator<String> iter = keyIterator();
        return new Iterator<Map.Entry<String, Object>>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Map.Entry<String, Object> next() {
                final String key = iter.next();
                final Object value = get(key);
                // This should not cause any problems; the key is actually a
                // string, but it does no harm to expose it as Object
                return new Entry(BeanMap.this, key, value);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove() not supported for BeanMap");
            }
        };
    }

    /**
     * Gets a Set of MapEntry objects that are the mappings for this BeanMap.
     * <p>
     * Each MapEntry can be set but not removed.
     * </p>
     *
     * @return the unmodifiable set of mappings
     */
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(new AbstractSet<Map.Entry<String, Object>>() {
            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return entryIterator();
            }

            @Override
            public int size() {
                return BeanMap.this.readMethods.size();
            }
        });
    }

    /**
     * Called during a successful {@link #put(String,Object)} operation. Default implementation does nothing. Override to be notified of property changes in the
     * bean caused by this map.
     *
     * @param key      the name of the property that changed
     * @param oldValue the old value for that property
     * @param newValue the new value for that property
     */
    protected void firePropertyChange(final Object key, final Object oldValue, final Object newValue) {
        // noop
    }

    /**
     * Gets the value of the bean's property with the given name.
     * <p>
     * The given name must be a {@link String} and must not be null; otherwise, this method returns {@code null}. If the bean defines a property with the given
     * name, the value of that property is returned. Otherwise, {@code null} is returned.
     * </p>
     * <p>
     * Write-only properties will not be matched as the test operates against property read methods.
     * </p>
     *
     * @param name the name of the property whose value to return
     * @return the value of the property with that name
     */
    @Override
    public Object get(final Object name) {
        if (bean != null) {
            final Method method = getReadMethod(name);
            if (method != null) {
                try {
                    return method.invoke(bean, NULL_ARGUMENTS);
                } catch (final IllegalAccessException | NullPointerException | InvocationTargetException | IllegalArgumentException e) {
                    logWarn(e);
                }
            }
        }
        return null;
    }

    /**
     * Gets the bean currently being operated on. The return value may be null if this map is empty.
     *
     * @return the bean being operated on by this map
     */
    public Object getBean() {
        return bean;
    }

    // Helper methods

    /**
     * Gets the accessor for the property with the given name.
     *
     * @param name the name of the property
     * @return null if the name is null; null if the name is not a {@link String}; null if no such property exists; or the accessor method for that property
     */
    protected Method getReadMethod(final Object name) {
        return readMethods.get(name);
    }

    /**
     * Gets the accessor for the property with the given name.
     *
     * @param name the name of the property
     * @return the accessor method for the property, or null
     */
    public Method getReadMethod(final String name) {
        return readMethods.get(name);
    }

    /**
     * Gets the type of the property with the given name.
     *
     * @param name the name of the property
     * @return the type of the property, or {@code null} if no such property exists
     */
    public Class<?> getType(final String name) {
        return types.get(name);
    }

    /**
     * Gets a transformer for the given primitive type.
     *
     * @param <R>  The transformer result type.
     * @param type the primitive type whose transformer to return
     * @return a transformer that will convert strings into that type, or null if the given type is not a primitive type
     */
    protected <R> Function<Object, R> getTypeTransformer(final Class<R> type) {
        return (Function<Object, R>) typeTransformers.get(type);
    }

    /**
     * Gets the mutator for the property with the given name.
     *
     * @param name the name of the
     * @return null if the name is null; null if the name is not a {@link String}; null if no such property exists; null if the property is read-only; or the
     *         mutator method for that property
     */
    protected Method getWriteMethod(final Object name) {
        return writeMethods.get(name);
    }

    /**
     * Gets the mutator for the property with the given name.
     *
     * @param name the name of the property
     * @return the mutator method for the property, or null
     */
    public Method getWriteMethod(final String name) {
        return writeMethods.get(name);
    }

    private void initialize() {
        if (getBean() == null) {
            return;
        }

        final Class<? extends Object> beanClass = getBean().getClass();
        try {
            // BeanInfo beanInfo = Introspector.getBeanInfo( bean, null );
            final BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            if (propertyDescriptors != null) {
                for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (propertyDescriptor != null) {
                        final String name = propertyDescriptor.getName();
                        final Method readMethod = propertyDescriptor.getReadMethod();
                        final Method writeMethod = propertyDescriptor.getWriteMethod();
                        final Class<? extends Object> aType = propertyDescriptor.getPropertyType();

                        if (readMethod != null) {
                            readMethods.put(name, readMethod);
                        }
                        if (writeMethod != null) {
                            writeMethods.put(name, writeMethod);
                        }
                        types.put(name, aType);
                    }
                }
            }
        } catch (final IntrospectionException e) {
            logWarn(e);
        }
    }

    /**
     * Convenience method for getting an iterator over the keys.
     * <p>
     * Write-only properties will not be returned in the iterator.
     * </p>
     *
     * @return an iterator over the keys
     */
    public Iterator<String> keyIterator() {
        return readMethods.keySet().iterator();
    }

    // Implementation methods

    /**
     * Gets the keys for this BeanMap.
     * <p>
     * Write-only properties are <strong>not</strong> included in the returned set of property names, although it is possible to set their value and to get
     * their type.
     * </p>
     *
     * @return BeanMap keys. The Set returned by this method is not modifiable.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    // The set actually contains strings; however, because it cannot be
    // modified there is no danger in selling it as Set<Object>
    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet((Set) readMethods.keySet());
    }

    /**
     * Logs the given exception to {@code System.out}. Used to display warnings while accessing/mutating the bean.
     *
     * @param ex the exception to log
     */
    protected void logInfo(final Exception ex) {
        // Deliberately do not use LOG4J or Commons Logging to avoid dependencies
        System.out.println("INFO: Exception: " + ex);
    }

    /**
     * Logs the given exception to {@code System.err}. Used to display errors while accessing/mutating the bean.
     *
     * @param ex the exception to log
     */
    protected void logWarn(final Exception ex) {
        // Deliberately do not use LOG4J or Commons Logging to avoid dependencies
        System.out.println("WARN: Exception: " + ex);
        ex.printStackTrace();
    }

    /**
     * Sets the bean property with the given name to the given value.
     *
     * @param name  the name of the property to set
     * @param value the value to set that property to
     * @return the previous value of that property
     * @throws IllegalArgumentException if the given name is null; if the given name is not a {@link String}; if the bean doesn't define a property with that
     *                                  name; or if the bean property with that name is read-only
     * @throws ClassCastException       if an error occurs creating the method args
     */
    @Override
    public Object put(final String name, final Object value) throws IllegalArgumentException, ClassCastException {
        if (bean != null) {
            final Object oldValue = get(name);
            final Method method = getWriteMethod(name);
            if (method == null) {
                throw new IllegalArgumentException("The bean of type: " + bean.getClass().getName() + " has no property called: " + name);
            }
            try {
                final Object[] arguments = createWriteMethodArguments(method, value);
                method.invoke(bean, arguments);

                final Object newValue = get(name);
                firePropertyChange(name, oldValue, newValue);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return oldValue;
        }
        return null;
    }

    /**
     * Puts all of the writable properties from the given BeanMap into this BeanMap. Read-only and Write-only properties will be ignored.
     *
     * @param map the BeanMap whose properties to put
     */
    public void putAllWriteable(final BeanMap map) {
        map.readMethods.keySet().forEach(key -> {
            if (getWriteMethod(key) != null) {
                put(key, map.get(key));
            }
        });
    }

    // Implementation classes

    /**
     * Reinitializes this bean. Called during {@link #setBean(Object)}. Does introspection to find properties.
     */
    protected void reinitialise() {
        readMethods.clear();
        writeMethods.clear();
        types.clear();
        initialize();
    }

    /**
     * Sets the bean to be operated on by this map. The given value may be null, in which case this map will be empty.
     *
     * @param newBean the new bean to operate on
     */
    public void setBean(final Object newBean) {
        bean = newBean;
        reinitialise();
    }

    /**
     * Returns the number of properties defined by the bean.
     *
     * @return the number of properties defined by the bean
     */
    @Override
    public int size() {
        return readMethods.size();
    }

    /**
     * Renders a string representation of this object.
     *
     * @return a {@code String} representation of this object
     */
    @Override
    public String toString() {
        return "BeanMap<" + bean + ">";
    }

    /**
     * Convenience method for getting an iterator over the values.
     *
     * @return an iterator over the values
     */
    public Iterator<Object> valueIterator() {
        final Iterator<?> iter = keyIterator();
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                final Object key = iter.next();
                return get(key);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove() not supported for BeanMap");
            }
        };
    }

    /**
     * Gets the values for the BeanMap.
     *
     * @return values for the BeanMap. The returned collection is not modifiable.
     */
    @Override
    public Collection<Object> values() {
        final ArrayList<Object> answer = new ArrayList<>(readMethods.size());
        valueIterator().forEachRemaining(answer::add);
        return Collections.unmodifiableList(answer);
    }
}
