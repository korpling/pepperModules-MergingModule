package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpanningRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSample.SaltSample;

public class MergerMapperTest_graph extends MergerMapper {

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
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setSElementId(sDoc1.getSElementId());
		getFixture().getMappingSubjects().add(sub1);
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		SaltSample.createAnaphoricAnnotations(sDoc1);

		// doc 2
		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("sdoc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setSElementId(sDoc2.getSElementId());
		getFixture().getMappingSubjects().add(sub2);
		SaltSample.createPrimaryData(sDoc2);
		SaltSample.createTokens(sDoc2);
		SaltSample.createSyntaxStructure(sDoc2);
		SaltSample.createSyntaxAnnotations(sDoc2);

		// doc 3
		SDocument sDoc3 = SaltFactory.eINSTANCE.createSDocument();
		sDoc3.setSId("sdoc3");
		sDoc3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub3 = new MappingSubject();
		sub3.setSElementId(sDoc3.getSElementId());
		getFixture().getMappingSubjects().add(sub3);
		SaltSample.createPrimaryData(sDoc3);
		SaltSample.createTokens(sDoc3);
		SaltSample.createMorphologyAnnotations(sDoc3);

		// template document contains all annotations
		SDocument template = SaltFactory.eINSTANCE.createSDocument();
		template.setSId("template");
		template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(template);
		SaltSample.createTokens(template);
		SaltSample.createSyntaxStructure(template);
		SaltSample.createAnaphoricAnnotations(template);
		SaltSample.createSyntaxAnnotations(template);
		SaltSample.createMorphologyAnnotations(template);

		this.isTestMode = true;

		this.mergeSDocumentGraph();

		// first document must be the base document
		assertEquals(sDoc1, container.getBaseDocument());
		// the text of the first document must be the base text
		assertEquals(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), container.getBaseDocument().getSDocumentGraph().getSTextualDSs().get(0));

		// the count of tokens in sDoc1 must be the same as before!
		assertEquals(template.getSDocumentGraph().getSTokens().size(), sDoc1.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(), sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSSpanningRelations().size(), sDoc1.getSDocumentGraph().getSSpanningRelations().size());
		assertEquals(template.getSDocumentGraph().getSStructures().size(), sDoc1.getSDocumentGraph().getSStructures().size());
		assertEquals(template.getSDocumentGraph().getSDominanceRelations().size(), sDoc1.getSDocumentGraph().getSDominanceRelations().size());

		assertEquals(template.getSDocumentGraph().getSNodes().size(), sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());

		assertEquals(template.getSDocumentGraph().getSRoots().size(), sDoc2.getSDocumentGraph().getSRoots().size());

