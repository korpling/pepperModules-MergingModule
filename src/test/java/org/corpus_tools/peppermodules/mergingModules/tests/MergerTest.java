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

import org.corpus_tools.pepper.testFramework.PepperManipulatorTest;
import org.corpus_tools.peppermodules.mergingModules.Merger;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.junit.Before;
import org.junit.Test;

public class MergerTest extends PepperManipulatorTest {

	@Before
	public void beforeEach() {
		setFixture(new Merger());
		getFixture().setSaltProject(SaltFactory.createSaltProject());
	}

	/**
	 * Tests the merging on level {@link MERGING_LEVEL#MERGE_DOCUMENTS}:
	 * 
	 * <pre>
	 *   c1    |    c1      |    c1      
	 *   |     |   /  \     |   /  \     
	 *   c2    |  c2   c3   |  c2   c3   
	 *   |     | /  \   |   | /  \   |   
	 *   d1    |d1  d2  d3  |d1  d2  d3
	 * </pre>
	 * 
	 * result (autodetect):
	 * 
	 * <pre>
	 *     c1
	 *    /  \     
	 *   c2   c3   
	 *  /  \   |   
	 * d1  d2  d3
	 * </pre>
	 */
	@Test
	public void test_CorpusStructure_1() {

		SCorpusGraph graph1 = SaltFactory.createSCorpusGraph();
		SCorpus c1_test = SaltFactory.createSCorpus();
		{
			SCorpus c2 = SaltFactory.createSCorpus();
			SDocument d1 = SaltFactory.createSDocument();
			d1.setDocumentGraph(SaltFactory.createSDocumentGraph());

			c1_test.setName("c1");
			c2.setName("c2");
			d1.setName("d1");

			graph1.addNode(c1_test);
			graph1.addSubCorpus(c1_test, c2);
			graph1.addDocument(c2, d1);
		}

		SCorpusGraph graph2 = SaltFactory.createSCorpusGraph();
		{
			SDocument d1 = SaltFactory.createSDocument();
			d1.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SDocument d2 = SaltFactory.createSDocument();
			d2.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SDocument d3 = SaltFactory.createSDocument();
			d3.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SCorpus c1 = SaltFactory.createSCorpus();
			SCorpus c2 = SaltFactory.createSCorpus();
			SCorpus c3 = SaltFactory.createSCorpus();

			c1.setName("c1");
			c2.setName("c2");
			c3.setName("c3");
			d1.setName("d1");
			d2.setName("d2");
			d3.setName("d3");

			graph2.addNode(c1);
			graph2.addSubCorpus(c1, c2);
			graph2.addSubCorpus(c1, c3);
			graph2.addDocument(c2, d1);
			graph2.addDocument(c2, d2);
			graph2.addDocument(c3, d3);
		}

		SCorpusGraph graph3 = SaltFactory.createSCorpusGraph();
		{
			SDocument d1 = SaltFactory.createSDocument();
			d1.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SDocument d2 = SaltFactory.createSDocument();
			d2.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SDocument d3 = SaltFactory.createSDocument();
			d3.setDocumentGraph(SaltFactory.createSDocumentGraph());
			SCorpus c1 = SaltFactory.createSCorpus();
			SCorpus c2 = SaltFactory.createSCorpus();
			SCorpus c3 = SaltFactory.createSCorpus();

			c1.setName("c1");
			c2.setName("c2");
			c3.setName("c3");
			d1.setName("d1");
			d2.setName("d2");
			d3.setName("d3");

			graph3.addNode(c1);
			graph3.addSubCorpus(c1, c3);
			graph3.addSubCorpus(c1, c2);
			graph3.addDocument(c2, d1);
			graph3.addDocument(c2, d2);
			graph3.addDocument(c3, d3);
		}

		getFixture().getSaltProject().addCorpusGraph(graph1);
		getFixture().getSaltProject().addCorpusGraph(graph2);
		getFixture().getSaltProject().addCorpusGraph(graph3);

		this.start();
		/**
		 * <pre>
		 *     c1 
		 *    / \ 
		 *  c2   c3 
		 *  / \   | 
		 * d1 d2 d3
		 * </pre>
		 */
		assertEquals(3, graph1.getCorpora().size());
		assertEquals(3, graph1.getDocuments().size());
		assertEquals(2, graph1.getCorpusRelations().size());
		assertEquals(3, graph1.getCorpusDocumentRelations().size());

		assertEquals(1, graph1.getRoots().size());
		assertEquals(2, graph1.getOutRelations(c1_test.getId()).size());
	}

