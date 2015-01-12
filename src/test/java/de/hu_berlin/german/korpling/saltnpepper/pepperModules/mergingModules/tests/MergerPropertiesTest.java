/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;

public class MergerPropertiesTest {

	private MergerProperties fixture = null;

	public MergerProperties getFixture() {
		return fixture;
	}

	public void setFixture(MergerProperties fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		setFixture(new MergerProperties());
	}

	@Test
	public void testGetPunctuations() {
		assertNotNull(getFixture().getPunctuations());
		char[] punctuations = { '.', ',', ':', ';', '!', '?', '(', ')', '{', '}', '<', '>' };
		assertEquals(punctuations.length, getFixture().getPunctuations().size());
		for (int i = 0; i < punctuations.length; i++) {
			assertTrue(getFixture().getPunctuations().contains(punctuations[i]));
		}
	}

	@Test
	public void testGetEscapeMapping() {
		assertNotNull(getFixture().getEscapeMapping());

		Hashtable<String, String> templateTable = new Hashtable<String, String>();
		templateTable.put(" ", "");
		templateTable.put("\t", "");
		templateTable.put("\n", "");
		templateTable.put("\r", "");

		templateTable.put("ä", "ae");
		templateTable.put("ö", "oe");
		templateTable.put("ü", "ue");
		templateTable.put("ß", "ss");
		templateTable.put("Ä", "Ae");
		templateTable.put("Ö", "Oe");
		templateTable.put("Ü", "Ue");

		assertEquals(templateTable.size(), getFixture().getEscapeMapping().size());
		for (String key : templateTable.keySet()) {
			assertTrue(getFixture().getEscapeMapping().containsKey(key));
			assertEquals(templateTable.get(key), getFixture().getEscapeMapping().get(key));
		}
	}
}
