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

import java.util.Objects;

import org.apache.commons.beanutils2.Converter;

/**
 * <p>
 * Provides a facade for {@link Converter} implementations preventing access to any public API in the implementation, other than that specified by
 * {@link Converter}.
 * <p>
 * This implementation can be used to prevent registered {@link Converter} implementations that provide configuration options from being retrieved and modified.
 * </p>
 *
 * @param <T> The converter type.
 * @since 1.8.0
 */
public final class ConverterFacade<T> implements Converter<T> {

    private final Converter<T> converter;

    /**
     * Constructs a converter which delegates to the specified {@link Converter} implementation.
     *
     * @param converter The converter to delegate to
     */
    public ConverterFacade(final Converter<T> converter) {
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    /**
     * Convert the input object into an output object of the specified type by delegating to the underlying {@link Converter} implementation.
     *
     * @param type  Data type to which this value should be converted
     * @param value The input value to be converted
     * @return The converted value.
     */
    @Override
    public <R> R convert(final Class<R> type, final Object value) {
        return converter.convert(type, value);
    }

    /**
     * Provide a String representation of this facade implementation sand the underlying {@link Converter} it delegates to.
     *
     * @return A String representation of this facade implementation sand the underlying {@link Converter} it delegates to
     */
    @Override
    public String toString() {
        return "ConverterFacade[" + converter.toString() + "]";
    }

}
