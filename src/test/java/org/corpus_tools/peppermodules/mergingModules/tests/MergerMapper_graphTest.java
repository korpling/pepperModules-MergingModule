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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.modules.MappingSubject;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.peppermodules.mergingModules.MergerMapper;
import org.corpus_tools.peppermodules.mergingModules.MergerProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.samples.SampleGenerator;
import org.corpus_tools.salt.util.Difference;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class MergerMapper_graphTest extends MergerMapper {

	private MergerMapper fixture = null;

	public MergerMapper getFixture() {
		return fixture;
	}

	public void setFixture(MergerMapper fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		setFixture(this);
		this.setProperties(new MergerProperties());
		this.initialize();
	}

	/**
	 * Tests the mapping of three documents containing the same primary data and
	 * same tokenization, but different annotation layers:
	 * <ol>
	 * <li>document1: anaphoric relations (pointing relations)</li>
	 * <li>document2: syntactic annotations</li>
	 * <li>document3: morphological annotations (POS and lemma)</li>
	 * </ol>
	 */
	@Test
	public void testMap3Documents_sameTokenization() {
		// set up empty documents

		// doc 1
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("sdoc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(sDoc1.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);
		SampleGenerator.createAnaphoricAnnotations(sDoc1);

		// doc 2
		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("sdoc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(sDoc2.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);
		SampleGenerator.createPrimaryData(sDoc2);
		SampleGenerator.createTokens(sDoc2);
		SampleGenerator.createSyntaxStructure(sDoc2);
		SampleGenerator.createSyntaxAnnotations(sDoc2);

		// doc 3
		SDocument sDoc3 = SaltFactory.createSDocument();
		sDoc3.setId("sdoc3");
		sDoc3.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub3 = new MappingSubject();
		sub3.setIdentifier(sDoc3.getIdentifier());
		getFixture().getMappingSubjects().add(sub3);
		SampleGenerator.createPrimaryData(sDoc3);
		SampleGenerator.createTokens(sDoc3);
		SampleGenerator.createMorphologyAnnotations(sDoc3);

		// template document contains all annotations
		SDocument template = SaltFactory.createSDocument();
		template.setId("template");
		template.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(template);
		SampleGenerator.createTokens(template);
		SampleGenerator.createSyntaxStructure(template);
		SampleGenerator.createAnaphoricAnnotations(template);
		SampleGenerator.createSyntaxAnnotations(template);
		SampleGenerator.createMorphologyAnnotations(template);

		this.isTestMode = true;

		this.mergeDocumentStructures(chooseBaseDocument());

		// second document must be the base document
		assertEquals(sDoc2, container.getBaseDocument());
		// the text of the first document must be the base text
		assertEquals(sDoc2.getDocumentGraph().getTextualDSs().get(0), container.getBaseDocument().getDocumentGraph().getTextualDSs().get(0));

		// the count of tokens in sDoc1 must be the same as before!
		assertEquals(template.getDocumentGraph().getTokens().size(), sDoc2.getDocumentGraph().getTokens().size());
		assertEquals(template.getDocumentGraph().getSpans().size(), sDoc2.getDocumentGraph().getSpans().size());
		assertEquals(template.getDocumentGraph().getSpanningRelations().size(), sDoc2.getDocumentGraph().getSpanningRelations().size());
		assertEquals(template.getDocumentGraph().getStructures().size(), sDoc2.getDocumentGraph().getStructures().size());
		assertEquals(template.getDocumentGraph().getDominanceRelations().size(), sDoc2.getDocumentGraph().getDominanceRelations().size());

		assertEquals(template.getDocumentGraph().getNodes().size(), sDoc2.getDocumentGraph().getNodes().size());
		assertEquals(template.getDocumentGraph().getRelations().size(), sDoc2.getDocumentGraph().getRelations().size());

		assertEquals(template.getDocumentGraph().getRoots().size(), sDoc2.getDocumentGraph().getRoots().size());

		assertNotNull(sDoc2.getDocumentGraph());
	}

	/**
	 * Tests one {@link SDocumentGraph} containing {@link SSpan}s and one
	 * {@link SDocumentGraph}, which does not. In the end, the one which does
	 * not should contain all spans, which are contained by the other
	 * {@link SDocumentGraph}.
	 */
	@Test
	public void testMergeSpans() {
		// set up empty documents
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("sdoc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(sDoc1.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("sdoc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(sDoc2.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);
		SampleGenerator.createPrimaryData(sDoc2);
		SampleGenerator.createTokens(sDoc2);
		SampleGenerator.createInformationStructureSpan(sDoc2);
		SampleGenerator.createInformationStructureAnnotations(sDoc2);

		// template document contains all annotations
		SDocument template = SaltFactory.createSDocument();
		template.setId("template");
		template.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(template);
		SampleGenerator.createTokens(template);
		SampleGenerator.createInformationStructureSpan(template);
		SampleGenerator.createInformationStructureAnnotations(template);

		assertEquals(0, sDoc1.getDocumentGraph().getSpans().size());
		assertEquals(0, sDoc1.getDocumentGraph().getSpanningRelations().size());

		this.isTestMode = true;
		this.mergeDocumentStructures(chooseBaseDocument());

		assertNotNull(sDoc2.getDocumentGraph());
		assertEquals(template.getDocumentGraph().getTokens().size(), sDoc2.getDocumentGraph().getTokens().size());
		assertEquals(template.getDocumentGraph().getSpans().size(), sDoc2.getDocumentGraph().getSpans().size());
		assertEquals(template.getDocumentGraph().getSpanningRelations().size(), sDoc2.getDocumentGraph().getSpanningRelations().size());

		assertEquals(template.getDocumentGraph().getNodes().size(), sDoc2.getDocumentGraph().getNodes().size());
		assertEquals(template.getDocumentGraph().getRelations().size(), sDoc2.getDocumentGraph().getRelations().size());
	}

	/**
	 * Tests two {@link SDocumentGraph}s containing {@link SSpan}s. Two equal 
	 * spans, one contains annotations, the other one does not. In the end, both
	 * shall have the same annotations.
	 */
	@Test
	public void testMergeSpans2() {
		SDocument fixture = SaltFactory.createSDocument();
		fixture.setId("sdoc1");
		fixture.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(fixture.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);
		SampleGenerator.createPrimaryData(fixture);
		SampleGenerator.createTokens(fixture);
		SampleGenerator.createInformationStructureSpan(fixture);

		SDocument biggerDoc = SaltFactory.createSDocument();
		biggerDoc.setId("sdoc2");
		biggerDoc.setDocumentGraph(SaltFactory.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(biggerDoc.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);
		SampleGenerator.createPrimaryData(biggerDoc);
		SampleGenerator.createTokens(biggerDoc);
		SampleGenerator.createInformationStructureSpan(biggerDoc);
		SampleGenerator.createInformationStructureAnnotations(biggerDoc);

		this.isTestMode = true;
		this.mergeDocumentStructures(chooseBaseDocument());
		
		// template document contains all annotations
		SDocument template = SaltFactory.createSDocument();
		template.setId("template");
		template.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(template);
		SampleGenerator.createTokens(template);
		SampleGenerator.createInformationStructureSpan(template);
		SampleGenerator.createInformationStructureAnnotations(template);

		Set<Difference> diffs= template.getDocumentGraph().findDiffs(fixture.getDocumentGraph());
		assertEquals(diffs+"", 0, diffs.size());
	}

	/**
	 * Tests the document status after the mapping of three documents containing
	 * the same primary data and same tokenization, but different annotation
	 * layers:
	 * <ol>
	 * <li>document1: anaphoric relations (pointing relations)</li>
	 * <li>document2: syntactic annotations</li>
	 * <li>document3: morphological annotations (POS and lemma)</li>
	 * </ol>
	 */
	@Test
	public void testMap3Documents_sameTokenizationDocumentStatus() {
		// set up empty documents
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("sdoc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("sdoc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());

		SDocument sDoc3 = SaltFactory.createSDocument();
		sDoc3.setId("sdoc3");
		sDoc3.setDocumentGraph(SaltFactory.createSDocumentGraph());

		MappingSubject sub1 = new MappingSubject();
		MappingSubject sub2 = new MappingSubject();
		MappingSubject sub3 = new MappingSubject();

		sub1.setIdentifier(sDoc1.getIdentifier());
		sub2.setIdentifier(sDoc2.getIdentifier());
		sub3.setIdentifier(sDoc3.getIdentifier());

		getFixture().getMappingSubjects().add(sub1);
		getFixture().getMappingSubjects().add(sub2);
		getFixture().getMappingSubjects().add(sub3);

		// document data
		// doc 1
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);
		SampleGenerator.createAnaphoricAnnotations(sDoc1);

		// doc 2
		SampleGenerator.createPrimaryData(sDoc2);
		SampleGenerator.createTokens(sDoc2);
		SampleGenerator.createSyntaxStructure(sDoc2);
		SampleGenerator.createSyntaxAnnotations(sDoc2);

		// doc 3
		SampleGenerator.createPrimaryData(sDoc3);
		SampleGenerator.createTokens(sDoc3);
		SampleGenerator.createMorphologyAnnotations(sDoc3);

		// template document contains all annotations
		SDocument template = SaltFactory.createSDocument();
		template.setId("template");
		template.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(template);
		SampleGenerator.createTokens(template);
		SampleGenerator.createSyntaxStructure(template);
		SampleGenerator.createAnaphoricAnnotations(template);
		SampleGenerator.createSyntaxAnnotations(template);
		SampleGenerator.createMorphologyAnnotations(template);

		this.isTestMode = false;
		mapSDocument();

		assertEquals(DOCUMENT_STATUS.COMPLETED, sub2.getMappingResult());
		assertEquals(DOCUMENT_STATUS.DELETED, sub1.getMappingResult());
		assertEquals(DOCUMENT_STATUS.DELETED, sub3.getMappingResult());
	}

	/**
	 * Tests the method
	 * {@link #mergeTokens(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)}
	 * and checks if correct equivalence classes for an artificial test case are
	 * created. <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * <li>{@value SampleGenerator#PRIMARY_TEXT_EN}</li>
	 * <li>Well. {@value SampleGenerator#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol>
	 */
	@Test
	public void testMergeTokens_case1() {
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("doc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc1);

		SLayer morphLayer = SaltFactory.createSLayer();
		morphLayer.setName("morphology");
		sDoc1.getDocumentGraph().addLayer(morphLayer);
		SampleGenerator.createToken(0, 2, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // Is
		SampleGenerator.createToken(3, 7, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // this
		SampleGenerator.createToken(8, 15, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // example
		SampleGenerator.createToken(16, 20, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // more
		SampleGenerator.createToken(21, 32, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // complicated
		SampleGenerator.createToken(33, 37, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // than
		SampleGenerator.createToken(38, 40, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // it
		SampleGenerator.createToken(41, 48, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // supposed
		SampleGenerator.createToken(49, 51, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // to
		// SampleGenerator.createToken(52,55,sDoc1.getDocumentGraph().getTextualDSs().get(0),sDoc1,morphLayer);
		// //be?
		SampleGenerator.createToken(52, 54, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // be
		SampleGenerator.createToken(54, 55, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // ?

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("doc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		sDoc2.getDocumentGraph().createTextualDS("Well. " + SampleGenerator.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getDocumentGraph().tokenize();

		List<SToken> baseTextToken = sDoc2.getDocumentGraph().getSortedTokenByText();
		List<SToken> otherTextToken = sDoc1.getDocumentGraph().getSortedTokenByText();

		this.normalizePrimaryTexts(sDoc1);
		this.normalizePrimaryTexts(sDoc2);

		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc2.getDocumentGraph().getTextualDSs().get(0), sDoc1.getDocumentGraph().getTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));
		assertEquals(otherTextToken.size(), equivalenceMap.size());

		// test 3 : the token alignment is correct : the equivalence classes are
		// correct
		int j = 0;
		for (int i = 2; i < 11; i++) {
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getDocumentGraph().getTextualDSs().get(0);
			assertEquals("Base Token " + base.getName() + "  and other token " + otherToken.getName() + " (start: " + this.container.getAlignedTokenStart(otherText, otherToken) + ") should be equal.", this.container.getTokenMapping(base, otherText), otherToken);
			j++;
		}

		int equivalenceMapSize = equivalenceMap.size();
		// assert that the merging did not change something
		this.mergeTokens(sDoc2.getDocumentGraph().getTextualDSs().get(0), sDoc1.getDocumentGraph().getTextualDSs().get(0), equivalenceMap);
		assertEquals(equivalenceMapSize, equivalenceMap.size());

		j = 0;
		for (int i = 2; i < 11; i++) {
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getDocumentGraph().getTextualDSs().get(0);
			assertEquals("Base Token " + base.getName() + ") and other token " + otherToken.getName() + " (start: " + this.container.getAlignedTokenStart(otherText, otherToken) + ") should be equal.", this.container.getTokenMapping(base, otherText), otherToken);
			j++;
		}
	}

	/**
	 * Tests the method
	 * {@link #mergeTokens(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)}
	 * and checks if correct equivalence classes for an artificial test case are
	 * created. <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * <li>{@value SampleGenerator#PRIMARY_TEXT_EN}</li>
	 * <li>Well. {@value SampleGenerator#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol>
	 * 
	 * In this test, the first text is used as base text and one token of the
	 * first text is removed
	 */
	@Test
	public void testMergeTokens_case2() {
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("doc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc1);

		SLayer morphLayer = SaltFactory.createSLayer();
		morphLayer.setName("morphology");
		sDoc1.getDocumentGraph().addLayer(morphLayer);
		SampleGenerator.createToken(0, 2, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // Is
		SampleGenerator.createToken(3, 7, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // this
		SampleGenerator.createToken(8, 15, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // example
		SampleGenerator.createToken(16, 20, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // more
		// SampleGenerator.createToken(21,32,sDoc1.getDocumentGraph().getTextualDSs().get(0),sDoc1,morphLayer);
		// //complicated
		SampleGenerator.createToken(33, 37, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // than
		SampleGenerator.createToken(38, 40, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // it
		SampleGenerator.createToken(41, 48, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // supposed
		SampleGenerator.createToken(49, 51, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // to
		// SampleGenerator.createToken(52,55,sDoc1.getDocumentGraph().getTextualDSs().get(0),sDoc1,morphLayer);
		// //be?
		SampleGenerator.createToken(52, 54, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // be
		SampleGenerator.createToken(54, 55, sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc1, morphLayer); // ?

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("doc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		sDoc2.getDocumentGraph().createTextualDS("Well. " + SampleGenerator.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getDocumentGraph().tokenize();

		List<SToken> baseTextToken = sDoc1.getDocumentGraph().getSortedTokenByText();
		List<SToken> otherTextToken = sDoc2.getDocumentGraph().getSortedTokenByText();

		this.normalizePrimaryTexts(sDoc1);
		this.normalizePrimaryTexts(sDoc2);

		// test 1 : sDoc2 must be the base document
		this.container.setBaseDocument(sDoc1);

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc2.getDocumentGraph().getTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));
		assertEquals(baseTextToken.size(), equivalenceMap.size());

		// the token alignment is correct : the equivalence classes are correct
		Hashtable<SNode, SNode> templateMap = new Hashtable<SNode, SNode>();
		templateMap.put(otherTextToken.get(2), baseTextToken.get(0));
		templateMap.put(otherTextToken.get(3), baseTextToken.get(1));
		templateMap.put(otherTextToken.get(4), baseTextToken.get(2));
		templateMap.put(otherTextToken.get(5), baseTextToken.get(3));
		templateMap.put(otherTextToken.get(7), baseTextToken.get(4));
		templateMap.put(otherTextToken.get(8), baseTextToken.get(5));
		templateMap.put(otherTextToken.get(9), baseTextToken.get(6));
		templateMap.put(otherTextToken.get(10), baseTextToken.get(7));
		templateMap.put(otherTextToken.get(11), baseTextToken.get(8));
		templateMap.put(otherTextToken.get(12), baseTextToken.get(9));

		assertEquals(templateMap, equivalenceMap);

		int equivalenceMapSize = equivalenceMap.size();
		// assert that the merging did not change something
		this.mergeTokens(sDoc1.getDocumentGraph().getTextualDSs().get(0), sDoc2.getDocumentGraph().getTextualDSs().get(0), equivalenceMap);
		assertTrue(equivalenceMapSize != equivalenceMap.size());
		assertEquals(baseTextToken.size() + 1, equivalenceMap.size());
		assertNotNull(equivalenceMap.get(otherTextToken.get(6)));
	}

	/**
	 * Merges 2 documents by merging texts and tokens, but not spans and
	 * structures, they should be copied.
	 */
	@Test
	public void testCopyNodes() {
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("doc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);
		SampleGenerator.createInformationStructureSpan(sDoc1);
		SampleGenerator.createSyntaxStructure(sDoc1);

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("doc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc2);
		SampleGenerator.createTokens(sDoc2);
		SampleGenerator.createInformationStructureSpan(sDoc2);
		SampleGenerator.createSyntaxStructure(sDoc2);

		int tokens = sDoc1.getDocumentGraph().getTokens().size();
		int spans = sDoc1.getDocumentGraph().getSpans().size() + sDoc2.getDocumentGraph().getSpans().size();
		int structs = sDoc1.getDocumentGraph().getStructures().size() + sDoc2.getDocumentGraph().getStructures().size();

		// create mapping subjects for documents
		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(sDoc1.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);

		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(sDoc2.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);

		MergerProperties props = new MergerProperties();
		PepperModuleProperty<Boolean> prop = (PepperModuleProperty<Boolean>) props.getProperty(MergerProperties.PROP_COPY_NODES);
		prop.setValue(true);
		this.setProperties(props);
		this.mergeDocumentStructures(sub1);

		assertEquals(tokens, sDoc1.getDocumentGraph().getTokens().size());
		assertEquals(spans, sDoc1.getDocumentGraph().getSpans().size());
		assertEquals(structs, sDoc1.getDocumentGraph().getStructures().size());
	}

	/**
	 * Merges 4 documents containing the following texts:
	 * <ol>
	 * <li>
	 * <ol>
	 * <li>Wie?UNINTERPRETABLE#Ne?</li>
	 * <li>SPK3!Wenndumehrals50Stundenhast,bekommstdumehrGeld.</li>
	 * <li>Nein.</li>
	 * </ol>
	 * </li>
	 * <li>Wie?UNINTERPRETABLE#Ne?</li>
	 * <li>SPK3!Wenndumehrals50Stundenhast,bekommstdumehrGeld.</li>
	 * <li>Nein.</li>
	 * </ol>
	 */
	@Test
	public void testMerge_MultipleDocumentsWithMultipleTexts() {
		List<SStructuredNode> structures = null;

		// document 0
		SDocument doc0 = SaltFactory.createSDocument();
		doc0.setId("doc0");
		doc0.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text01 = doc0.getDocumentGraph().createTextualDS("Wie?UNINTERPRETABLE#Ne?");
		SToken tok01 = doc0.getDocumentGraph().createToken(text01, 0, 3);
		SToken tok02 = doc0.getDocumentGraph().createToken(text01, 3, 4);
		SToken tok03 = doc0.getDocumentGraph().createToken(text01, 4, 19);
		SToken tok04 = doc0.getDocumentGraph().createToken(text01, 19, 20);
		SToken tok05 = doc0.getDocumentGraph().createToken(text01, 20, 22);
		SToken tok06 = doc0.getDocumentGraph().createToken(text01, 22, 23);

		STextualDS text02 = doc0.getDocumentGraph().createTextualDS("SPK3!Wenndumehrals50Stundenhast,bekommstdumehrGeld.");
		SToken tok07 = doc0.getDocumentGraph().createToken(text02, 0, 4);
		SToken tok08 = doc0.getDocumentGraph().createToken(text02, 4, 5);
		SToken tok09 = doc0.getDocumentGraph().createToken(text02, 5, 9);
		SToken tok010 = doc0.getDocumentGraph().createToken(text02, 9, 11);
		SToken tok011 = doc0.getDocumentGraph().createToken(text02, 11, 15);
		SToken tok012 = doc0.getDocumentGraph().createToken(text02, 15, 18);
		SToken tok013 = doc0.getDocumentGraph().createToken(text02, 18, 20);
		SToken tok014 = doc0.getDocumentGraph().createToken(text02, 20, 27);
		SToken tok015 = doc0.getDocumentGraph().createToken(text02, 27, 31);
		SToken tok016 = doc0.getDocumentGraph().createToken(text02, 31, 32);
		SToken tok017 = doc0.getDocumentGraph().createToken(text02, 32, 40);
		SToken tok018 = doc0.getDocumentGraph().createToken(text02, 40, 42);
		SToken tok019 = doc0.getDocumentGraph().createToken(text02, 42, 46);
		SToken tok020 = doc0.getDocumentGraph().createToken(text02, 46, 50);
		SToken tok021 = doc0.getDocumentGraph().createToken(text02, 50, 51);

		STextualDS text03 = doc0.getDocumentGraph().createTextualDS("Nein.?");
		SToken tok022 = doc0.getDocumentGraph().createToken(text03, 0, 4);
		SToken tok023 = doc0.getDocumentGraph().createToken(text03, 4, 5);

		// create document 1
		SDocument doc1 = SaltFactory.createSDocument();
		doc1.setId("doc1");
		doc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text11 = doc1.getDocumentGraph().createTextualDS("Wie?UNINTERPRETABLE#Ne?.");
		SToken tok11 = doc1.getDocumentGraph().createToken(text11, 0, 3);
		SToken tok12 = doc1.getDocumentGraph().createToken(text11, 3, 4);
		SToken tok13 = doc1.getDocumentGraph().createToken(text11, 4, 19);
		SToken tok14 = doc1.getDocumentGraph().createToken(text11, 19, 20);
		SToken tok15 = doc1.getDocumentGraph().createToken(text11, 20, 22);
		SToken tok16 = doc1.getDocumentGraph().createToken(text11, 22, 23);
		SStructure struct11 = doc1.getDocumentGraph().createSStructure(tok11);
		struct11.createAnnotation(null, "cat", "ADVX");
		SStructure struct12 = doc1.getDocumentGraph().createSStructure(tok12);
		struct12.createAnnotation(null, "cat", "NSU");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct11);
		structures.add(struct12);
		SStructure struct13 = doc1.getDocumentGraph().createStructure(structures);
		struct13.createAnnotation(null, "cat", "VROOT");

		SStructure struct14 = doc1.getDocumentGraph().createSStructure(tok13);
		struct14.createAnnotation(null, "cat", "FRAG");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct14);
		structures.add(tok14);
		SStructure struct15 = doc1.getDocumentGraph().createStructure(structures);
		struct15.createAnnotation(null, "cat", "VROOT");

		SStructure struct16 = doc1.getDocumentGraph().createSStructure(tok15);
		struct16.createAnnotation(null, "cat", "DM");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct16);
		structures.add(tok16);
		SStructure struct17 = doc1.getDocumentGraph().createStructure(structures);
		struct17.createAnnotation(null, "cat", "VROOT");

		// create document 2
		SDocument doc2 = SaltFactory.createSDocument();
		doc2.setId("doc2");
		doc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text22 = doc2.getDocumentGraph().createTextualDS("SPK3!Wenndumehrals50Stundenhast,bekommstdumehrGeld.");
		SToken tok21 = doc2.getDocumentGraph().createToken(text22, 0, 4);
		SToken tok22 = doc2.getDocumentGraph().createToken(text22, 4, 5);
		SToken tok23 = doc2.getDocumentGraph().createToken(text22, 5, 9);
		SToken tok24 = doc2.getDocumentGraph().createToken(text22, 9, 11);
		SToken tok25 = doc2.getDocumentGraph().createToken(text22, 11, 15);
		SToken tok26 = doc2.getDocumentGraph().createToken(text22, 15, 18);
		SToken tok27 = doc2.getDocumentGraph().createToken(text22, 18, 20);
		SToken tok28 = doc2.getDocumentGraph().createToken(text22, 20, 27);
		SToken tok29 = doc2.getDocumentGraph().createToken(text22, 27, 31);
		SToken tok210 = doc2.getDocumentGraph().createToken(text22, 31, 32);
		SToken tok211 = doc2.getDocumentGraph().createToken(text22, 32, 40);
		SToken tok212 = doc2.getDocumentGraph().createToken(text22, 40, 42);
		SToken tok213 = doc2.getDocumentGraph().createToken(text22, 42, 46);
		SToken tok214 = doc2.getDocumentGraph().createToken(text22, 46, 50);
		SToken tok215 = doc2.getDocumentGraph().createToken(text22, 50, 51);
		SStructure struct01 = doc2.getDocumentGraph().createSStructure(tok21);
		struct01.createAnnotation(null, "cat", "NX");
		SStructure struct02 = doc2.getDocumentGraph().createSStructure(tok22);
		struct02.createAnnotation(null, "cat", "DM");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct01);
		structures.add(struct02);
		SStructure struct03 = doc2.getDocumentGraph().createStructure(structures);
		struct03.createAnnotation(null, "cat", "VROOT");

		SStructure struct04 = doc2.getDocumentGraph().createSStructure(tok23);
		struct04.createAnnotation(null, "cat", "C");
		SStructure struct05 = doc2.getDocumentGraph().createSStructure(tok24);
		struct05.createAnnotation(null, "cat", "NX");
		SStructure struct06 = doc2.getDocumentGraph().createSStructure(tok25);
		struct06.createAnnotation(null, "cat", "NX");
		structures = new ArrayList<SStructuredNode>();
		structures.add(tok26);
		structures.add(tok27);
		structures.add(tok28);
		SStructure struct07 = doc2.getDocumentGraph().createStructure(structures);
		struct07.createAnnotation(null, "cat", "NX");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct05);
		structures.add(struct06);
		structures.add(struct07);
		SStructure struct08 = doc2.getDocumentGraph().createStructure(structures);
		struct08.createAnnotation(null, "cat", "MF");

		// create document 3
		SDocument doc3 = SaltFactory.createSDocument();
		doc3.setId("doc3");
		doc3.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text31 = doc3.getDocumentGraph().createTextualDS("Nein.");
		SToken tok31 = doc3.getDocumentGraph().createToken(text31, 0, 4);
		SToken tok32 = doc3.getDocumentGraph().createToken(text31, 4, 5);
		SStructure struct31 = doc3.getDocumentGraph().createSStructure(tok31);
		struct31.createAnnotation(null, "cat", "DM");
		structures = new ArrayList<SStructuredNode>();
		structures.add(struct31);
		structures.add(tok32);
		SStructure struct32 = doc3.getDocumentGraph().createStructure(structures);
		struct32.createAnnotation(null, "cat", "VROOT");

		// create mapping subjects for documents
		MappingSubject sub0 = new MappingSubject();
		sub0.setIdentifier(doc0.getIdentifier());
		getFixture().getMappingSubjects().add(sub0);

		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(doc1.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);

		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(doc2.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);

		MappingSubject sub3 = new MappingSubject();
		sub3.setIdentifier(doc3.getIdentifier());
		getFixture().getMappingSubjects().add(sub3);

		this.mergeDocumentStructures(sub0);

		assertEquals(3, doc0.getDocumentGraph().getTextualDSs().size());
		assertEquals(23, doc0.getDocumentGraph().getTokens().size());
		assertEquals(17, doc0.getDocumentGraph().getStructures().size());
	}

	@Test
	public void testMovingNodes() throws Exception {
		SDocument sDoc1 = SaltFactory.createSDocument();
		SDocument sDoc2 = SaltFactory.createSDocument();

		sDoc1.setName("doc1");
		sDoc2.setName("doc2");

		SDocumentGraph graph1 = SaltFactory.createSDocumentGraph();
		SDocumentGraph graph2 = SaltFactory.createSDocumentGraph();
		sDoc1.setDocumentGraph(graph1);
		sDoc2.setDocumentGraph(graph2);

		STextualDS sTextDS1 = sDoc1.getDocumentGraph().createTextualDS("boat");
		SToken tok1 = sDoc1.getDocumentGraph().createToken(sTextDS1, 0, 3);
		SSpan span1 = graph1.createSpan(tok1);
		SSpanningRelation rel1 = (SSpanningRelation) (SRelation)graph1.getInRelations(tok1.getId()).get(0);

		assertEquals(graph1, tok1.getGraph());
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);

		tok1.setGraph(graph2);

		assertEquals(graph2, tok1.getGraph());
		assertEquals(graph1, span1.getGraph());
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);
		// move all node before the relation
		span1.setGraph(graph2);
		rel1.setGraph(graph2);
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);
	}

	/**
	 * 
	 */
	@Test
	public void testCopySLayers() {
		SCorpusGraph g1 = SaltFactory.createSCorpusGraph();
		SDocument d1_1 = g1.createDocument(URI.createURI("/c1/d1"));
		d1_1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_1.getDocumentGraph().createTextualDS("a sample text");
		d1_1.getDocumentGraph().tokenize();
		d1_1.getDocumentGraph().createSpan(d1_1.getDocumentGraph().getTokens());
		SLayer sLayer = SaltFactory.createSLayer();
		sLayer.setName("myLayer");
		d1_1.getDocumentGraph().addLayer(sLayer);
		for (SToken sTok : d1_1.getDocumentGraph().getTokens()) {
			sLayer.addNode(sTok);
		}
		sLayer.addNode(d1_1.getDocumentGraph().getSpans().get(0));
		for (SSpanningRelation rel : d1_1.getDocumentGraph().getSpanningRelations()) {
			sLayer.addRelation(rel);
		}

		SCorpusGraph g2 = SaltFactory.createSCorpusGraph();
		SDocument d1_2 = g2.createDocument(URI.createURI("/c1/d1"));
		d1_2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_2.getDocumentGraph().createTextualDS("a sample text.");
		d1_2.getDocumentGraph().tokenize();

		MappingSubject subj_1 = new MappingSubject();
		subj_1.setIdentifier(d1_1.getIdentifier());
		getFixture().getMappingSubjects().add(subj_1);
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setIdentifier(d1_2.getIdentifier());
		getFixture().getMappingSubjects().add(subj_2);

		getFixture().setBaseCorpusStructure(g2);
		getFixture().mapSDocument();

		assertNotNull(d1_2.getDocumentGraph().getLayerByName("myLayer").size());
		assertEquals(1, d1_2.getDocumentGraph().getLayerByName("myLayer").size());

		SLayer fixSLayer = d1_2.getDocumentGraph().getLayerByName("myLayer").get(0);
		assertNotNull(fixSLayer.getNodes());
		assertEquals(4, fixSLayer.getNodes().size());
		assertNotNull(fixSLayer.getRelations());
		assertEquals(3, fixSLayer.getRelations().size());
	}
}
