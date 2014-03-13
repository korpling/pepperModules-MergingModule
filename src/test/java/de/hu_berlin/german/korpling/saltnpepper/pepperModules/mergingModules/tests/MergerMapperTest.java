package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpanningRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
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
		this.mapSDocument();
		
		
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
		
		// the count of tokens in sDoc1 must be the same as before!
		assertEquals(template.getSDocumentGraph().getSTokens().size(),     sDoc1.getSDocumentGraph().getSTokens().size());
		
		assertEquals(template.getSDocumentGraph().getSNodes().size(),     sDoc1.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), sDoc1.getSDocumentGraph().getSRelations().size());
		
		assertEquals(template.getSDocumentGraph().getSTokens().size(),     sDoc1.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(),      sDoc1.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSStructures().size(), sDoc1.getSDocumentGraph().getSStructures().size());
		
		assertNotNull(sDoc1.getSDocumentGraph());
		assertNull(sDoc2.getSDocumentGraph());
		assertNull(sDoc3.getSDocumentGraph());
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
		
		this.isTestMode = false;
		this.mapSDocument();
		
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
		
		//System.out.println("Testing Alignment");
		
		
		
		//TODO check alignTests
		this.normalizeTextualLayer(sDoc1);
		//System.out.println("Testing Alignment");
		this.normalizeTextualLayer(sDoc2);
		
		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());
		
		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		assertTrue(this.alignTexts(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0),nonEquivalentTokenInOtherTexts));
		
		
		
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
		
		
		this.mapSDocument();
		
		//test the result
		
		// assert base document = sDoc1 & baseText = norm
		assertEquals("The base Document should be document 1. but it is not!",sDoc1, this.container.getBaseDocument());
		assertEquals("The base text should be norm of document 1. but it is not!",sTextDS1, this.container.getBaseText());
		System.out.println("Base text is: "+this.container.getBaseText().getSName());
		System.out.println("Base text SText is: "+this.container.getBaseText().getSText());
	}
	
	@Test
	public void testMovingLabels() throws Exception {
		SToken tok1 = SaltFactory.eINSTANCE.createSToken();
		SToken tok2 = SaltFactory.eINSTANCE.createSToken();
		
		// identical annotation --> no need to copy
		SAnnotation anno1 = SaltFactory.eINSTANCE.createSAnnotation();
		anno1.setName("anno1");
		anno1.setSValue("annotext1");
		
		// new annotation --> should be copied
		SAnnotation anno2 = SaltFactory.eINSTANCE.createSAnnotation();
		anno2.setName("anno2");
		anno2.setSValue("annotext2");
		
		// annotation with different value --> name will be changed
		SAnnotation anno3 = SaltFactory.eINSTANCE.createSAnnotation();
		anno3.setName("anno2");
		anno3.setSValue("annotext22");

		tok1.addSAnnotation(anno1);
		tok1.addSAnnotation(anno2);
		tok2.addSAnnotation(anno3);
		
		MergerMapper mm = new MergerMapper();
		mm.moveAllLabels(tok1, tok2, true);

		assertEquals("annotext1", tok2.getSAnnotation("anno1").getSValueSTEXT());
		assertEquals("annotext22", tok2.getSAnnotation("anno2")
				.getSValueSTEXT());
		assertEquals("annotext2",
				tok2.getSAnnotation("anno2" + MergerMapper.LABEL_NAME_EXTENSION)
						.getSValueSTEXT());
	}
	
	@Test
	public void testMetaAnnotationMove() throws Exception {
		MergerMapper mm = new MergerMapper();
		SCorpus sCorp = SaltFactory.eINSTANCE.createSCorpus();
		SDocument sDoc = SaltFactory.eINSTANCE.createSDocument();
		
		SMetaAnnotation meta = SaltFactory.eINSTANCE.createSMetaAnnotation();
		String annoName = "metaAnno";
		String annoValue= "metaValue";
		meta.setName(annoName);
		meta.setValue(annoValue);
		
		sCorp.addSMetaAnnotation(meta);
		
		mm.moveAllLabels(sCorp, sDoc, true);
		
		assertEquals(1, sDoc.getSMetaAnnotations().size());
		assertNotNull(sDoc.getSMetaAnnotation(annoName));
		assertEquals(annoName, sDoc.getSMetaAnnotation(annoName).getSName());
		assertEquals(annoValue, sDoc.getSMetaAnnotation(annoName).getValue());
		
		SMetaAnnotation meta2 = SaltFactory.eINSTANCE.createSMetaAnnotation();
		String annoName2 = "metaAnno";
		String annoValue2= "metaValue_1";
		meta2.setName(annoName2);
		meta2.setValue(annoValue2);
		
		sCorp.addSMetaAnnotation(meta2);
		
		mm.moveAllLabels(sCorp, sDoc, true);
		
		assertEquals(2, sDoc.getSMetaAnnotations().size());
		assertNotNull(sDoc.getSMetaAnnotation(annoName2));
		assertEquals(annoName2 + MergerMapper.LABEL_NAME_EXTENSION, sDoc.getSMetaAnnotation(annoName2).getSName());
		assertEquals(annoValue2, sDoc.getSMetaAnnotation(annoName2).getValue());
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
		System.out.println("id: "+ d1_1.getSElementId());
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
		
		//TODO what is necessary, to run test
		
		MappingSubject result= this.chooseBaseDocument();
		assertEquals(subj_2, result);
	}
}