	/**
	 * Tests the merging on level {@link MERGING_LEVEL#MERGE_DOCUMENTS}:
	 * 
	 * <pre>
	 *   |    c1      |    c1       |  c1          
	 *   |   /  \     |   /  \      |   |          
	 *   |  c2   c3   |  c2   c3    |  c2 
	 *   | /  \   |   | /  \   |    |   |
	 *   |d1  d2  d3  |d1  d2  d3   |  d1
	 * </pre>
	 * 
	 * result (autodetect):
	 * 
	 * <pre>
	 *     c1
	 *    /  \     
	 *   c2   c3   
	 *  /  \   |   
	 * d1  d2  d3
	 * </pre>
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void test_CorpusStructure_2() throws InterruptedException {
		// graph 1
		SCorpusGraph graph1 = SaltFactory.createSCorpusGraph();
		SCorpus c1_1 = SaltFactory.createSCorpus();
		SCorpus c2_1 = SaltFactory.createSCorpus();
		SCorpus c3_1 = SaltFactory.createSCorpus();
		SDocument d1_1 = SaltFactory.createSDocument();
		SDocument d2_1 = SaltFactory.createSDocument();
		SDocument d3_1 = SaltFactory.createSDocument();
		c1_1.setName("c1");
		c2_1.setName("c2");
		c3_1.setName("c3");
		d1_1.setName("d1");
		d2_1.setName("d2");
		d3_1.setName("d3");
		c1_1.createMetaAnnotation(null, "anno1", "someValue");
		c2_1.createMetaAnnotation(null, "anno1", "someValue");
		c3_1.createMetaAnnotation(null, "anno1", "someValue");
		d1_1.createMetaAnnotation(null, "anno1", "someValue");
		d2_1.createMetaAnnotation(null, "anno1", "someValue");
		d3_1.createMetaAnnotation(null, "anno1", "someValue");
		graph1.addNode(c1_1);
		graph1.addSubCorpus(c1_1, c2_1);
		graph1.addSubCorpus(c1_1, c3_1);
		graph1.addDocument(c2_1, d1_1);
		graph1.addDocument(c2_1, d2_1);
		graph1.addDocument(c3_1, d3_1);
		getFixture().getSaltProject().addCorpusGraph(graph1);

		// graph2
		SCorpusGraph graph2 = SaltFactory.createSCorpusGraph();
		SCorpus c1_2 = SaltFactory.createSCorpus();
		SCorpus c2_2 = SaltFactory.createSCorpus();
		SCorpus c3_2 = SaltFactory.createSCorpus();
		SDocument d1_2 = SaltFactory.createSDocument();
		SDocument d2_2 = SaltFactory.createSDocument();
		SDocument d3_2 = SaltFactory.createSDocument();
		c1_2.setName("c1");
		c2_2.setName("c2");
		d1_2.setName("d1");
		d2_2.setName("d2");
		c3_2.setName("c3");
		d3_2.setName("d3");
		c1_2.createMetaAnnotation(null, "anno2", "someValue");
		c2_2.createMetaAnnotation(null, "anno2", "someValue");
		d2_2.createMetaAnnotation(null, "anno2", "someValue");
		d1_2.createMetaAnnotation(null, "anno2", "someValue");
		c3_2.createMetaAnnotation(null, "anno2", "someValue");
		d3_2.createMetaAnnotation(null, "anno2", "someValue");
		graph2.addNode(c1_2);
		graph2.addSubCorpus(c1_2, c2_2);
		graph2.addSubCorpus(c1_2, c3_2);
		graph2.addDocument(c2_2, d1_2);
		graph2.addDocument(c2_2, d2_2);
		graph2.addDocument(c3_2, d3_2);
		getFixture().getSaltProject().addCorpusGraph(graph2);

		// graph3
		SCorpusGraph graph3 = SaltFactory.createSCorpusGraph();
		SCorpus c1_3 = SaltFactory.createSCorpus();
		SCorpus c2_3 = SaltFactory.createSCorpus();
		SDocument d1_3 = SaltFactory.createSDocument();
		c1_3.setName("c1");
		c2_3.setName("c2");
		d1_3.setName("d1");
		c1_3.createMetaAnnotation(null, "anno3", "someValue");
		c2_3.createMetaAnnotation(null, "anno3", "someValue");
		d1_3.createMetaAnnotation(null, "anno3", "someValue");
		graph3.addNode(c1_3);
		graph3.addSubCorpus(c1_3, c2_3);
		graph3.addDocument(c2_3, d1_3);
		getFixture().getSaltProject().addCorpusGraph(graph3);

		this.start();

		/**
		 * <pre>
		 *     c1 
		 *    / \ 
		 *   c2 c3 
		 *  / \ | 
		 * d1 d2 d3
		 * </pre>
		 */
		assertEquals(3, graph1.getCorpora().size());
		assertEquals(3, graph1.getDocuments().size());
		assertEquals(2, graph1.getCorpusRelations().size());
		assertEquals(3, graph1.getCorpusDocumentRelations().size());

		assertEquals("all meta-annotations: " + c1_1.getMetaAnnotations(), 3, c1_1.getMetaAnnotations().size());
		assertEquals("all meta-annotations: " + c2_1.getMetaAnnotations(), 3, c2_1.getMetaAnnotations().size());
		assertEquals("all meta-annotations: " + c3_1.getMetaAnnotations(), 2, c3_1.getMetaAnnotations().size());
		assertEquals("all meta-annotations: " + d1_1.getMetaAnnotations(), 3, d1_1.getMetaAnnotations().size());
		assertEquals("all meta-annotations: " + d2_1.getMetaAnnotations(), 2, d2_1.getMetaAnnotations().size());
		assertEquals("all meta-annotations: " + d3_1.getMetaAnnotations(), 2, d3_1.getMetaAnnotations().size());
	}
}
