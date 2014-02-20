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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
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
		//@Mario, das soll so, der erste Test ist, ob wir mit leeren Texten zurecht kommen
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
	 * Tests the method {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)} and checks
	 * if correct equivalence classes for an artificial test case are created.
	 * <br/>
	 * The test case uses two texts:
	 * 
	 * <ol>
	 * 	<li>{@link SaltSample#PRIMARY_TEXT_EN}</li>
	 *  <li>Well. {@link SaltSample#PRIMARY_TEXT_EN} I am not sure!</li>
	 * </ol> 
	 */
	@Test
	public void testAlignTexts_case1(){
		SDocument sDoc1= SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createTokens(sDoc1);
		
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. "+SaltSample.PRIMARY_TEXT_EN+" I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();
		
		//TODO check alignTests
		
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
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		String norm= "Die deutschen Namen, die dann oft daneben stehen, tragen meist ein solches Gepräge der Unklarheit, dass sie jeden zurückschrecken müssen, der sie statt der lateinischen einführen möchte.";
		String dipl= "Die deutſchen Namen, die dann oft daneben ſtehen, tragen meiſt ein ſolches Gepräge der Unklarheit, daſz ſie jeden zurückſchrecken müſſen, der ſie ſtatt der lateiniſchen einführen möchte.";
		STextualDS sTextDS1= sDoc1.getSDocumentGraph().createSTextualDS(norm);
		//TODO tokenize
		
		STextualDS sTextDS2= sDoc1.getSDocumentGraph().createSTextualDS(dipl);
		//TODO tokenize
		
		//create document2
		SDocument sDoc2= SaltFactory.eINSTANCE.createSDocument();
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
		//test the result
	}

}
