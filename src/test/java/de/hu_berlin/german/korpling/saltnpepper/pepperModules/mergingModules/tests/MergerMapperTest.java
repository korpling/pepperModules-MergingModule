package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperty;
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

public class MergerMapperTest extends MergerMapper{

	private MergerMapper fixture= null;
	public MergerMapper getFixture() {
		return fixture;
	}

	public void setFixture(MergerMapper fixture) {
		this.fixture = fixture;
	}
	@Before
	public void setUp(){
		setFixture(this);
		this.setProperties(new MergerProperties());
		this.initialize();
	}
	/**
	 * Tests the mapping of three documents containing the same primary data and same tokenization, but
	 * different annotation layers:
	 * <ol>
	 * 	<li>document1: anaphoric relations (pointing relations)</li>
	 * 	<li>document2: syntactic annotations </li>
	 * 	<li>document3: morphological annotations (POS and lemma)</li>
	 * </ol>
	 */
	@Test
	public void testMap3Documents_sameTokenization() {
		// set up empty documents
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		SDocument sDoc3= SaltFactory.eINSTANCE.createSDocument();
		
		sDoc1.setSId("sdoc1");
		sDoc2.setSId("sdoc2");
		sDoc3.setSId("sdoc3");

		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
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
		SDocument template= SaltFactory.eINSTANCE.createSDocument();
		template.setSId("template");
		template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(template);
		SaltSample.createTokens(template);
		SaltSample.createSyntaxStructure(template);
		SaltSample.createAnaphoricAnnotations(template);
		SaltSample.createSyntaxAnnotations(template);
		SaltSample.createMorphologyAnnotations(template);
		/*
		STextualDS sText1 = sDoc1.getSDocumentGraph().getSTextualDSs().get(0);
		STextualDS sText2 = sDoc2.getSDocumentGraph().getSTextualDSs().get(0);
		STextualDS sText3 = sDoc3.getSDocumentGraph().getSTextualDSs().get(0);
		
		sText1.setId(sText1.getId()+"_1");
		sText2.setId(sText1.getId()+"_2");
		sText3.setId(sText1.getId()+"_3");
		*/
		
		this.isTestMode = true;
		this.mergeSDocumentGraph();
		
		
		// First test: the first document must be the base document
		assertEquals(sDoc1,container.getBaseDocument());
		//assertNotNull("Document 2 was deleted!",sDoc2);
		//assertNotNull("The text of Document 2 was deleted!",sDoc2.getSDocumentGraph().getSTextualDSs());
		//assertTrue("The text of Document 2 was deleted!",sDoc2.getSDocumentGraph().getSTextualDSs().size() != 0);
		
		//assertNotNull("The tokens of Document 2 was deleted!",sDoc2.getSDocumentGraph().getSTokens());
		//assertTrue("The tokens of Document 2 was deleted!",sDoc2.getSDocumentGraph().getSTokens().size() != 0);
		
		//assertNotNull("We have a bug here. A token is deleted!",this.container.getAlignedTokenByStart(sText2, 0));
		//System.out.println("Normalized SText for doc2: "+this.container.getNormalizedText(sText2));
		
		// Second test: the text of the first document must be the base text
		assertEquals(sDoc1.getSDocumentGraph().getSTextualDSs().get(0),container.getBaseDocument().getSDocumentGraph().getSTextualDSs().get(0));
		
		// Fourth test: for (doc : [doc2,doc3])
		//                 for every token i of doc:
		//                      token j of doc1 is equivalent to token i
		/*
		EList<SToken> sDoc1Tokens = sDoc1.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> sDoc2Tokens = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> sDoc3Tokens = sDoc3.getSDocumentGraph().getSortedSTokenByText();
		int i = 0;
		
		for (SToken sDoc1Token : sDoc1Tokens){
			//System.out.println("Position and length of doc1 token "+sDoc1Token.getSName()+": "+this.container.getAlignedTokenStart(sText1, sDoc1Token)+"/"+this.container.getAlignedTokenLength(sText1, sDoc1Token));
			//System.out.println("Position and length of doc2 token "+sDoc2Tokens.get(i).getSName()+": "+this.container.getAlignedTokenStart(sText1, sDoc2Tokens.get(i))+"/"+this.container.getAlignedTokenLength(sText1, sDoc2Tokens.get(i)));
			//System.out.println("Position and length of doc3 token "+sDoc3Tokens.get(i).getSName()+": "+this.container.getAlignedTokenStart(sText1, sDoc3Tokens.get(i))+"/"+this.container.getAlignedTokenLength(sText1, sDoc3Tokens.get(i)));
			assertEquals(sDoc2Tokens.get(i),this.container.getTokenMapping(sDoc1Token, sDoc2.getSDocumentGraph().getSTextualDSs().get(0)));
			assertEquals(sDoc3Tokens.get(i),this.container.getTokenMapping(sDoc1Token, sDoc3.getSDocumentGraph().getSTextualDSs().get(0)));
			i++;
		}*/
		
		//SaltFactory.eINSTANCE.save_DOT(sDoc1.getSDocumentGraph(), URI.createFileURI("/home/florian/Test/merging/mergedDoc.dot"));
		
		// the count of tokens in sDoc1 must be the same as before!
		assertEquals(template.getSDocumentGraph().getSTokens().size(),     sDoc1.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(),      sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSSpanningRelations().size(),      sDoc1.getSDocumentGraph().getSSpanningRelations().size());
		
		System.out.println(template.getSDocumentGraph().getSNodes());
		System.out.println("---");
		System.out.println(sDoc1.getSDocumentGraph().getSNodes());
		
		assertEquals(template.getSDocumentGraph().getSNodes().size(),     sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
		
		assertEquals(template.getSDocumentGraph().getSStructures().size(), sDoc1.getSDocumentGraph().getSStructures().size());
		
		assertEquals(template.getSDocumentGraph().getSRoots().size(), sDoc2.getSDocumentGraph().getSRoots().size());
		
		assertNotNull(sDoc1.getSDocumentGraph());
//		TODO: Why should the SDocumentGraph suddenly become null?
//		assertNull(sDoc2.getSDocumentGraph());
//		assertNull(sDoc3.getSDocumentGraph());
	}
	
	/**
	 * Tests one {@link SDocumentGraph} containing {@link SSpan}s and one {@link SDocumentGraph}, which does not.
	 * In the end, the one which does not should contain all spans, which are contained by the other {@link SDocumentGraph}.
	 */
	@Test
	public void testMergeSpans() {
		// set up empty documents
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setSElementId(sDoc1.getSElementId());
		getFixture().getMappingSubjects().add(sub1); 
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
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
		SDocument template= SaltFactory.eINSTANCE.createSDocument();
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
		
		assertEquals(template.getSDocumentGraph().getSNodes().size(),     sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
	}
	
	/**
	 * Tests two {@link SDocumentGraph} containing {@link SSpan}s the same spans, one contains annotations, the other one does not.
	 * In the end, both shall have the same annotations.
	 */
	@Test
	public void testMergeSpans2() {
		// set up empty documents
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		MappingSubject sub1 = new MappingSubject();
		sub1.setSElementId(sDoc1.getSElementId());
		getFixture().getMappingSubjects().add(sub1); 
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		SaltSample.createInformationStructureSpan(sDoc1);
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
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
		SDocument template= SaltFactory.eINSTANCE.createSDocument();
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
		System.out.println(template.getSDocumentGraph().getSNodes());
		System.out.println("---");
		System.out.println(sDoc1.getSDocumentGraph().getSNodes());
		assertEquals(template.getSDocumentGraph().getSSpans().size(), sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSSpanningRelations().size(), sDoc1.getSDocumentGraph().getSSpanningRelations().size());
		
		assertEquals(template.getSDocumentGraph().getSSpans().get(0).getSAnnotations().size(), sDoc1.getSDocumentGraph().getSSpans().get(0).getSAnnotations().size());
		assertTrue(template.getSDocumentGraph().getSSpans().get(0).getSAnnotations().containsAll(sDoc1.getSDocumentGraph().getSSpans().get(0).getSAnnotations()));
		
		assertEquals(template.getSDocumentGraph().getSSpans().get(1).getSAnnotations().size(), sDoc1.getSDocumentGraph().getSSpans().get(1).getSAnnotations().size());
		assertTrue(template.getSDocumentGraph().getSSpans().get(1).getSAnnotations().containsAll(sDoc1.getSDocumentGraph().getSSpans().get(1).getSAnnotations()));
		
		
		assertEquals(template.getSDocumentGraph().getSNodes().size(),     sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
	}
	
	/**
	 * Tests the document status after the mapping of three documents containing the same primary data and same tokenization, but
	 * different annotation layers:
	 * <ol>
	 * 	<li>document1: anaphoric relations (pointing relations)</li>
	 * 	<li>document2: syntactic annotations </li>
	 * 	<li>document3: morphological annotations (POS and lemma)</li>
	 * </ol>
	 */
	@Test
	public void testMap3Documents_sameTokenizationDocumentStatus() {
		// set up empty documents
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("sdoc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("sdoc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
				
		SDocument sDoc3= SaltFactory.eINSTANCE.createSDocument();
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
		SDocument template= SaltFactory.eINSTANCE.createSDocument();
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
		assertEquals(DOCUMENT_STATUS.DELETED,   sub2.getMappingResult());
		assertEquals(DOCUMENT_STATUS.DELETED,   sub3.getMappingResult());
		
	}
	
	/**
	 * Tests the normalization function with different texts.
	 * <ol>
	 * 	<li>empty texts</li>
	 * 	<li>english text containing whitespaces and punctuations</li>
	 * 	<li>german text containing whitespaces and umlauts</li>
	 * </ol>
	 */
	@Test
	public void testNormalize(){
		String origText="";
		String normText="";
		
		//test 1
		SDocument doc1 = SaltFactory.eINSTANCE.createSDocument();
		doc1.setSId("doc1");
		doc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		doc1.getSDocumentGraph().createSTextualDS(origText);
		this.normalizeTextualLayer(doc1);
		
		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);
		
		//test2 
		origText= "Is this sample more complicated, than it appears to be?";
		normText= "Isthissamplemorecomplicated,thanitappearstobe?";
		doc1.getSDocumentGraph().getSTextualDSs().get(0).setSText(origText);
		this.normalizeTextualLayer(doc1);
		
		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);
		
		//test3 
		origText= "Das wäre überaus schön";
		normText= "Daswaereueberausschoen";
		
		doc1.getSDocumentGraph().getSTextualDSs().get(0).setSText(origText);
		this.normalizeTextualLayer(doc1);
		
		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);
	}
	
	@Test
	public void testIndexOfOmitChars(){
		String baseText ="This,isasmallExample!";
		String otherText ="Thisisno";
		
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties)getProperties()).getPunctuations()),-1);
		
		otherText ="This;is";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties)getProperties()).getPunctuations()),0);
		
		baseText ="Thisisnosmallexample.Itisasmallerexample!";
		otherText = "exampleItis";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties)getProperties()).getPunctuations()),13);
		
		otherText = ".exampleItis";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties)getProperties()).getPunctuations()),13);
		
		baseText ="Thisisnosmallexampl.Itisasmallerexampl";
		otherText = "example";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties)getProperties()).getPunctuations()),-1);
				
	}
	
	/**
	 *
	 * 
	 */
	@Test
	public void textCreateBaseTextNormOriginalMapping(){
		String origText=" thäs is";
		
		//test 1
		SDocument doc1 = SaltFactory.eINSTANCE.createSDocument();
		doc1.setSId("doc1");
		doc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		doc1.getSDocumentGraph().createSTextualDS(origText);
		this.normalizeTextualLayer(doc1);
		
		List<Integer> template = new Vector<Integer>();
		
		//assertTrue(! ((MergerProperties)this.getProperties()).getEscapeMapping().isEmpty());
		
		/**
		 * Example2: dipl: " thäs is"
	     *                  01234567
	     *           norm: "thaesis"
	     *                  0123456
	     *                 0->1
	     *                 1->2
	     *                 2->3
	     *                 3->3
	     *                 4->4
	     *                 5->6
	     *                 6->7
		 */
		template.add(1);template.add(2);template.add(3);template.add(3);template.add(4);template.add(6);template.add(7);
		assertEquals(template, this.createBaseTextNormOriginalMapping(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		
	}
	
	/**
	 * Tests the method {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} and checks
	 * if correct equivalence classes for an artificial test case are created.
	 * <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * 	<li>{@value SaltSample#PRIMARY_TEXT_EN}</li>
	 *  <li>Well. {@value SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol> 
	 */
	@Test
	public void testAlignTexts_case1(){
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(sDoc1);
		SaltSample.createTokens(sDoc1);
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. "+SaltSample.PRIMARY_TEXT_EN+" I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();
		
		/*
		for (STextualRelation rel : sDoc2.getSDocumentGraph().getSTextualRelations()){
			System.out.println("Base Token: " + rel.getSToken().getSName()+" : String : "+sDoc2.getSDocumentGraph().getSTextualDSs().get(0).getSText().substring(rel.getSStart(),rel.getSEnd()));
		}
		
		for (STextualRelation rel : sDoc1.getSDocumentGraph().getSTextualRelations()){
			System.out.println("Other Token: " + rel.getSToken().getSName()+" : String : "+sDoc1.getSDocumentGraph().getSTextualDSs().get(0).getSText().substring(rel.getSStart(),rel.getSEnd()));
		}*/
		
		EList<SToken> baseTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> otherTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();
		
		
		System.out.println("Testing Alignment");
		
		//TODO check alignTests
		this.normalizeTextualLayer(sDoc1);
		//System.out.println("Testing Alignment");
		this.normalizeTextualLayer(sDoc2);
		
		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());
		
		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode,SNode> equivalenceMap = new Hashtable<SNode,SNode>();
		assertTrue(this.alignTexts(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0),nonEquivalentTokenInOtherTexts,equivalenceMap));
		
		
		
		// test 3 : the token alignment is correct : the equivalence classes are correct
		int j = 0;
		for (int i = 2 ; i < 11 ; i++){
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0); 
			assertEquals("Base Token "+base.getSName() + 
					" (start: "
					+this.container.getAlignedTokenStart(this.container.getBaseText(), base)+
					") and other token "+otherToken.getSName()+ " (start: "+
					this.container.getAlignedTokenStart(otherText, otherToken)
					+") should be equal.",this.container.getTokenMapping(base, otherText),otherToken);
			j++;
		}
		
		
		
	}
	
	/**
	 * Tests the method {@link #mergeTokens(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} and checks
	 * if correct equivalence classes for an artificial test case are created.
	 * <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * 	<li>{@value SaltSample#PRIMARY_TEXT_EN}</li>
	 *  <li>Well. {@value SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol> 
	 */
	@Test
	public void testMergeTokens_case1(){
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(sDoc1);
		
		SLayer morphLayer = SaltFactory.eINSTANCE.createSLayer();
		morphLayer.setSName("morphology");
		sDoc1.getSDocumentGraph().addSLayer(morphLayer);
		SaltSample.createToken(0,2,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//Is
		SaltSample.createToken(3,7,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//this
		SaltSample.createToken(8,15,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//example
		SaltSample.createToken(16,20,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//more
		SaltSample.createToken(21,32,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//complicated
		SaltSample.createToken(33,37,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//than
		SaltSample.createToken(38,40,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//it
		SaltSample.createToken(41,48,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//supposed
		SaltSample.createToken(49,51,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//to
		//SaltSample.createToken(52,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be?
		SaltSample.createToken(52,54,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be
		SaltSample.createToken(54,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//?
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. "+SaltSample.PRIMARY_TEXT_EN+" I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();
		
		EList<SToken> baseTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> otherTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();
		
		//System.out.println("Testing Alignment");
		System.out.println("DEBUG:!!!!! Merge Tokens 1 START");
		this.normalizeTextualLayer(sDoc1);
		//System.out.println("Testing Alignment");
		this.normalizeTextualLayer(sDoc2);
		
		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());
		
		
		
		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode,SNode> equivalenceMap = new Hashtable<SNode,SNode>();
		assertTrue(this.alignTexts(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0),nonEquivalentTokenInOtherTexts,equivalenceMap));
		System.out.println("Equivalent Tokens of other text:");
		for (SNode tok : equivalenceMap.keySet()){
			System.out.print("\t"+tok.getSName());
		}
		System.out.println("");
		System.out.println("Equivalences:");
		for (SNode node : equivalenceMap.keySet()){
			System.out.println(node.getSName() + " -> " + equivalenceMap.get(node).getSName());
		}
		System.out.println("DEBUG:!!!!! Merge Tokens 1 END");
		assertEquals(otherTextToken.size(),equivalenceMap.size());
		
		System.out.println("DEBUG!!!!!!!!!!!!!!!: Equivalence map size: "+equivalenceMap.size());
		
		// test 3 : the token alignment is correct : the equivalence classes are correct
		int j = 0;
		for (int i = 2 ; i < 11 ; i++){
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0); 
			assertEquals("Base Token "+base.getSName() + 
					" (start: "
					+this.container.getAlignedTokenStart(this.container.getBaseText(), base)+
					") and other token "+otherToken.getSName()+ " (start: "+
					this.container.getAlignedTokenStart(otherText, otherToken)
					+") should be equal.",this.container.getTokenMapping(base, otherText),otherToken);
			j++;
		}
		
		int equivalenceMapSize = equivalenceMap.size();
		// assert that the merging did not change something
		this.mergeTokens(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0), equivalenceMap);
		assertEquals(equivalenceMapSize, equivalenceMap.size());
		
		j = 0;
		for (int i = 2 ; i < 11 ; i++){
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0); 
			assertEquals("Base Token "+base.getSName() + 
					" (start: "
					+this.container.getAlignedTokenStart(this.container.getBaseText(), base)+
					") and other token "+otherToken.getSName()+ " (start: "+
					this.container.getAlignedTokenStart(otherText, otherToken)
					+") should be equal.",this.container.getTokenMapping(base, otherText),otherToken);
			j++;
		}
	}
	
	/**
	 * Tests the method {@link #mergeTokens(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} and checks
	 * if correct equivalence classes for an artificial test case are created.
	 * <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * 	<li>{@value SaltSample#PRIMARY_TEXT_EN}</li>
	 *  <li>Well. {@value SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol> 
	 * 
	 * In this test, the first text is used as base text and one token of the first text is removed
	 */
	@Test
	public void testMergeTokens_case2(){
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(sDoc1);
		
		SLayer morphLayer = SaltFactory.eINSTANCE.createSLayer();
		morphLayer.setSName("morphology");
		sDoc1.getSDocumentGraph().addSLayer(morphLayer);
		SaltSample.createToken(0,2,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//Is
		SaltSample.createToken(3,7,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//this
		SaltSample.createToken(8,15,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//example
		SaltSample.createToken(16,20,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//more
		//SaltSample.createToken(21,32,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//complicated
		SaltSample.createToken(33,37,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//than
		SaltSample.createToken(38,40,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//it
		SaltSample.createToken(41,48,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//supposed
		SaltSample.createToken(49,51,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//to
		//SaltSample.createToken(52,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be?
		SaltSample.createToken(52,54,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be
		SaltSample.createToken(54,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//?
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. "+SaltSample.PRIMARY_TEXT_EN+" I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();
		
		EList<SToken> baseTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> otherTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		
		//System.out.println("Testing Alignment");
		System.out.println("DEBUG:!!!!! MERGE TOKENS 2 START");
		this.normalizeTextualLayer(sDoc1);
		//System.out.println("Testing Alignment");
		this.normalizeTextualLayer(sDoc2);
		
		// test 1 : sDoc2 must be the base document
		this.container.setBaseDocument(sDoc1);
		this.container.setBaseText(sDoc1.getSDocumentGraph().getSTextualDSs().get(0));
		
		
		
		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode,SNode> equivalenceMap = new Hashtable<SNode,SNode>();
		assertTrue(this.alignTexts(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0),nonEquivalentTokenInOtherTexts,equivalenceMap));
		System.out.println("Equivalent Tokens of other text:");
		for (SNode tok : equivalenceMap.keySet()){
			System.out.print("\t"+tok.getSName());
		}
		
		System.out.println("");
		System.out.println("Equivalences:");
		for (SNode node : equivalenceMap.keySet()){
			System.out.println(node.getSName() + " -> " + equivalenceMap.get(node).getSName());
		}
		System.out.println("DEBUG:!!!!! MERGE TOKENS 2 END");
		assertEquals(baseTextToken.size(),equivalenceMap.size());
		
		System.out.println("DEBUG!!!!!!!!!!!!!!!: Equivalence map size: "+equivalenceMap.size());
		
		// test 3 : the token alignment is correct : the equivalence classes are correct
		Hashtable<SNode,SNode> templateMap = new Hashtable<SNode, SNode>();
//		templateMap.put(otherTextToken.get(0), baseTextToken.get(2));
//		templateMap.put(otherTextToken.get(1), baseTextToken.get(3));
//		templateMap.put(otherTextToken.get(2), baseTextToken.get(4));
//		templateMap.put(otherTextToken.get(3), baseTextToken.get(5));
//		templateMap.put(otherTextToken.get(4), baseTextToken.get(7));
//		templateMap.put(otherTextToken.get(5), baseTextToken.get(8));
//		templateMap.put(otherTextToken.get(6), baseTextToken.get(9));
//		templateMap.put(otherTextToken.get(7), baseTextToken.get(10));
//		templateMap.put(otherTextToken.get(8), baseTextToken.get(11));
//		templateMap.put(otherTextToken.get(9), baseTextToken.get(12));
		
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
//		int j = 0;
//		for (int i = 2 ; i < 11 ; i++){
//			SToken base = baseTextToken.get(i);
//			SToken otherToken = otherTextToken.get(j);
//			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0); 
//			assertEquals("Base Token "+base.getSName() + 
//					" (start: "
//					+this.container.getAlignedTokenStart(this.container.getBaseText(), base)+
//					") and other token "+otherToken.getSName()+ " (start: "+
//					this.container.getAlignedTokenStart(otherText, otherToken)
//					+") should be equal.",this.container.getTokenMapping(base, otherText),otherToken);
//			j++;
//		}
		
		int equivalenceMapSize = equivalenceMap.size();
		// assert that the merging did not change something
		this.mergeTokens(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0), equivalenceMap);
		assertTrue(equivalenceMapSize != equivalenceMap.size());
		assertEquals(baseTextToken.size()+1, equivalenceMap.size());
		assertNotNull(equivalenceMap.get(otherTextToken.get(6)));
		
		
		
		/*
		j = 0;
		for (int i = 2 ; i < 11 ; i++){
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getSDocumentGraph().getSTextualDSs().get(0); 
			assertEquals("Base Token "+base.getSName() + 
					" (start: "
					+this.container.getAlignedTokenStart(this.container.getBaseText(), base)+
					") and other token "+otherToken.getSName()+ " (start: "+
					this.container.getAlignedTokenStart(otherText, otherToken)
					+") should be equal.",this.container.getTokenMapping(base, otherText),otherToken);
			j++;
		}*/
		
		
//		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
//		sDoc1.setSId("doc1");
//		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
//		SaltSample.createPrimaryData(sDoc1);
//		
//		SLayer morphLayer = SaltFactory.eINSTANCE.createSLayer();
//		morphLayer.setSName("morphology");
//		sDoc1.getSDocumentGraph().addSLayer(morphLayer);
//		SaltSample.createToken(0,2,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//Is
//		SaltSample.createToken(3,7,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//this
//		SaltSample.createToken(8,15,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//example
//		SaltSample.createToken(16,20,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//more
//		//SaltSample.createToken(21,32,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//complicated
//		SaltSample.createToken(33,37,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//than
//		SaltSample.createToken(38,40,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//it
//		SaltSample.createToken(41,48,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//supposed
//		SaltSample.createToken(49,51,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//to
//		//SaltSample.createToken(52,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be?
//		SaltSample.createToken(52,54,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//be
//		SaltSample.createToken(54,55,sDoc1.getSDocumentGraph().getSTextualDSs().get(0),sDoc1,morphLayer);		//?
//		
//		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
//		sDoc2.setSId("doc2");
//		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
//		sDoc2.getSDocumentGraph().createSTextualDS("Well. "+SaltSample.PRIMARY_TEXT_EN+" I am not sure!");
//		sDoc2.getSDocumentGraph().tokenize();
//		
//		EList<SToken> baseTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();
//		EList<SToken> otherTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
//		
//		//System.out.println("Testing Alignment");
//		
//		this.normalizeTextualLayer(sDoc1);
//		//System.out.println("Testing Alignment");
//		this.normalizeTextualLayer(sDoc2);
//		this.container.setBaseDocument(sDoc1);
//		this.container.setBaseText(sDoc1.getSDocumentGraph().getSTextualDSs().get(0));
//		
//		// test 1 : align must return true
//		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
//		Hashtable<SNode,SNode> equivalenceMap = new Hashtable<SNode,SNode>();
//		assertTrue(this.alignTexts(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0),nonEquivalentTokenInOtherTexts,equivalenceMap));
//		
//		System.out.println("MergeTokens1: Equivalent Tokens of other text:");
//		for (SNode tok : equivalenceMap.keySet()){
//			System.out.print("\t"+tok.getSName());
//		}
//		
//		System.out.println("");
//		
//		System.out.println("Equivalences:");
//		for (SNode node : equivalenceMap.keySet()){
//			System.out.println(node.getSName() + " -> " + equivalenceMap.get(node).getSName());
//		}
//		
//		// test 2: There are 9 token equivalences
//		assertEquals(baseTextToken.size()-1,equivalenceMap.size());
//		
//		// test 3 : the token alignment is correct : the equivalence classes are correct
//		
//		Hashtable<SNode,SNode> templateMap = new Hashtable<SNode, SNode>();
//		templateMap.put(otherTextToken.get(2), baseTextToken.get(0));
//		templateMap.put(otherTextToken.get(3), baseTextToken.get(1));
//		templateMap.put(otherTextToken.get(4), baseTextToken.get(2));
//		templateMap.put(otherTextToken.get(5), baseTextToken.get(3));
//		templateMap.put(otherTextToken.get(7), baseTextToken.get(4));
//		templateMap.put(otherTextToken.get(8), baseTextToken.get(5));
//		templateMap.put(otherTextToken.get(9), baseTextToken.get(6));
//		templateMap.put(otherTextToken.get(10), baseTextToken.get(7));
//		templateMap.put(otherTextToken.get(11), baseTextToken.get(8));
//		templateMap.put(otherTextToken.get(12), baseTextToken.get(9));
//		
//		for (SToken baseTextTok : baseTextToken){
//			SToken otherTextTok = this.container.getTokenMapping(baseTextTok, sDoc2.getSDocumentGraph().getSTextualDSs().get(0));
//			assertNotNull("Base text token "+baseTextTok.getSName()+" does not have an equivalent",otherTextTok);
//		}
//		
//		assertEquals(templateMap, equivalenceMap);
//		
//		int equivalenceMapSize = equivalenceMap.size();
//		// assert that the merging added tokens.
//		this.mergeTokens(sDoc1.getSDocumentGraph().getSTextualDSs().get(0), sDoc2.getSDocumentGraph().getSTextualDSs().get(0), equivalenceMap);
//		assertTrue(equivalenceMapSize != equivalenceMap.size());
//		assertEquals(baseTextToken.size(), equivalenceMap.size());
	}
	
	/**
	 * Tests the method {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} against some real data excerpts and
	 * checks if the creation of equivalence classes works correctly.<br/>
	 * This test creates 2 documents. Document 1 contains 2 texts German of the Ridges corpus (dipl and norm).
	 * Document 2 contains 1 text also from the Ridges corpus, but in its treetagger representation (without whitespaces). 
	 */
	@Test
	public void testAlignTexts_caseRidges1(){
		// create document 1
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		String norm= "Die deutschen Namen, die dann oft daneben stehen, tragen meist ein solches Gepräge der Unklarheit, dass sie jeden zurückschrecken müssen, der sie statt der lateinischen einführen möchte.";
		String dipl= "Die deutſchen Namen, die dann oft daneben ſtehen, tragen meiſt ein ſolches Gepräge der Unklarheit, daſz ſie jeden zurückſchrecken müſſen, der ſie ſtatt der lateiniſchen einführen möchte.";
		STextualDS sTextDS1= sDoc1.getSDocumentGraph().createSTextualDS(norm);
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 0, 3);		//Die
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 4, 13);	//deutschen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 14, 19);	//Namen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 19, 20);	//,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 21, 24);	//die
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 25, 29);	//dann
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 30, 33);	//oft
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 34, 41);	//daneben
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 42, 48);	//stehen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 48, 49);   //,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 50, 56);	//tragen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 57, 62);	//meist
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 63, 66);	//ein
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 67, 74);	//solches
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 75, 82);	//Gepräge
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 83, 86);	//der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 87, 97);	//Unklarheit
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 97, 98);	//,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 99, 103);	//dass
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 104, 107);	//sie
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 108, 113);	//jeden
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 114, 129);	//zurückschrecken
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 130, 136);	//müssen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 136, 137);	//,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 138, 141);	//der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 142, 145);	//sie
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 146, 151);	//statt
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 152, 155);	//der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 156, 168);	//lateinischen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 169, 178);	//einführen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 179, 185);	//möchte
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 185, 186);	//.
		
		
		STextualDS sTextDS2= sDoc1.getSDocumentGraph().createSTextualDS(dipl);
		// tokenize : Die Deutschen Unklarheit möchte
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 0, 3);		//Die
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 4, 13);	//deutschen
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 87, 97);	//Unklarheit
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 179, 185);	//möchte
		
		//create document2
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		String tokenizer= "DiedeutschenNamen,diedannoftdanebenstehen,tragenmeisteinsolchesGeprägederUnklarheit,dasssiejedenzurückschreckenmüssen,dersiestattderlateinischeneinführenmöchte.";
		STextualDS sTextDS3= sDoc2.getSDocumentGraph().createSTextualDS(tokenizer);
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 0, 3);		//Die
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 3, 12);	//deutschen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 12, 17);	//Namen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 17, 18);	//,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 18, 21);	//die
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 21, 25);	//dann
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 25, 28);	//oft
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 28, 35);	//daneben
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 35, 41);	//stehen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 41, 42);	//,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 42, 48);	//tragen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 48, 53);	//meist
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 53, 56);	//ein
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 56, 63);	//solches
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 63, 70);	//Gepräge
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 70, 73);	//der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 73, 83);	//Unklarheit
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 83, 84);	//s
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 84, 88);	//dass
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 88, 91);	//sie
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 91, 96);	//jeden
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 96, 111);	//zurückschrecken
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 111, 117);	//müssen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 117, 118);	//,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 118, 121);	//der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 121, 124);	//sie
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 124, 129);	//statt
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 129, 132);	//der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 132, 144);	//lateinischen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 144, 153);	//einführen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 153, 159);	//möchte
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 159, 160);	//.
		
		//TODO call align method (or whatever)
		//MergerMapper mm = new MergerMapper();
		MappingSubject sub1 = new MappingSubject();
		MappingSubject sub2 = new MappingSubject();
		
		sub1.setSElementId(sDoc1.getSElementId());
		sub2.setSElementId(sDoc2.getSElementId());
		
		getFixture().getMappingSubjects().add(sub1);
		getFixture().getMappingSubjects().add(sub2);
		
		
		this.mergeSDocumentGraph();
		
		//test the result
		
		// assert base document = sDoc1 & baseText = norm
		assertEquals("The base Document should be document 1. but it is not!",sDoc1, this.container.getBaseDocument());
		assertEquals("The base text should be norm of document 1. but it is not!",sTextDS1, this.container.getBaseText());
