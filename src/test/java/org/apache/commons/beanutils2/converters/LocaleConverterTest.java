/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.beanutils2.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LocaleConverter}.
 *
 * @since 2.0.0
 */
class LocaleConverterTest {

    private LocaleConverter converter;

    @BeforeEach
    public void before() {
        converter = new LocaleConverter();
    }

    @Test
    void testConvertCustomLocale() {
        final Locale expected = Locale.forLanguageTag("en-owo");
        final Locale actual = converter.convert(Locale.class, "en-owo");

        assertEquals(expected, actual);
    }

    @Test
    void testConvertStandardLocale() {
        final Locale expected = Locale.ENGLISH;
        final Locale actual = converter.convert(Locale.class, "en");

        assertEquals(expected, actual);
    }
}
