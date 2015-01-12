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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.Merger;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotatableElement;

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
		getFixture().setSaltProject(SaltFactory.eINSTANCE.createSaltProject());
	}

	/**
	 * Tests method
	 * {@link #moveSMetaAnnotations(de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotatableElement, de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotatableElement)}
	 */
	@Test
	public void testMoveSMetaAnnotations() {
		SMetaAnnotatableElement source = SaltFactory.eINSTANCE.createSMetaAnnotatableElement();
		source.createSMetaAnnotation("ns", "meta1", "whatever");
		source.createSMetaAnnotation(null, "meta2", "whatever");

		SMetaAnnotatableElement target = SaltFactory.eINSTANCE.createSMetaAnnotatableElement();
		target.createSMetaAnnotation("ns", "meta3", "whatever");

		moveSMetaAnnotations(source, target);

		assertEquals(3, target.getSMetaAnnotations().size());
		assertNotNull(target.getSMetaAnnotation("ns::meta1"));
		assertNotNull(target.getSMetaAnnotation("meta2"));
		assertNotNull(target.getSMetaAnnotation("ns::meta3"));

		source = SaltFactory.eINSTANCE.createSMetaAnnotatableElement();
		source.createSMetaAnnotation("ns", "meta1", "whatever");
		moveSMetaAnnotations(source, target);
		assertEquals(3, target.getSMetaAnnotations().size());
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
		SCorpusGraph graph1 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1 = SaltFactory.eINSTANCE.createSCorpus();
		c1.setSName("c1");
		graph1.addSNode(c1);
		SDocument d1 = SaltFactory.eINSTANCE.createSDocument();
		d1.setSName("d1");
		graph1.addSDocument(c1, d1);

		SCorpusGraph graph2 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_2 = SaltFactory.eINSTANCE.createSCorpus();
		c1_2.setSName("c1");
		graph2.addSNode(c1_2);
		SCorpus c2_2 = SaltFactory.eINSTANCE.createSCorpus();
		c2_2.setSName("c2");
		graph2.addSSubCorpus(c1_2, c2_2);
		SDocument d1_2 = SaltFactory.eINSTANCE.createSDocument();
		d1_2.setSName("d1");
		graph2.addSDocument(c2_2, d1_2);
		SDocument d2_2 = SaltFactory.eINSTANCE.createSDocument();
		d2_2.setSName("d2");
		graph2.addSDocument(c2_2, d2_2);
		SCorpus c3_2 = SaltFactory.eINSTANCE.createSCorpus();
		c3_2.setSName("c3");
		graph2.addSSubCorpus(c1_2, c3_2);
		SDocument d3_2 = SaltFactory.eINSTANCE.createSDocument();
		d3_2.setSName("d3");
		graph2.addSDocument(c3_2, d3_2);

		SCorpusGraph graph3 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_3 = SaltFactory.eINSTANCE.createSCorpus();
		c1_3.setSName("c1");
		graph3.addSNode(c1_3);
		SCorpus c2_3 = SaltFactory.eINSTANCE.createSCorpus();
		c2_3.setSName("c2");
		graph3.addSSubCorpus(c1_3, c2_3);
		SDocument d1_3 = SaltFactory.eINSTANCE.createSDocument();
		d1_3.setSName("d1");
		graph3.addSDocument(c2_3, d1_3);
		SDocument d2_3 = SaltFactory.eINSTANCE.createSDocument();
		d2_3.setSName("d2");
		graph3.addSDocument(c2_3, d2_3);
		SCorpus c3_3 = SaltFactory.eINSTANCE.createSCorpus();
		c3_3.setSName("c3");
		graph3.addSSubCorpus(c1_3, c3_3);
		SDocument d3_3 = SaltFactory.eINSTANCE.createSDocument();
		d3_3.setSName("d3");
		graph3.addSDocument(c3_3, d3_3);

		getFixture().getSaltProject().getSCorpusGraphs().add(graph1);
		getFixture().getSaltProject().getSCorpusGraphs().add(graph2);
		getFixture().getSaltProject().getSCorpusGraphs().add(graph3);

		List<SElementId> importOrder;

		importOrder = this.proposeImportOrder(graph1);
		assertNotNull(importOrder);
		assertEquals(1, importOrder.size());
		assertEquals(d1.getSElementId(), importOrder.get(0));

		importOrder = this.proposeImportOrder(graph2);
		assertNotNull(importOrder);
		assertEquals(3, importOrder.size());
		assertEquals(d1_2.getSElementId(), importOrder.get(2));
		assertEquals(d2_2.getSElementId(), importOrder.get(1));
		assertEquals(d3_2.getSElementId(), importOrder.get(0));

		importOrder = this.proposeImportOrder(graph3);
		assertNotNull(importOrder);
		assertEquals(d1_3.getSElementId(), importOrder.get(2));
		assertEquals(d2_3.getSElementId(), importOrder.get(1));
		assertEquals(d3_3.getSElementId(), importOrder.get(0));
	}
}