		assertNotNull(sDoc1.getSDocumentGraph());
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
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setSElementId(sDoc1.getSElementId());
		getFixture().getMappingSubjects().add(sub1);
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("sdoc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setSElementId(sDoc2.getSElementId());
		getFixture().getMappingSubjects().add(sub2);
		SaltSample.createPrimaryData(sDoc2);
		SaltSample.createTokens(sDoc2);
		SaltSample.createInformationStructureSpan(sDoc2);
		SaltSample.createInformationStructureAnnotations(sDoc2);

		// template document contains all annotations
		SDocument template = SaltFactory.eINSTANCE.createSDocument();
		template.setSId("template");
		template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(template);
		SaltSample.createTokens(template);
		SaltSample.createInformationStructureSpan(template);
		SaltSample.createInformationStructureAnnotations(template);

		assertEquals(0, sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(0, sDoc1.getSDocumentGraph().getSSpanningRelations().size());

		this.isTestMode = true;
		this.mergeSDocumentGraph();

		assertNotNull(sDoc1.getSDocumentGraph());
		assertEquals(template.getSDocumentGraph().getSTokens().size(), sDoc1.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(), sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSSpanningRelations().size(), sDoc1.getSDocumentGraph().getSSpanningRelations().size());

		assertEquals(template.getSDocumentGraph().getSNodes().size(), sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
	}

	/**
	 * Tests two {@link SDocumentGraph} containing {@link SSpan}s the same
	 * spans, one contains annotations, the other one does not. In the end, both
	 * shall have the same annotations.
	 */
	@Test
	public void testMergeSpans2() {
		// set up empty documents
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setSElementId(sDoc1.getSElementId());
		getFixture().getMappingSubjects().add(sub1);
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		SaltSample.createInformationStructureSpan(sDoc1);

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("sdoc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub2 = new MappingSubject();
		sub2.setSElementId(sDoc2.getSElementId());
		getFixture().getMappingSubjects().add(sub2);
		SaltSample.createPrimaryData(sDoc2);
		SaltSample.createTokens(sDoc2);
		SaltSample.createInformationStructureSpan(sDoc2);
		SaltSample.createInformationStructureAnnotations(sDoc2);

		// template document contains all annotations
		SDocument template = SaltFactory.eINSTANCE.createSDocument();
		template.setSId("template");
		template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(template);
		SaltSample.createTokens(template);
		SaltSample.createInformationStructureSpan(template);
		SaltSample.createInformationStructureAnnotations(template);

		this.isTestMode = true;
		this.mergeSDocumentGraph();

		assertNotNull(sDoc1.getSDocumentGraph());
		assertEquals(template.getSDocumentGraph().getSTokens().size(), sDoc1.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(), sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSSpanningRelations().size(), sDoc1.getSDocumentGraph().getSSpanningRelations().size());

		assertEquals(template.getSDocumentGraph().getSSpans().get(0).getSAnnotations().size(), sDoc1.getSDocumentGraph().getSSpans().get(0).getSAnnotations().size());
		assertTrue(template.getSDocumentGraph().getSSpans().get(0).getSAnnotations().containsAll(sDoc1.getSDocumentGraph().getSSpans().get(0).getSAnnotations()));

		assertEquals(template.getSDocumentGraph().getSSpans().get(1).getSAnnotations().size(), sDoc1.getSDocumentGraph().getSSpans().get(1).getSAnnotations().size());
		assertTrue(template.getSDocumentGraph().getSSpans().get(1).getSAnnotations().containsAll(sDoc1.getSDocumentGraph().getSSpans().get(1).getSAnnotations()));

		assertEquals(template.getSDocumentGraph().getSNodes().size(), sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
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
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("sdoc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());

		SDocument sDoc3 = SaltFactory.eINSTANCE.createSDocument();
		sDoc3.setSId("sdoc3");
		sDoc3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());

		MappingSubject sub1 = new MappingSubject();
		MappingSubject sub2 = new MappingSubject();
		MappingSubject sub3 = new MappingSubject();

		sub1.setSElementId(sDoc1.getSElementId());
		sub2.setSElementId(sDoc2.getSElementId());
		sub3.setSElementId(sDoc3.getSElementId());

		getFixture().getMappingSubjects().add(sub1);
		getFixture().getMappingSubjects().add(sub2);
		getFixture().getMappingSubjects().add(sub3);

		// document data
		// doc 1
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		SaltSample.createAnaphoricAnnotations(sDoc1);

		// doc 2
		SaltSample.createPrimaryData(sDoc2);
		SaltSample.createTokens(sDoc2);
		SaltSample.createSyntaxStructure(sDoc2);
		SaltSample.createSyntaxAnnotations(sDoc2);

		// doc 3
		SaltSample.createPrimaryData(sDoc3);
		SaltSample.createTokens(sDoc3);
		SaltSample.createMorphologyAnnotations(sDoc3);

		// template document contains all annotations
		SDocument template = SaltFactory.eINSTANCE.createSDocument();
		template.setSId("template");
		template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(template);
		SaltSample.createTokens(template);
		SaltSample.createSyntaxStructure(template);
		SaltSample.createAnaphoricAnnotations(template);
		SaltSample.createSyntaxAnnotations(template);
		SaltSample.createMorphologyAnnotations(template);

		this.isTestMode = false;
		this.mergeSDocumentGraph();

		assertEquals(DOCUMENT_STATUS.COMPLETED, sub1.getMappingResult());
		assertEquals(DOCUMENT_STATUS.DELETED, sub2.getMappingResult());
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
	 * <li>{@value SaltSample#PRIMARY_TEXT_EN}</li>
	 * <li>Well. {@value SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol>
	 */
	@Test
	public void testMergeTokens_case1() {
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(sDoc1);

		SLayer morphLayer = SaltFactory.eINSTANCE.createSLayer();
		morphLayer.setSName("morphology");
		sDoc1.getSDocumentGraph().addSLayer(morphLayer);
		SaltSample.createToken(0, 2, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // Is
		SaltSample.createToken(3, 7, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // this
		SaltSample.createToken(8, 15, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // example
		SaltSample.createToken(16, 20, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // more
		SaltSample.createToken(21, 32, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // complicated
		SaltSample.createToken(33, 37, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // than
		SaltSample.createToken(38, 40, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // it
		SaltSample.createToken(41, 48, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // supposed
		SaltSample.createToken(49, 51, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // to
		// SaltSample.createToken(52,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);
		// //be?
		SaltSample.createToken(52, 54, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // be
		SaltSample.createToken(54, 55, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // ?

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. " + SaltSample.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();

		EList<SToken> baseTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> otherTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();

		this.normalizeTextualLayer(sDoc1);
		this.normalizeTextualLayer(sDoc2);

		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));
		assertEquals(otherTextToken.size(), equivalenceMap.size());

		// test 3 : the token alignment is correct : the equivalence classes are
		// correct
		int j = 0;
		for (int i = 2; i < 11; i++) {
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0);
			assertEquals("Base Token " + base.getSName() + " (start: " + this.container.getAlignedTokenStart(this.container.getBaseText(), base) + ") and other token " + otherToken.getSName() + " (start: " + this.container.getAlignedTokenStart(otherText, otherToken) + ") should be equal.", this.container.getTokenMapping(base, otherText), otherToken);
			j++;
		}

		int equivalenceMapSize = equivalenceMap.size();
		// assert that the merging did not change something
		this.mergeTokens(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0), equivalenceMap);
		assertEquals(equivalenceMapSize, equivalenceMap.size());

		j = 0;
		for (int i = 2; i < 11; i++) {
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0);
			assertEquals("Base Token " + base.getSName() + " (start: " + this.container.getAlignedTokenStart(this.container.getBaseText(), base) + ") and other token " + otherToken.getSName() + " (start: " + this.container.getAlignedTokenStart(otherText, otherToken) + ") should be equal.", this.container.getTokenMapping(base, otherText), otherToken);
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
	 * <li>{@value SaltSample#PRIMARY_TEXT_EN}</li>
	 * <li>Well. {@value SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol>
	 * 
	 * In this test, the first text is used as base text and one token of the
	 * first text is removed
	 */
	@Test
	public void testMergeTokens_case2() {
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(sDoc1);

		SLayer morphLayer = SaltFactory.eINSTANCE.createSLayer();
		morphLayer.setSName("morphology");
		sDoc1.getSDocumentGraph().addSLayer(morphLayer);
		SaltSample.createToken(0, 2, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // Is
		SaltSample.createToken(3, 7, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // this
		SaltSample.createToken(8, 15, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // example
		SaltSample.createToken(16, 20, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // more
		// SaltSample.createToken(21,32,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);
		// //complicated
		SaltSample.createToken(33, 37, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // than
		SaltSample.createToken(38, 40, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // it
		SaltSample.createToken(41, 48, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // supposed
		SaltSample.createToken(49, 51, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // to
		// SaltSample.createToken(52,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);
		// //be?
		SaltSample.createToken(52, 54, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // be
		SaltSample.createToken(54, 55, sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc1, morphLayer); // ?

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. " + SaltSample.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();

		List<SToken> baseTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();
		List<SToken> otherTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();

		this.normalizeTextualLayer(sDoc1);
		this.normalizeTextualLayer(sDoc2);

		// test 1 : sDoc2 must be the base document
		this.container.setBaseDocument(sDoc1);
		this.container.setBaseText(sDoc1.getSDocumentGraph().getSTextualDSs().get(0));

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));
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
		this.mergeTokens(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0), equivalenceMap);
		assertTrue(equivalenceMapSize != equivalenceMap.size());
		assertEquals(baseTextToken.size() + 1, equivalenceMap.size());
		assertNotNull(equivalenceMap.get(otherTextToken.get(6)));
	}

	@Test
	public void testMovingNodes() throws Exception {
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();

		sDoc1.setSName("doc1");
		sDoc2.setSName("doc2");

		SDocumentGraph graph1 = SaltFactory.eINSTANCE.createSDocumentGraph();
		SDocumentGraph graph2 = SaltFactory.eINSTANCE.createSDocumentGraph();
		sDoc1.setSDocumentGraph(graph1);
		sDoc2.setSDocumentGraph(graph2);

		STextualDS sTextDS1 = sDoc1.getSDocumentGraph().createSTextualDS("boat");
		SToken tok1 = sDoc1.getSDocumentGraph().createSToken(sTextDS1, 0, 3);
		SSpan span1 = graph1.createSSpan(tok1);
		SSpanningRelation rel1 = (SSpanningRelation) graph1.getInEdges(tok1.getSId()).get(0);

		assertEquals(graph1, tok1.getSDocumentGraph());
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);

		tok1.setSDocumentGraph(graph2);

		assertEquals(graph2, tok1.getSDocumentGraph());
		assertEquals(graph1, span1.getSDocumentGraph());
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);
		// move all node before the edge
		span1.setSDocumentGraph(graph2);
		rel1.setSDocumentGraph(graph2);
		assertEquals(rel1.getSource(), span1);
		assertEquals(rel1.getTarget(), tok1);
	}

	/**
	 * 
	 */
	@Test
	public void testCopySLayers() {
		SCorpusGraph g1 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_1 = g1.createSDocument(URI.createURI("/c1/d1"));
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_1.getSDocumentGraph().createSTextualDS("a sample text");
		d1_1.getSDocumentGraph().tokenize();
		d1_1.getSDocumentGraph().createSSpan(d1_1.getSDocumentGraph().getSTokens());
		SLayer sLayer = SaltFactory.eINSTANCE.createSLayer();
		sLayer.setSName("myLayer");
		d1_1.getSDocumentGraph().addSLayer(sLayer);
		for (SToken sTok : d1_1.getSDocumentGraph().getSTokens()) {
			sLayer.getSNodes().add(sTok);
		}
		sLayer.getSNodes().add(d1_1.getSDocumentGraph().getSSpans().get(0));
		for (SSpanningRelation rel : d1_1.getSDocumentGraph().getSSpanningRelations()) {
			sLayer.getSRelations().add(rel);
		}

		SCorpusGraph g2 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_2 = g2.createSDocument(URI.createURI("/c1/d1"));
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_2.getSDocumentGraph().createSTextualDS("a sample text.");
		d1_2.getSDocumentGraph().tokenize();

		MappingSubject subj_1 = new MappingSubject();
		subj_1.setSElementId(d1_1.getSElementId());
		getFixture().getMappingSubjects().add(subj_1);
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setSElementId(d1_2.getSElementId());
		getFixture().getMappingSubjects().add(subj_2);

		getFixture().setBaseCorpusStructure(g2);
		getFixture().mapSDocument();

		assertNotNull(d1_2.getSDocumentGraph().getSLayerByName("myLayer").size());
		assertEquals(1, d1_2.getSDocumentGraph().getSLayerByName("myLayer").size());

		SLayer fixSLayer = d1_2.getSDocumentGraph().getSLayerByName("myLayer").get(0);
		assertNotNull(fixSLayer.getSNodes());
		assertEquals(4, fixSLayer.getSNodes().size());
		assertNotNull(fixSLayer.getSRelations());
		assertEquals(3, fixSLayer.getSRelations().size());
	}
}
