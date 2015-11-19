/**
 * Copyright 2015 Humboldt-Universität zu Berlin.
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
package org.corpus_tools.peppermodules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.corpus_tools.peppermodules.mergingModules.Merger;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.Identifier;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Florian Zipser
 *
 */
public class Merger_internalTest extends Merger {

	private Merger fixture = null;

	public Merger getFixture() {
		return fixture;
	}

	public void setFixture(Merger fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		setFixture(this);
		getFixture().setSaltProject(SaltFactory.createSaltProject());
	}

	/**
	 * Tests the merging on level {@link MERGING_LEVEL#MERGE_DOCUMENT_PATHS}:
	 * 
	 * <pre>
	 *  c1    |    c1      |    c1      
	 *  |     |   /  \     |   /  \     
	 *  d1    |  c2   c3   |  c2   c3   
	 *        | /  \   |   | /  \   |   
	 *        |d1  d2  d3  |d1  d2  d3
	 * </pre>
	 */
	@Test
	public void testProposeImportOrder() {
		SCorpusGraph graph1 = SaltFactory.createSCorpusGraph();
		SCorpus c1 = SaltFactory.createSCorpus();
		c1.setName("c1");
		graph1.addNode(c1);
		SDocument d1 = SaltFactory.createSDocument();
		d1.setName("d1");
		graph1.addDocument(c1, d1);

		SCorpusGraph graph2 = SaltFactory.createSCorpusGraph();
		SCorpus c1_2 = SaltFactory.createSCorpus();
		c1_2.setName("c1");
		graph2.addNode(c1_2);
		SCorpus c2_2 = SaltFactory.createSCorpus();
		c2_2.setName("c2");
		graph2.addSubCorpus(c1_2, c2_2);
		SDocument d1_2 = SaltFactory.createSDocument();
		d1_2.setName("d1");
		graph2.addDocument(c2_2, d1_2);
		SDocument d2_2 = SaltFactory.createSDocument();
		d2_2.setName("d2");
		graph2.addDocument(c2_2, d2_2);
		SCorpus c3_2 = SaltFactory.createSCorpus();
		c3_2.setName("c3");
		graph2.addSubCorpus(c1_2, c3_2);
		SDocument d3_2 = SaltFactory.createSDocument();
		d3_2.setName("d3");
		graph2.addDocument(c3_2, d3_2);

		SCorpusGraph graph3 = SaltFactory.createSCorpusGraph();
		SCorpus c1_3 = SaltFactory.createSCorpus();
		c1_3.setName("c1");
		graph3.addNode(c1_3);
		SCorpus c2_3 = SaltFactory.createSCorpus();
		c2_3.setName("c2");
		graph3.addSubCorpus(c1_3, c2_3);
		SDocument d1_3 = SaltFactory.createSDocument();
		d1_3.setName("d1");
		graph3.addDocument(c2_3, d1_3);
		SDocument d2_3 = SaltFactory.createSDocument();
		d2_3.setName("d2");
		graph3.addDocument(c2_3, d2_3);
		SCorpus c3_3 = SaltFactory.createSCorpus();
		c3_3.setName("c3");
		graph3.addSubCorpus(c1_3, c3_3);
		SDocument d3_3 = SaltFactory.createSDocument();
		d3_3.setName("d3");
		graph3.addDocument(c3_3, d3_3);

		getFixture().getSaltProject().addCorpusGraph(graph1);
		getFixture().getSaltProject().addCorpusGraph(graph2);
		getFixture().getSaltProject().addCorpusGraph(graph3);

		
		List<Identifier> importOrder1 = this.proposeImportOrder(graph1);
        List<Identifier> importOrder2 = this.proposeImportOrder(graph2);
		List<Identifier> importOrder3 = this.proposeImportOrder(graph3);
		
        assertNotNull(importOrder1);
		assertNotNull(importOrder2);
        assertNotNull(importOrder3);
        
		assertEquals(1, importOrder1.size());
		assertEquals(3, importOrder2.size());
		assertEquals(3, importOrder3.size());
        
        // check that there are the documents with the same ID at the same
        // index at each import
        
        assertEquals(d1.getIdentifier(), importOrder1.get(0));
		assertEquals(d1_2.getIdentifier(), importOrder2.get(0));
		assertEquals(d1_3.getIdentifier(), importOrder3.get(0));

		assertEquals(d2_2.getIdentifier(), importOrder2.get(1));
   		assertEquals(d2_3.getIdentifier(), importOrder3.get(1));

		assertEquals(d3_2.getIdentifier(), importOrder2.get(2));
		assertEquals(d3_3.getIdentifier(), importOrder3.get(2));
	}
}