//		System.out.println("Base text is: "+this.container.getBaseText().getSName());
//		System.out.println("Base text SText is: "+this.container.getBaseText().getSText());
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
		
		STextualDS sTextDS1= sDoc1.getSDocumentGraph().createSTextualDS("boat");
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
	 * Checks, that algorithm chooses the expected base document automatically.
	 */
	@Test
	public void testChooseBaseDocument(){
		SCorpusGraph g1= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_1= g1.createSDocument(URI.createURI("/c1/d1"));
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_1.getSDocumentGraph().createSTextualDS("a sample text");
		MappingSubject subj_1= new MappingSubject();
		subj_1.setSElementId(d1_1.getSElementId());
		getFixture().getMappingSubjects().add(subj_1);
		
		SCorpusGraph g2= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_2= g2.createSDocument(URI.createURI("/c1/d1"));
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_2.getSDocumentGraph().createSTextualDS("This is a sample text.");
		MappingSubject subj_2= new MappingSubject();
		subj_2.setSElementId(d1_2.getSElementId());
		getFixture().getMappingSubjects().add(subj_2);
		
		SCorpusGraph g3= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_3= g3.createSDocument(URI.createURI("/c1/d1"));
		d1_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_3.getSDocumentGraph().createSTextualDS("a sample");
		MappingSubject subj_3= new MappingSubject();
		subj_3.setSElementId(d1_3.getSElementId());
		getFixture().getMappingSubjects().add(subj_3);
		
		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()){
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
				SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
				this.normalizeTextualLayer(sDoc);
			}
		}
		
		MappingSubject result= this.chooseBaseDocument();
		assertEquals(subj_2, result);
		assertEquals(d1_2,this.container.getBaseDocument());
	}
	
	/**
	 * Checks, that algorithm chooses the expected base document which was set manually.
	 */
	@Test
	public void testChooseBaseDocument_manual(){
		SCorpusGraph g1= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_1= g1.createSDocument(URI.createURI("/c1/d1"));
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_1.getSDocumentGraph().createSTextualDS("a sample text");
		MappingSubject subj_1= new MappingSubject();
		subj_1.setSElementId(d1_1.getSElementId());
		getFixture().getMappingSubjects().add(subj_1);
		
		SCorpusGraph g2= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_2= g2.createSDocument(URI.createURI("/c1/d1"));
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_2.getSDocumentGraph().createSTextualDS("This is a sample text.");
		MappingSubject subj_2= new MappingSubject();
		subj_2.setSElementId(d1_2.getSElementId());
		getFixture().getMappingSubjects().add(subj_2);
		
		SCorpusGraph g3= SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_3= g3.createSDocument(URI.createURI("/c1/d1"));
		d1_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_3.getSDocumentGraph().createSTextualDS("a sample");
		MappingSubject subj_3= new MappingSubject();
		subj_3.setSElementId(d1_3.getSElementId());
		getFixture().getMappingSubjects().add(subj_3);
		
		PepperModuleProperty prop= this.getFixture().getProperties().getProperty(MergerProperties.PROP_FIRST_AS_BASE);
		prop.setValue(Boolean.TRUE);
		
		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()){
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
				SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
				this.normalizeTextualLayer(sDoc);
			}
		}
		
		getFixture().setBaseCorpusStructure(g3);
		
		MappingSubject result= this.chooseBaseDocument();
		
		assertEquals(subj_3.getSElementId(), result.getSElementId());
		assertEquals(d1_3,this.container.getBaseDocument());
	}
}
