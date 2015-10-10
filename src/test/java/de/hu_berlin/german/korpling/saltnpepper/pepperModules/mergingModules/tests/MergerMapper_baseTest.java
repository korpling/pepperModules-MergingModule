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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.corpus_tools.pepper.modules.MappingSubject;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.samples.SampleGenerator;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;

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
		SDocument doc1 = SaltFactory.createSDocument();
		doc1.setId("doc1");
		doc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		doc1.getDocumentGraph().createTextualDS(origText);
		this.normalizePrimaryTexts(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getDocumentGraph().getTextualDSs().get(0)));
		this.container.finishDocument(doc1);

		// test2
		origText = "Is this sample more complicated, than it appears to be?";
		normText = "Isthissamplemorecomplicated,thanitappearstobe?";
		doc1.getDocumentGraph().getTextualDSs().get(0).setText(origText);
		this.normalizePrimaryTexts(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getDocumentGraph().getTextualDSs().get(0)));
		this.container.finishDocument(doc1);

		// test3
		origText = "Das wäre überaus schön";
		normText = "Daswaereueberausschoen";

		doc1.getDocumentGraph().getTextualDSs().get(0).setText(origText);
		this.normalizePrimaryTexts(doc1);

		assertEquals(normText, this.container.getNormalizedText(doc1.getDocumentGraph().getTextualDSs().get(0)));
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
		SDocument doc1 = SaltFactory.createSDocument();
		doc1.setId("doc1");
		doc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		doc1.getDocumentGraph().createTextualDS(origText);
		this.normalizePrimaryTexts(doc1);

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
		template.add(8);
		assertEquals(template, this.createBaseTextNormOriginalMapping(doc1.getDocumentGraph().getTextualDSs().get(0)));
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
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("doc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		SampleGenerator.createPrimaryData(sDoc1);
		SampleGenerator.createTokens(sDoc1);

		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("doc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		sDoc2.getDocumentGraph().createTextualDS("Well. " + SampleGenerator.PRIMARY_TEXT_EN + " I am not sure!");
		sDoc2.getDocumentGraph().tokenize();

		List<SToken> baseTextToken = sDoc2.getDocumentGraph().getSortedTokenByText();
		List<SToken> otherTextToken = sDoc1.getDocumentGraph().getSortedTokenByText();

		// TODO check alignTests
		this.normalizePrimaryTexts(sDoc1);
		this.normalizePrimaryTexts(sDoc2);

		// test 1 : sDoc2 must be the base document
		assertEquals(sDoc2, this.container.getBaseDocument());

		// test 2 : align must return true
		HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
		Hashtable<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
		assertTrue(this.alignTexts(sDoc2.getDocumentGraph().getTextualDSs().get(0), sDoc1.getDocumentGraph().getTextualDSs().get(0), nonEquivalentTokenInOtherTexts, equivalenceMap));

		// test 3 : the token alignment is correct : the equivalence classes are
		// correct
		int j = 0;
		for (int i = 2; i < 11; i++) {
			SToken base = baseTextToken.get(i);
			SToken otherToken = otherTextToken.get(j);
			STextualDS otherText = sDoc1.getDocumentGraph().getTextualDSs().get(0);
			assertEquals("Base Token " + base.getName() + " and other token " + otherToken.getName() + " (start: " + this.container.getAlignedTokenStart(otherText, otherToken) + ") should be equal.", this.container.getTokenMapping(base, otherText), otherToken);
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
		SDocument sDoc1 = SaltFactory.createSDocument();
		sDoc1.setId("doc1");
		sDoc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		String norm = "Die deutschen Namen, die dann oft daneben stehen, tragen meist ein solches Gepräge der Unklarheit, dass sie jeden zurückschrecken müssen, der sie statt der lateinischen einführen möchte.";
		String dipl = "Die deutſchen Namen, die dann oft daneben ſtehen, tragen meiſt ein ſolches Gepräge der Unklarheit, daſz ſie jeden zurückſchrecken müſſen, der ſie ſtatt der lateiniſchen einführen möchte.";
		STextualDS sTextDS1 = sDoc1.getDocumentGraph().createTextualDS(norm);
		sDoc1.getDocumentGraph().createToken(sTextDS1, 0, 3); // Die
		sDoc1.getDocumentGraph().createToken(sTextDS1, 4, 13); // deutschen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 14, 19); // Namen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 19, 20); // ,
		sDoc1.getDocumentGraph().createToken(sTextDS1, 21, 24); // die
		sDoc1.getDocumentGraph().createToken(sTextDS1, 25, 29); // dann
		sDoc1.getDocumentGraph().createToken(sTextDS1, 30, 33); // oft
		sDoc1.getDocumentGraph().createToken(sTextDS1, 34, 41); // daneben
		sDoc1.getDocumentGraph().createToken(sTextDS1, 42, 48); // stehen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 48, 49); // ,
		sDoc1.getDocumentGraph().createToken(sTextDS1, 50, 56); // tragen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 57, 62); // meist
		sDoc1.getDocumentGraph().createToken(sTextDS1, 63, 66); // ein
		sDoc1.getDocumentGraph().createToken(sTextDS1, 67, 74); // solches
		sDoc1.getDocumentGraph().createToken(sTextDS1, 75, 82); // Gepräge
		sDoc1.getDocumentGraph().createToken(sTextDS1, 83, 86); // der
		sDoc1.getDocumentGraph().createToken(sTextDS1, 87, 97); // Unklarheit
		sDoc1.getDocumentGraph().createToken(sTextDS1, 97, 98); // ,
		sDoc1.getDocumentGraph().createToken(sTextDS1, 99, 103); // dass
		sDoc1.getDocumentGraph().createToken(sTextDS1, 104, 107); // sie
		sDoc1.getDocumentGraph().createToken(sTextDS1, 108, 113); // jeden
		sDoc1.getDocumentGraph().createToken(sTextDS1, 114, 129); // zurückschrecken
		sDoc1.getDocumentGraph().createToken(sTextDS1, 130, 136); // müssen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 136, 137); // ,
		sDoc1.getDocumentGraph().createToken(sTextDS1, 138, 141); // der
		sDoc1.getDocumentGraph().createToken(sTextDS1, 142, 145); // sie
		sDoc1.getDocumentGraph().createToken(sTextDS1, 146, 151); // statt
		sDoc1.getDocumentGraph().createToken(sTextDS1, 152, 155); // der
		sDoc1.getDocumentGraph().createToken(sTextDS1, 156, 168); // lateinischen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 169, 178); // einführen
		sDoc1.getDocumentGraph().createToken(sTextDS1, 179, 185); // möchte
		sDoc1.getDocumentGraph().createToken(sTextDS1, 185, 186); // .

		STextualDS sTextDS2 = sDoc1.getDocumentGraph().createTextualDS(dipl);
		// tokenize : Die Deutschen Unklarheit möchte
		sDoc1.getDocumentGraph().createToken(sTextDS2, 0, 3); // Die
		sDoc1.getDocumentGraph().createToken(sTextDS2, 4, 13); // deutschen
		sDoc1.getDocumentGraph().createToken(sTextDS2, 87, 97); // Unklarheit
		sDoc1.getDocumentGraph().createToken(sTextDS2, 179, 185); // möchte

		// create document2
		SDocument sDoc2 = SaltFactory.createSDocument();
		sDoc2.setId("doc2");
		sDoc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		String tokenizer = "DiedeutschenNamen,diedannoftdanebenstehen,tragenmeisteinsolchesGeprägederUnklarheit,dasssiejedenzurückschreckenmüssen,dersiestattderlateinischeneinführenmöchte.";
		STextualDS sTextDS3 = sDoc2.getDocumentGraph().createTextualDS(tokenizer);
		sDoc2.getDocumentGraph().createToken(sTextDS3, 0, 3); // Die
		sDoc2.getDocumentGraph().createToken(sTextDS3, 3, 12); // deutschen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 12, 17); // Namen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 17, 18); // ,
		sDoc2.getDocumentGraph().createToken(sTextDS3, 18, 21); // die
		sDoc2.getDocumentGraph().createToken(sTextDS3, 21, 25); // dann
		sDoc2.getDocumentGraph().createToken(sTextDS3, 25, 28); // oft
		sDoc2.getDocumentGraph().createToken(sTextDS3, 28, 35); // daneben
		sDoc2.getDocumentGraph().createToken(sTextDS3, 35, 41); // stehen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 41, 42); // ,
		sDoc2.getDocumentGraph().createToken(sTextDS3, 42, 48); // tragen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 48, 53); // meist
		sDoc2.getDocumentGraph().createToken(sTextDS3, 53, 56); // ein
		sDoc2.getDocumentGraph().createToken(sTextDS3, 56, 63); // solches
		sDoc2.getDocumentGraph().createToken(sTextDS3, 63, 70); // Gepräge
		sDoc2.getDocumentGraph().createToken(sTextDS3, 70, 73); // der
		sDoc2.getDocumentGraph().createToken(sTextDS3, 73, 83); // Unklarheit
		sDoc2.getDocumentGraph().createToken(sTextDS3, 83, 84); // s
		sDoc2.getDocumentGraph().createToken(sTextDS3, 84, 88); // dass
		sDoc2.getDocumentGraph().createToken(sTextDS3, 88, 91); // sie
		sDoc2.getDocumentGraph().createToken(sTextDS3, 91, 96); // jeden
		sDoc2.getDocumentGraph().createToken(sTextDS3, 96, 111); // zurückschrecken
		sDoc2.getDocumentGraph().createToken(sTextDS3, 111, 117); // müssen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 117, 118); // ,
		sDoc2.getDocumentGraph().createToken(sTextDS3, 118, 121); // der
		sDoc2.getDocumentGraph().createToken(sTextDS3, 121, 124); // sie
		sDoc2.getDocumentGraph().createToken(sTextDS3, 124, 129); // statt
		sDoc2.getDocumentGraph().createToken(sTextDS3, 129, 132); // der
		sDoc2.getDocumentGraph().createToken(sTextDS3, 132, 144); // lateinischen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 144, 153); // einführen
		sDoc2.getDocumentGraph().createToken(sTextDS3, 153, 159); // möchte
		sDoc2.getDocumentGraph().createToken(sTextDS3, 159, 160); // .

		// TODO call align method (or whatever)
		// MergerMapper mm = new MergerMapper();
		MappingSubject sub1 = new MappingSubject();
		MappingSubject sub2 = new MappingSubject();

		sub1.setIdentifier(sDoc1.getIdentifier());
		sub2.setIdentifier(sDoc2.getIdentifier());

		getFixture().getMappingSubjects().add(sub1);
		getFixture().getMappingSubjects().add(sub2);

		this.mergeDocumentStructures(chooseBaseDocument());

		// assert base document = sDoc1 & baseText = norm
		assertEquals("The base Document should be document 1. but it is not!", sDoc1, this.container.getBaseDocument());
	}

	/**
	 * Checks if three documents containing 2 texts each, are mergebale (each
	 * text of a document has a matching one in the other documents).
	 * <ol>
	 * <li>document 1:
	 * <ol>
	 * <li>This is the first text.</li>
	 * <li>This is the second text.</li>
	 * </ol>
	 * </li>
	 * <li>document 2:
	 * <ol>
	 * <li>Thisisthefirsttext.</li>
	 * <li>Thisisthesecondtext.</li>
	 * </ol>
	 * </li>
	 * <li>document 3:
	 * <ol>
	 * <li>This is the first text.</li>
	 * <li>This is the second text.</li>
	 * </ol>
	 * </li>
	 * </ol>
	 */
	@Test
	public void testAlignTexts_n2m() {
		// create document 1
		SDocument doc1 = SaltFactory.createSDocument();
		doc1.setId("doc1");
		doc1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text11 = doc1.getDocumentGraph().createTextualDS("This is the first text.");
		doc1.getDocumentGraph().createToken(text11, 0, 4);
		doc1.getDocumentGraph().createToken(text11, 5, 7);
		STextualDS text12 = doc1.getDocumentGraph().createTextualDS("This is the second text.");
		doc1.getDocumentGraph().createToken(text12, 0, 4);
		doc1.getDocumentGraph().createToken(text12, 5, 7);

		// create document 2
		SDocument doc2 = SaltFactory.createSDocument();
		doc2.setId("doc2");
		doc2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text21 = doc2.getDocumentGraph().createTextualDS("Thisisthefirsttext.");
		doc2.getDocumentGraph().createToken(text21, 6, 9);
		doc2.getDocumentGraph().createToken(text21, 9, 14);
		STextualDS text22 = doc2.getDocumentGraph().createTextualDS("Thisisthesecondtext.");
		doc2.getDocumentGraph().createToken(text22, 6, 9);
		doc2.getDocumentGraph().createToken(text22, 9, 15);

		// create document 3
		SDocument doc3 = SaltFactory.createSDocument();
		doc3.setId("doc3");
		doc3.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text31 = doc3.getDocumentGraph().createTextualDS("This   is   the   first    text.");
		doc3.getDocumentGraph().createToken(text31, 27, 31);
		doc3.getDocumentGraph().createToken(text31, 31, 32);
		STextualDS text32 = doc3.getDocumentGraph().createTextualDS("This   is   the   second   text.");
		doc3.getDocumentGraph().createToken(text32, 27, 31);
		doc3.getDocumentGraph().createToken(text32, 31, 32);

		// create mapping subjects for documents
		MappingSubject sub1 = new MappingSubject();
		sub1.setIdentifier(doc1.getIdentifier());
		getFixture().getMappingSubjects().add(sub1);

		MappingSubject sub2 = new MappingSubject();
		sub2.setIdentifier(doc2.getIdentifier());
		getFixture().getMappingSubjects().add(sub2);

		MappingSubject sub3 = new MappingSubject();
		sub3.setIdentifier(doc3.getIdentifier());
		getFixture().getMappingSubjects().add(sub3);

		this.mergeDocumentStructures(chooseBaseDocument());

		assertEquals(2, doc1.getDocumentGraph().getTextualDSs().size());
		assertEquals(12, doc1.getDocumentGraph().getTokens().size());
		assertEquals(12, doc1.getDocumentGraph().getTextualRelations().size());
	}

	/**
	 * Checks, that algorithm chooses the expected base document automatically.
	 * Should be the one having the most nodes and relations (in sum).
	 */
	@Test
	public void testChooseBaseDocument() {
		SaltProject project = SaltFactory.createSaltProject();

		SCorpusGraph g1 = SaltFactory.createSCorpusGraph();
		project.addCorpusGraph(g1);
		SDocument d1_1 = g1.createDocument(URI.createURI("/c1/d1"));
		d1_1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_1.getDocumentGraph().createTextualDS("a sample text");
		d1_1.getDocumentGraph().tokenize();
		MappingSubject subj_1 = new MappingSubject();
		subj_1.setIdentifier(d1_1.getIdentifier());
		getFixture().getMappingSubjects().add(subj_1);

		SCorpusGraph g2 = SaltFactory.createSCorpusGraph();
		project.addCorpusGraph(g2);
		SDocument d1_2 = g2.createDocument(URI.createURI("/c1/d1"));
		d1_2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_2.getDocumentGraph().createTextualDS("This is a sample text.");
		d1_2.getDocumentGraph().tokenize();
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setIdentifier(d1_2.getIdentifier());
		getFixture().getMappingSubjects().add(subj_2);

		SCorpusGraph g3 = SaltFactory.createSCorpusGraph();
		project.addCorpusGraph(g3);
		SDocument d1_3 = g3.createDocument(URI.createURI("/c1/d1"));
		d1_3.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_3.getDocumentGraph().createTextualDS("a sample");
		d1_3.getDocumentGraph().tokenize();
		MappingSubject subj_3 = new MappingSubject();
		subj_3.setIdentifier(d1_3.getIdentifier());
		getFixture().getMappingSubjects().add(subj_3);

		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()) {
			if (subj.getIdentifier().getIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getIdentifier().getIdentifiableElement();
				this.normalizePrimaryTexts(sDoc);
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
		SCorpusGraph g1 = SaltFactory.createSCorpusGraph();
		SDocument d1_1 = g1.createDocument(URI.createURI("/c1/d1"));
		d1_1.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_1.getDocumentGraph().createTextualDS("a sample text");
		MappingSubject subj_1 = new MappingSubject();
		subj_1.setIdentifier(d1_1.getIdentifier());
		getFixture().getMappingSubjects().add(subj_1);

		SCorpusGraph g2 = SaltFactory.createSCorpusGraph();
		SDocument d1_2 = g2.createDocument(URI.createURI("/c1/d1"));
		d1_2.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_2.getDocumentGraph().createTextualDS("This is a sample text.");
		MappingSubject subj_2 = new MappingSubject();
		subj_2.setIdentifier(d1_2.getIdentifier());
		getFixture().getMappingSubjects().add(subj_2);

		SCorpusGraph g3 = SaltFactory.createSCorpusGraph();
		SDocument d1_3 = g3.createDocument(URI.createURI("/c1/d1"));
		d1_3.setDocumentGraph(SaltFactory.createSDocumentGraph());
		d1_3.getDocumentGraph().createTextualDS("a sample");
		MappingSubject subj_3 = new MappingSubject();
		subj_3.setIdentifier(d1_3.getIdentifier());
		getFixture().getMappingSubjects().add(subj_3);

		PepperModuleProperty prop = getFixture().getProperties().getProperty(MergerProperties.PROP_FIRST_AS_BASE);
		prop.setValue(Boolean.TRUE);

		this.initialize();
		// normalize all texts
		for (MappingSubject subj : getFixture().getMappingSubjects()) {
			if (subj.getIdentifier().getIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getIdentifier().getIdentifiableElement();
				this.normalizePrimaryTexts(sDoc);
			}
		}

		getFixture().setBaseCorpusStructure(g3);

		MappingSubject result = this.chooseBaseDocument();

		assertEquals(subj_3.getIdentifier(), result.getIdentifier());
		assertEquals(d1_3, this.container.getBaseDocument());
	}
}
