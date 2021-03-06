/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.hadoop;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.SecretKey;
import org.junit.Before;
import org.junit.Test;

public final class KeyMaterialsTest {

    private static final String KEY_ALG = "AES";
    private static final int KEY_SIZE = 128;
    private static final int IV_SIZE = 16;

    private KeyMaterial keyMaterial;
    private KeyPair keyPair;

    @Before
    public void before() throws NoSuchAlgorithmException, NoSuchProviderException {
        keyPair = KeyPairs.generateKeyPair();
        SecretKey secretKey = KeyMaterials.generateKey(KEY_ALG, KEY_SIZE);
        byte[] iv = KeyMaterials.generateIv(IV_SIZE);
        keyMaterial = KeyMaterial.of(secretKey, iv);
    }

    @Test
    public void testGenerateKey() {
        SecretKey secretKey = KeyMaterials.generateKey(KEY_ALG, KEY_SIZE);
        assertThat(secretKey.getAlgorithm(), is(KEY_ALG));
    }

    @Test
    public void testGenerateIv() {
        byte[] iv = KeyMaterials.generateIv(IV_SIZE);
        assertThat(iv.length, is(IV_SIZE));
    }

    @Test
    public void testWrapAndUnwrap() {
        byte[] wrapped = KeyMaterials.wrap(keyMaterial, keyPair.getPublic());
        KeyMaterial unwrapped = KeyMaterials.unwrap(wrapped, keyPair.getPrivate());
        assertThat(unwrapped, is(keyMaterial));
    }

    @Test
    public void testUnwrap_wrongVersion() {
        byte[] wrapped = KeyMaterials.wrap(keyMaterial, keyPair.getPublic());
        wrapped[0] = 0x02;

        try {
            KeyMaterials.unwrap(wrapped, keyPair.getPrivate());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Invalid KeyMaterials format version. Expected 1 but found 2"));
        }
    }

    @Test
    public void testSerializeDeserialize() {
        SerializableKeyMaterial skm = KeyMaterials.serialize(keyMaterial);
        assertThat(KeyMaterials.deserialize(skm), is(keyMaterial));
    }

}
