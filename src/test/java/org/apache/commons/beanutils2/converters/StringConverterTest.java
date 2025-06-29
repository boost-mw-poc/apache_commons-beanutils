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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@code StringConverter}.
 */
class StringConverterTest {
    /** The converter to be tested. */
    private StringConverter converter;

    @BeforeEach
    protected void setUp() throws Exception {
        converter = new StringConverter();
    }

    /**
     * Tests a conversion to a string type.
     */
    @Test
    void testConvertToTypeString() {
        final Object value = new Object();
        final String strVal = converter.convert(String.class, value);
        assertEquals(value.toString(), strVal, "Wrong conversion result");
    }

    /**
     * Tests whether the correct default type is returned.
     */
    @Test
    void testDefaultType() {
        assertEquals(String.class, converter.getDefaultType(), "Wrong default type");
    }
}
