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
package org.apache.commons.beanutils2.converters;

/**
 * {@link org.apache.commons.beanutils2.Converter} implementation that handles conversion to and from <strong>Boolean</strong> objects.
 * {@link org.apache.commons.beanutils2.Converter} implementation that handles conversion to and from {@link Boolean} objects.
 * <p>
 * Can be configured to either return a <em>default value</em> or throw a {@code ConversionException} if a conversion error occurs.
 * </p>
 * <p>
 * By default any object whose string representation is one of the values {"yes", "y", "true", "on", "1"} is converted to Boolean.TRUE, and string
 * representations {"no", "n", "false", "off", "0"} are converted to Boolean.FALSE. The recognized true/false strings can be changed by:
 * </p>
 *
 * <pre>
 * String[] trueStrings = { "oui", "o", "1" };
 * String[] falseStrings = { "non", "n", "0" };
 * Converter bc = new BooleanConverter(trueStrings, falseStrings);
 * ConvertUtils.register(bc, Boolean.class);
 * ConvertUtils.register(bc, Boolean.TYPE);
 * </pre>
 *
 * <p>
 * Case is ignored when converting values to true or false.
 * </p>
 *
 * @since 1.3
 */
public final class BooleanConverter extends AbstractConverter<Boolean> {

    /**
     * Copies the provided array, and ensures that all the strings in the newly created array contain only lower-case letters.
     * <p>
     * Using this method to copy string arrays means that changes to the src array do not modify the dst array.
     * </p>
     */
    private static String[] copyStrings(final String[] src) {
        final String[] dst = new String[src.length];
        for (int i = 0; i < src.length; ++i) {
            dst[i] = toLowerCase(src[i]);
        }
        return dst;
    }

    /**
     * The set of strings that are known to map to Boolean.TRUE.
     */
    private String[] trueStrings = { "true", "yes", "y", "on", "1" };

    /**
     * The set of strings that are known to map to Boolean.FALSE.
     */
    private String[] falseStrings = { "false", "no", "n", "off", "0" };

    /**
     * Constructs a {@link org.apache.commons.beanutils2.Converter} that will throw a {@link org.apache.commons.beanutils2.ConversionException} if a conversion
     * error occurs, that is, if the string value being converted is not one of the known true strings, nor one of the known false strings.
     */
    public BooleanConverter() {
    }

    /**
     * Constructs a {@link org.apache.commons.beanutils2.Converter} that will return the specified default value if a conversion error occurs, that is, the
     * string value being converted is not one of the known true strings, nor one of the known false strings.
     *
     * @param defaultValue The default value to be returned if the value being converted is not recognized. This value may be null, in which case null will be
     *                     returned on conversion failure. When non-null, it is expected that this value will be either Boolean.TRUE or Boolean.FALSE. The
     *                     special value BooleanConverter.NO_DEFAULT can also be passed here, in which case this constructor acts like the no-argument one.
     */
    public BooleanConverter(final Boolean defaultValue) {
        super(defaultValue);
    }

    /**
     * Constructs a {@link org.apache.commons.beanutils2.Converter} that will throw a {@link org.apache.commons.beanutils2.ConversionException} if a conversion
     * error occurs, that is, the string value being converted is not one of the known true strings, nor one of the known false strings.
     * <p>
     * The provided string arrays are copied, so that changes to the elements of the array after this call is made do not affect this object.
     *
     * @param trueStrings  is the set of strings which should convert to the value Boolean.TRUE. The value null must not be present. Case is ignored.
     * @param falseStrings is the set of strings which should convert to the value Boolean.TRUE. The value null must not be present. Case is ignored.
     * @since 1.8.0
     */
    public BooleanConverter(final String[] trueStrings, final String[] falseStrings) {
        this.trueStrings = copyStrings(trueStrings);
        this.falseStrings = copyStrings(falseStrings);
    }

    /**
     * Constructs a {@link org.apache.commons.beanutils2.Converter} that will return the specified default value if a conversion error occurs.
     * <p>
     * The provided string arrays are copied, so that changes to the elements of the array after this call is made do not affect this object.
     *
     * @param trueStrings  is the set of strings which should convert to the value Boolean.TRUE. The value null must not be present. Case is ignored.
     * @param falseStrings is the set of strings which should convert to the value Boolean.TRUE. The value null must not be present. Case is ignored.
     * @param defaultValue The default value to be returned if the value being converted is not recognized. This value may be null, in which case null will be
     *                     returned on conversion failure. When non-null, it is expected that this value will be either Boolean.TRUE or Boolean.FALSE. The
     *                     special value BooleanConverter.NO_DEFAULT can also be passed here, in which case an exception will be thrown on conversion failure.
     * @since 1.8.0
     */
    public BooleanConverter(final String[] trueStrings, final String[] falseStrings, final Boolean defaultValue) {
        super(defaultValue);
        this.trueStrings = copyStrings(trueStrings);
        this.falseStrings = copyStrings(falseStrings);
    }

    /**
     * Converts the specified input object into an output object of the specified type.
     *
     * @param <T>   Target type of the conversion.
     * @param type  is the type to which this value should be converted. In the case of this BooleanConverter class, this value is ignored.
     * @param value is the input value to be converted. The toString method shall be invoked on this object, and the result compared (ignoring case) against the
     *              known "true" and "false" string values.
     *
     * @return Boolean.TRUE if the value was a recognized "true" value, Boolean.FALSE if the value was a recognized "false" value, or the default value if the
     *         value was not recognized and the constructor was provided with a default value.
     *
     * @throws Throwable if an error occurs converting to the specified type
     * @since 1.8.0
     */
    @Override
    protected <T> T convertToType(final Class<T> type, final Object value) throws Throwable {
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            // All the values in the trueStrings and falseStrings arrays are
            // guaranteed to be lower-case. By converting the input value
            // to lowercase too, we can use the efficient String.equals method
            // instead of the less-efficient String.equalsIgnoreCase method.
            final String stringValue = toLowerCase(value);

            for (final String trueString : trueStrings) {
                if (trueString.equals(stringValue)) {
                    return type.cast(Boolean.TRUE);
                }
            }

            for (final String falseString : falseStrings) {
                if (falseString.equals(stringValue)) {
                    return type.cast(Boolean.FALSE);
                }
            }
        }

        throw conversionException(type, value);
    }

    /**
     * Gets the default type this {@code Converter} handles.
     *
     * @return The default type this {@code Converter} handles.
     * @since 1.8.0
     */
    @Override
    protected Class<Boolean> getDefaultType() {
        return Boolean.class;
    }
}
