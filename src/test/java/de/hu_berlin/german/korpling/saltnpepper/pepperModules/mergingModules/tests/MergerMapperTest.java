package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerMapper;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
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
		setFixture(new MergerMapper());
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
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SDocument sDoc3= SaltFactory.eINSTANCE.createSDocument();
		sDoc3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		
		DocumentStatusPair pair1= new DocumentStatusPair(sDoc1);
		getFixture().getDocumentPairs().add(pair1);
		DocumentStatusPair pair2= new DocumentStatusPair(sDoc2);
		getFixture().getDocumentPairs().add(pair2);
		DocumentStatusPair pair3= new DocumentStatusPair(sDoc3);
		getFixture().getDocumentPairs().add(pair3);
		
			SaltSample.createPrimaryData(sDoc1);
			SaltSample.createTokens(sDoc1);
		SaltSample.createAnaphoricAnnotations(sDoc1);
			
			SaltSample.createPrimaryData(sDoc2);
			SaltSample.createTokens(sDoc2);
			SaltSample.createSyntaxStructure(sDoc2);
		SaltSample.createSyntaxAnnotations(sDoc2);
		
			SaltSample.createPrimaryData(sDoc3);
			SaltSample.createTokens(sDoc3);
		SaltSample.createMorphologyAnnotations(sDoc3);
		
		SDocument template= SaltFactory.eINSTANCE.createSDocument();
			template.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
			SaltSample.createPrimaryData(template);
			SaltSample.createTokens(template);
			SaltSample.createSyntaxStructure(template);
		SaltSample.createAnaphoricAnnotations(template);
		SaltSample.createSyntaxAnnotations(template);
		SaltSample.createMorphologyAnnotations(template);
		
		getFixture().map();
		
		assertEquals(DOCUMENT_STATUS.COMPLETED, pair1.status);
		assertEquals(DOCUMENT_STATUS.DELETED, pair2.status);
		assertEquals(DOCUMENT_STATUS.DELETED, pair3.status);
		
		assertNotNull(pair1.sDocument.getSDocumentGraph());
		assertNull(pair2.sDocument.getSDocumentGraph());
		assertNull(pair3.sDocument.getSDocumentGraph());
		
		assertEquals(template.getSDocumentGraph().getSNodes().size(), pair1.sDocument.getSDocumentGraph().getSNodes().size());
		assertEquals(template.getSDocumentGraph().getSRelations().size(), pair1.sDocument.getSDocumentGraph().getSRelations().size());
		
		assertEquals(template.getSDocumentGraph().getSTokens().size(), pair1.sDocument.getSDocumentGraph().getSTokens().size());
		assertEquals(template.getSDocumentGraph().getSSpans().size(), pair1.sDocument.getSDocumentGraph().getSSpans().size());
		assertEquals(template.getSDocumentGraph().getSStructures().size(), pair1.sDocument.getSDocumentGraph().getSStructures().size());
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
		fail("check origText vs normText");
		
		
		//test2 
		origText= "Is this sample more complicated, than it appears to be?";
		normText= "Isthissamplemorecomplicatedthanitappearstobe";
		fail("check origText vs normText");
		
		//test3 
		origText= "Das wäre überaus schön";
		normText= "Daswaereueberausschoen";
		fail("check origText vs normText");
	}
	/**
	 * Tests the method {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} against some real data excerpts and
	 * checks if the creation of equivalence classes works correctly. 
	 */
	@Test
	public void testAlignTexts_case1(){
		
	}

}
