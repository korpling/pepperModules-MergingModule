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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperty;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.samples.SampleGenerator;

public class MergerMapper_baseTest extends MergerMapper {

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
	 * Tests the normalization function with different texts.
	 * <ol>
	 * <li>empty texts</li>
	 * <li>english text containing whitespaces and punctuations</li>
	 * <li>german text containing whitespaces and umlauts</li>
	 * </ol>
	 */
	@Test
	public void testNormalize() {
		String origText = "";
		String normText = "";

		// test 1
		SDocument doc1 = SaltFactory.eINSTANCE.createSDocument();
		doc1.setSId("doc1");
		doc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		doc1.getSDocumentGraph().createSTextualDS(origText);
		this.normalizeTextualLayer(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);

		// test2
		origText = "Is this sample more complicated, than it appears to be?";
		normText = "Isthissamplemorecomplicated,thanitappearstobe?";
		doc1.getSDocumentGraph().getSTextualDSs().get(0).setSText(origText);
		this.normalizeTextualLayer(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);

		// test3
		origText = "Das wäre überaus schön";
		normText = "Daswaereueberausschoen";

		doc1.getSDocumentGraph().getSTextualDSs().get(0).setSText(origText);
		this.normalizeTextualLayer(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
		this.container.finishDocument(doc1);
	}

	@Test
	public void testIndexOfOmitChars() {
		String baseText = "This,isasmallExample!";
		String otherText = "Thisisno";

		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties) getProperties()).getPunctuations()), -1);

		otherText = "This;is";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties) getProperties()).getPunctuations()), 0);

		baseText = "Thisisnosmallexample.Itisasmallerexample!";
		otherText = "exampleItis";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties) getProperties()).getPunctuations()), 13);

		otherText = ".exampleItis";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties) getProperties()).getPunctuations()), 13);

		baseText = "Thisisnosmallexampl.Itisasmallerexampl";
		otherText = "example";
		assertEquals(this.indexOfOmitChars(baseText, otherText, false, ((MergerProperties) getProperties()).getPunctuations()), -1);

	}

	/**
	 *
	 * 
	 */
	@Test
	public void textCreateBaseTextNormOriginalMapping() {
		String origText = " thäs is";

		// test 1
		SDocument doc1 = SaltFactory.eINSTANCE.createSDocument();
		doc1.setSId("doc1");
		doc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		doc1.getSDocumentGraph().createSTextualDS(origText);
		this.normalizeTextualLayer(doc1);

		List<Integer> template = new Vector<Integer>();

		/**
		 * Example2: dipl: " thäs is" 01234567 norm: "thaesis" 0123456 0->1 1->2
		 * 2->3 3->3 4->4 5->6 6->7
		 */
		template.add(1);
		template.add(2);
		template.add(3);
		template.add(3);
		template.add(4);
		template.add(6);
		template.add(7);
		assertEquals(template, this.createBaseTextNormOriginalMapping(doc1.getSDocumentGraph().getSTextualDSs().get(0)));
	}

	/**
	 * Tests the method
	 * {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)}
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
	public void testAlignTexts_case1() {
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);

		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		sDoc2.getSDocumentGraph().createSTextualDS("Well. " + SampleGenerator.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getSDocumentGraph().tokenize();

		EList<SToken> baseTextToken = sDoc2.getSDocumentGraph().getSortedSTokenByText();
		EList<SToken> otherTextToken = sDoc1.getSDocumentGraph().getSortedSTokenByText();

		// TODO check alignTests
		this.normalizeTextualLayer(sDoc1);
		this.normalizeTextualLayer(sDoc2);

		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc2.getSDocumentGraph().getSTextualDSs().get(0), sDoc1.getSDocumentGraph().getSTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));

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
	}

	
	/**
	 * Tests the method
	 * {@link #alignTexts(de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS, de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS)}
	 * against some real data excerpts and checks if the creation of equivalence
	 * classes works correctly.<br/>
	 * This test creates 2 documents. Document 1 contains 2 texts German of the
	 * Ridges corpus (dipl and norm). Document 2 contains 1 text also from the
	 * Ridges corpus, but in its treetagger representation (without
	 * whitespaces).
	 */
	@Test
	public void testAlignTexts_caseRidges1() {
		// create document 1
		SDocument sDoc1 = SaltFactory.eINSTANCE.createSDocument();
		sDoc1.setSId("doc1");
		sDoc1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		String norm = "Die deutschen Namen, die dann oft daneben stehen, tragen meist ein solches Gepräge der Unklarheit, dass sie jeden zurückschrecken müssen, der sie statt der lateinischen einführen möchte.";
		String dipl = "Die deutſchen Namen, die dann oft daneben ſtehen, tragen meiſt ein ſolches Gepräge der Unklarheit, daſz ſie jeden zurückſchrecken müſſen, der ſie ſtatt der lateiniſchen einführen möchte.";
		STextualDS sTextDS1 = sDoc1.getSDocumentGraph().createSTextualDS(norm);
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 0, 3); // Die
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 4, 13); // deutschen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 14, 19); // Namen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 19, 20); // ,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 21, 24); // die
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 25, 29); // dann
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 30, 33); // oft
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 34, 41); // daneben
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 42, 48); // stehen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 48, 49); // ,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 50, 56); // tragen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 57, 62); // meist
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 63, 66); // ein
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 67, 74); // solches
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 75, 82); // Gepräge
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 83, 86); // der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 87, 97); // Unklarheit
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 97, 98); // ,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 99, 103); // dass
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 104, 107); // sie
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 108, 113); // jeden
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 114, 129); // zurückschrecken
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 130, 136); // müssen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 136, 137); // ,
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 138, 141); // der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 142, 145); // sie
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 146, 151); // statt
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 152, 155); // der
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 156, 168); // lateinischen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 169, 178); // einführen
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 179, 185); // möchte
		sDoc1.getSDocumentGraph().createSToken(sTextDS1, 185, 186); // .

		STextualDS sTextDS2 = sDoc1.getSDocumentGraph().createSTextualDS(dipl);
		// tokenize : Die Deutschen Unklarheit möchte
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 0, 3); // Die
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 4, 13); // deutschen
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 87, 97); // Unklarheit
		sDoc1.getSDocumentGraph().createSToken(sTextDS2, 179, 185); // möchte

		// create document2
		SDocument sDoc2 = SaltFactory.eINSTANCE.createSDocument();
		sDoc2.setSId("doc2");
		sDoc2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		String tokenizer = "DiedeutschenNamen,diedannoftdanebenstehen,tragenmeisteinsolchesGeprägederUnklarheit,dasssiejedenzurückschreckenmüssen,dersiestattderlateinischeneinführenmöchte.";
		STextualDS sTextDS3 = sDoc2.getSDocumentGraph().createSTextualDS(tokenizer);
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 0, 3); // Die
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 3, 12); // deutschen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 12, 17); // Namen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 17, 18); // ,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 18, 21); // die
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 21, 25); // dann
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 25, 28); // oft
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 28, 35); // daneben
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 35, 41); // stehen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 41, 42); // ,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 42, 48); // tragen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 48, 53); // meist
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 53, 56); // ein
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 56, 63); // solches
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 63, 70); // Gepräge
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 70, 73); // der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 73, 83); // Unklarheit
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 83, 84); // s
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 84, 88); // dass
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 88, 91); // sie
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 91, 96); // jeden
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 96, 111); // zurückschrecken
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 111, 117); // müssen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 117, 118); // ,
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 118, 121); // der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 121, 124); // sie
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 124, 129); // statt
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 129, 132); // der
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 132, 144); // lateinischen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 144, 153); // einführen
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 153, 159); // möchte
		sDoc2.getSDocumentGraph().createSToken(sTextDS3, 159, 160); // .

		// TODO call align method (or whatever)
		// MergerMapper mm = new MergerMapper();
		MappingSubject sub1 = new MappingSubject();
		MappingSubject sub2 = new MappingSubject();

		sub1.setSElementId(sDoc1.getSElementId());
		sub2.setSElementId(sDoc2.getSElementId());

		getFixture().getMappingSubjects().add(sub1);
		getFixture().getMappingSubjects().add(sub2);

		this.mergeSDocumentGraph();

		// assert base document = sDoc1 & baseText = norm
		assertEquals("The base Document should be document 1. but it is not!", sDoc1, this.container.getBaseDocument());
		assertEquals("The base text should be norm of document 1. but it is not!", sTextDS1, this.container.getBaseText());
	}

	/**
	 * Checks, that algorithm chooses the expected base document automatically.
	 */
	@Test
	public void testChooseBaseDocument() {
		SCorpusGraph g1 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_1 = g1.createSDocument(URI.createURI("/c1/d1"));
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_1.getSDocumentGraph().createSTextualDS("a sample text");
		MappingSubject subj_1 = new MappingSubject();
		subj_1.setSElementId(d1_1.getSElementId());
		getFixture().getMappingSubjects().add(subj_1);

		SCorpusGraph g2 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_2 = g2.createSDocument(URI.createURI("/c1/d1"));
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_2.getSDocumentGraph().createSTextualDS("This is a sample text.");
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setSElementId(d1_2.getSElementId());
		getFixture().getMappingSubjects().add(subj_2);

		SCorpusGraph g3 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_3 = g3.createSDocument(URI.createURI("/c1/d1"));
		d1_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_3.getSDocumentGraph().createSTextualDS("a sample");
		MappingSubject subj_3 = new MappingSubject();
		subj_3.setSElementId(d1_3.getSElementId());
		getFixture().getMappingSubjects().add(subj_3);

		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()) {
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
				this.normalizeTextualLayer(sDoc);
			}
		}

		MappingSubject result = this.chooseBaseDocument();
		assertEquals(subj_2, result);
		assertEquals(d1_2, this.container.getBaseDocument());
	}

	/**
	 * Checks, that algorithm chooses the expected base document which was set
	 * manually.
	 */
	@Test
	public void testChooseBaseDocument_manual() {
		SCorpusGraph g1 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_1 = g1.createSDocument(URI.createURI("/c1/d1"));
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_1.getSDocumentGraph().createSTextualDS("a sample text");
		MappingSubject subj_1 = new MappingSubject();
		subj_1.setSElementId(d1_1.getSElementId());
		getFixture().getMappingSubjects().add(subj_1);

		SCorpusGraph g2 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_2 = g2.createSDocument(URI.createURI("/c1/d1"));
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_2.getSDocumentGraph().createSTextualDS("This is a sample text.");
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setSElementId(d1_2.getSElementId());
		getFixture().getMappingSubjects().add(subj_2);

		SCorpusGraph g3 = SaltFactory.eINSTANCE.createSCorpusGraph();
		SDocument d1_3 = g3.createSDocument(URI.createURI("/c1/d1"));
		d1_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		d1_3.getSDocumentGraph().createSTextualDS("a sample");
		MappingSubject subj_3 = new MappingSubject();
		subj_3.setSElementId(d1_3.getSElementId());
		getFixture().getMappingSubjects().add(subj_3);

		PepperModuleProperty prop = this.getFixture().getProperties().getProperty(MergerProperties.PROP_FIRST_AS_BASE);
		prop.setValue(Boolean.TRUE);

		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()) {
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
				this.normalizeTextualLayer(sDoc);
			}
		}

		getFixture().setBaseCorpusStructure(g3);

		MappingSubject result = this.chooseBaseDocument();

		assertEquals(subj_3.getSElementId(), result.getSElementId());
		assertEquals(d1_3, this.container.getBaseDocument());
	}
}
