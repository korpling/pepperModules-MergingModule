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

package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleDataException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.GRAPH_TRAVERSE_TYPE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * This class does the real merging, the main task is to merge a set of document
 * graphs.
 * 
 * @author Mario Frank
 * @author Jakob Schmoling
 * @author Florian Zipser
 */
public class MergerMapper extends PepperMapperImpl implements PepperMapper {

	private static final Logger logger = LoggerFactory.getLogger(MergerMapper.class);

	protected boolean isTestMode = false;

	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		if (this.getMappingSubjects().size() != 0) {
			if (logger.isDebugEnabled()) {
				StringBuilder str = new StringBuilder();
				str.append("Start merging corpora: ");
				for (MappingSubject subj : getMappingSubjects()) {
					str.append("'");
					str.append(SaltFactory.eINSTANCE.getGlobalId(subj.getSElementId()));
					str.append("' ");
				}
				logger.debug("[Merger] " + str.toString());
			}

			SCorpus baseCorpus = null;
			// emit corpus in base corpus-structure
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getSElementId().getSIdentifiableElement() instanceof SCorpus) {
					SCorpus sCorp = (SCorpus) subj.getSElementId().getSIdentifiableElement();
					if (sCorp.getSCorpusGraph().equals(getBaseCorpusStructure())) {
						baseCorpus = sCorp;
						break;
					}
				}
			}
			// copy all annotations of corpus
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getSElementId().getSIdentifiableElement() instanceof SCorpus) {
					SCorpus sCorp = (SCorpus) subj.getSElementId().getSIdentifiableElement();
					if (sCorp == baseCorpus) {// corpus is base corpus
						subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					} else {// corpus is not base corpus
						SaltFactory.eINSTANCE.moveSAnnotations(sCorp, baseCorpus);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(sCorp, baseCorpus);
						subj.setMappingResult(DOCUMENT_STATUS.DELETED);
					}

				}
			}
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * This method is called by the Pepper framework and merges a set of given
	 * {@link SDocumentGraph} objects.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		this.initialize();
		if (this.getMappingSubjects().size() != 0) {

			if (logger.isDebugEnabled()) {
				StringBuilder str = new StringBuilder();
				str.append("Start merging documents: ");
				for (MappingSubject subj : getMappingSubjects()) {
					str.append("'");
					str.append(SaltFactory.eINSTANCE.getGlobalId(subj.getSElementId()));
					str.append("' ");
				}
				logger.debug("[Merger] " + str.toString());
			}

			boolean isEmpty = true;

			// base subject containing the base document
			MappingSubject baseSubject = chooseBaseDocument();
			if (baseSubject == null) {
				throw new PepperModuleException(this, "This might be a bug, no base document could have been computed.");
			}
			// base document
			SDocument baseDocument = (SDocument) baseSubject.getSElementId().getSIdentifiableElement();

			// copy all annotations of document
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
					SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
					if (sDoc != baseDocument) {// document is not base corpus
						SaltFactory.eINSTANCE.moveSAnnotations(sDoc, baseDocument);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(sDoc, baseDocument);
					}
				}
			}

			// emit if document contains content
			for (MappingSubject subj : getMappingSubjects()) {
				if ((subj.getSElementId() != null) && (subj.getSElementId().getIdentifiableElement() != null)) {
					SDocument doc = (SDocument) subj.getSElementId().getIdentifiableElement();
					if ((doc.getSDocumentGraph() != null) && (doc.getSDocumentGraph().getSNodes().size() != 0)) {
						isEmpty = false;
						break;
					}
				}
			}
			if (!isEmpty) {
				mergeSDocumentGraph();
			}

			// check, that base document emitted by algorithm is in base
			// corpus-structure, if not, copy it
			for (MappingSubject subj : getMappingSubjects()) {
				SDocument sDoc = ((SDocument) subj.getSElementId().getSIdentifiableElement());
				if (DOCUMENT_STATUS.COMPLETED.equals(subj.getMappingResult())) {
					if (!sDoc.equals(baseDocument)) {
						SDocumentGraph oldGraph = sDoc.getSDocumentGraph();
						sDoc.setSDocumentGraph(baseDocument.getSDocumentGraph());
						baseDocument.setSDocumentGraph(oldGraph);
						subj.setMappingResult(DOCUMENT_STATUS.DELETED);
					} else if (sDoc.equals(baseDocument)) {
						subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					}
				}
			}

			logger.debug("[Merger] " + "merged documents {}. ", getMappingSubjects());
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * Determines which {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 **/
	private SCorpusGraph baseCorpusStructure = null;

	/**
	 * Returns the {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 * 
	 * @return
	 */
	public SCorpusGraph getBaseCorpusStructure() {
		return baseCorpusStructure;
	}

	/**
	 * Sets the {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 * 
	 * @param baseCorpusStructure
	 */
	public void setBaseCorpusStructure(SCorpusGraph baseCorpusStructure) {
		this.baseCorpusStructure = baseCorpusStructure;
	}

	/**
	 * This method initializes the mapping.
	 */
	protected void initialize() {
		if (getContainer() == null) {
			container = new TokenMergeContainer();
		}
	}

	/**
	 * A map to relate nodes of one graph to nodes to another graph.
	 */
	private Map<SNode, SNode> node2NodeMap = null;

	public void mergeSDocumentGraph() {
		if (this.getMappingSubjects().size() != 0) {
			MappingSubject baseDocument = null;

			if (this.getMappingSubjects().size() < 2) {
				baseDocument = getMappingSubjects().get(0);
			} else {
				this.initialize();
				// normalize all texts
				for (MappingSubject subj : this.getMappingSubjects()) {
					if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
						SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
						this.normalizeTextualLayer(sDoc);
					}
				}
				baseDocument = chooseBaseDocument();
				if (baseDocument == null) {
					throw new PepperModuleException(this, "Could not choose a base SDocument");
				}

				// align all texts and create the nonEquivalentTokenSets
				// / base text -- < Other Document -- nonEquivalentTokens >
				Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> nonEquivalentTokenSets = this.allignAllTexts();

				// / choose the perfect STextualDS of the base Document
				SDocument baseDoc = getContainer().getBaseDocument();
				STextualDS baseText = chooseBaseText(baseDoc, nonEquivalentTokenSets);
				if (baseText == null) {
					throw new PepperModuleException(this, "Could not choose a base STextualDS.");
				}
				// set the base text
				getContainer().setBaseText(baseText);

				// clear the table of non-equivalent tokens
				nonEquivalentTokenSets.clear();

				// merge two document-structures pairwise
				for (MappingSubject subj : this.getMappingSubjects()) {
					// for all documents
					SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
					if (sDoc != getContainer().getBaseDocument()) {
						// ignore the base document and merge the others

						int initialSize = getContainer().getBaseDocument().getSDocumentGraph().getSNodes().size();
						if (sDoc.getSDocumentGraph().getSNodes().size() > initialSize) {
							initialSize = sDoc.getSDocumentGraph().getSNodes().size();
						}
						node2NodeMap = new Hashtable<SNode, SNode>(initialSize);

						if (sDoc.getSDocumentGraph().getSTextualDSs() != null) {
							// there should be texts
							logger.trace("[Merger] " + "Aligning the texts of {} to the base text. ", SaltFactory.eINSTANCE.getGlobalId(sDoc.getSElementId()));

							Set<SToken> nonEquivalentTokensOfOtherText = new HashSet<SToken>();
							nonEquivalentTokensOfOtherText.addAll(sDoc.getSDocumentGraph().getSTokens());

							for (STextualDS sTextualDS : sDoc.getSDocumentGraph().getSTextualDSs()) {
								// align the texts
								this.alignTexts(getContainer().getBaseText(), sTextualDS, nonEquivalentTokensOfOtherText, node2NodeMap);
								this.mergeTokens(getContainer().getBaseText(), sTextualDS, node2NodeMap);
							}
							// merge the document content
							mergeDocumentStructure((SDocument) baseDocument.getSElementId().getSIdentifiableElement(), sDoc);
							// we are finished with the document. Free the
							// memory
							if (!this.isTestMode) {
								getContainer().finishDocument(sDoc);
								subj.setMappingResult(DOCUMENT_STATUS.DELETED);
							}
						} else {
							// there are no texts. So, just copy everything into
							// the base document graph
							logger.warn("There is no text in document {} to be merged. Will not copy the tokens!", SaltFactory.eINSTANCE.getGlobalId(sDoc.getSElementId()));
							// merge the document content
							mergeDocumentStructure((SDocument) baseDocument.getSElementId().getSIdentifiableElement(), sDoc);
							// we are finished with the document. Free the
							// memory

							if (!this.isTestMode) {
								getContainer().finishDocument(sDoc);
								subj.setMappingResult(DOCUMENT_STATUS.DELETED);
							}
						}
					}
				}
			}

			if (baseDocument != null) {
				if (!this.isTestMode) {
					getContainer().finishDocument((SDocument) baseDocument.getSElementId().getSIdentifiableElement());
					baseDocument.setMappingResult(DOCUMENT_STATUS.COMPLETED);
				}
			} else {
				// nothing to be merged
				int i = 0;
				for (MappingSubject subj : getMappingSubjects()) {
					if (i == 0) {
						subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					} else {
						subj.setMappingResult(DOCUMENT_STATUS.DELETED);
					}
					i++;
				}
			}
		} else {
			logger.warn("[Merger] No documents to merge. ");
		}
	}

	/** the {@link TokenMergeContainer} instance **/
	protected TokenMergeContainer container = null;

	public TokenMergeContainer getContainer() {
		return container;
	}

	/* *******************************************************************
	 * Choosing base SDocument and base STextualDS
	 * ******************************************************************
	 */

	/**
	 * Chooses the base {@link SDocument} in which all nodes, relations etc.
	 * have to be merged in in further processing.
	 * <br/>
	 * The base document is the one which is the one contained in the base corpus structure or if
	 * the corpus structure is not set, the one having the most nodes and relations.
	 * 
	 * @return The base {@link MappingSubject} containing the base document
	 */
	protected MappingSubject chooseBaseDocument() {
		MappingSubject baseSubject = null;
		//maximum number of SNodes and SRelations contained in document structure
		int maxNumOfElements= 0;
		for (MappingSubject subj : getMappingSubjects()) {
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
				// check if document and document structure is given
				if (sDoc == null) {
					throw new PepperModuleException(this, "A Mappingsubject does not contain a document object. This seems to be a bug. ");
				} else if (sDoc.getSDocumentGraph() == null) {
					logger.warn("The document '" + SaltFactory.eINSTANCE.getGlobalId(sDoc.getSElementId()) + "' does not contain a document structure. Therefore it was ignored. ");
					continue;
				}
				
				if (getBaseCorpusStructure() == null) {
					//current number of SNodes and SRelations contained in document structure
					int currNumOfElements= (sDoc.getSDocumentGraph().getSNodes().size() + sDoc.getSDocumentGraph().getSRelations().size());
					if (maxNumOfElements < currNumOfElements){
						//numOfElements is less than current sum of nodes and relations, current document structure is the bigger one
						
						maxNumOfElements= currNumOfElements; 
						baseSubject = subj;
					}
				} else {
					// take document which is contained in base corpus structure
	
					if (sDoc.getSCorpusGraph().equals(getBaseCorpusStructure())) {
						baseSubject = subj;
						break;
					}
				}
			}
		}
		getContainer().setBaseDocument((SDocument) baseSubject.getSElementId().getSIdentifiableElement());
		return baseSubject;
	}

	/**
	 * This method chooses a base {@link STextualDS} for the given base
	 * {@link SDocument} heuristically by determining the {@link STextualDS}
	 * which has the least count of tokens which do not have an equivalent in
	 * all other documents.
	 * 
	 * @param baseDoc
	 *            The base {@link SDocument}
	 * @param nonEquivalentTokenSets
	 *            The set of tokens which do not have an equivalent in the base
	 *            {@link STextualDS}
	 * @return The {@link STextualDS} which is suited best to be the base
	 *         {@link STextualDS}
	 */
	protected STextualDS chooseBaseText(SDocument baseDoc, Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> nonEquivalentTokenSets) {
		STextualDS baseText = null;
		int minimalNonEquivalentTokens = -1;
		for (STextualDS text : baseDoc.getSDocumentGraph().getSTextualDSs()) {
			// for all texts of the base document
			Hashtable<SDocument, HashSet<SToken>> nonEQTokensInOtherDoc = nonEquivalentTokenSets.get(text);
			if (nonEQTokensInOtherDoc != null) {
				// there is a set of non-equivalent token for the current
				// base text
				int countOfNonEquivalentTokens = 0;
				for (SDocument otherDoc : nonEQTokensInOtherDoc.keySet()) {
					// count the number of tokens of all documents which do
					// not have an equivalent in the current base text
					countOfNonEquivalentTokens += nonEQTokensInOtherDoc.get(otherDoc).size();
				} // count the number of tokens of all documents which do
					// have an equivalent in the current base text
				if (minimalNonEquivalentTokens == -1) {
					// if the minimalNonEquivalentTokens value is -1, we did
					// not process a document, yet. initialize
					minimalNonEquivalentTokens = countOfNonEquivalentTokens;
					baseText = text;
				} // if the minimalNonEquivalentTokens value is -1, we did
					// process a document, yet. initialize
				else { // there is some base text
					if (minimalNonEquivalentTokens > countOfNonEquivalentTokens) {
						// if there are less non-equivalent tokens for this
						// text than for
						// some other, set this text as base text
						minimalNonEquivalentTokens = countOfNonEquivalentTokens;
						baseText = text;
					} // if there are less non-equivalent tokens for this
						// text than for some other, set this text as base
				}
			} // there is a set of non-equivalent token for the current base
				// text
		}
		if (baseText != null) {
			logger.trace("[Merger] " + "Chose base text. It is text with id '{}'.", SaltFactory.eINSTANCE.getGlobalId(baseText.getSElementId()));
		}
		return baseText;
	}

	/* *******************************************************************
	 * Normalizing STextualDS
	 * ******************************************************************
	 */

	/**
	 * This method normalizes the text specified by the given {@link STextualDS}
	 * .
	 * 
	 * @param sTextualDS
	 *            the {@link STextualDS} to normalize
	 * @return The normalized text
	 */
	protected static String normalizeText(STextualDS sTextualDS, Map<String, String> escapeTable) {
		String normalizedText = null;
		StringBuilder normalizedTextBuilder = new StringBuilder();
		// normalize the text
		for (char c : sTextualDS.getSText().toCharArray()) {
			String originalString = new String();
			originalString += c;
			String stringToEscape = escapeTable.get(originalString);
			// fill the StringBuilder
			if (stringToEscape != null) {
				normalizedTextBuilder.append(stringToEscape);
			} else {
				normalizedTextBuilder.append(c);
			}
		}
		// now we have the normalized text
		normalizedText = normalizedTextBuilder.toString();
		return normalizedText;
	}

	protected String createOriginalToNormalizedMapping(STextualDS sTextualDS, List<Integer> originalToNormalizedMapping) {
		StringBuilder normalizedTextBuilder = new StringBuilder();
		int start = 0;
		for (char c : sTextualDS.getSText().toCharArray()) {
			String originalString = new String();
			originalString += c;
			String stringToEscape = ((MergerProperties) getProperties()).getEscapeMapping().get(originalString);
			if (stringToEscape == null) {
				originalToNormalizedMapping.add(start);
				normalizedTextBuilder.append(c);
				start += 1;
			} else {
				if (stringToEscape.length() > 0) {
					originalToNormalizedMapping.add(start);
					for (int i = 0; i < stringToEscape.length(); i++) {
						// one char is mapped to many. all chars have the same
						// index in the original text
						start += 1;
					}
					normalizedTextBuilder.append(stringToEscape);
				} else { // one char is mapped to the empty string.
							// TODO: TALK ABOUT THIS!
					originalToNormalizedMapping.add(start);
				}
			}
		}

		String normalizedText = normalizedTextBuilder.toString();
		return normalizedText;
	}

	/**
	 * This method normalizes the textual layer for the given {@link SDocument}.
	 * Note: only the normalization is done. The equivalent {@link SToken} are
	 * not determined in any way. For this functionality, you need to use
	 * {@link alignDocuments}.
	 * 
	 * @param sDocument
	 *            the {@link SDocument} for which the textual layer should be
	 *            normalized.
	 */
	protected void normalizeTextualLayer(SDocument sDocument) {
		if (sDocument == null)
			throw new PepperModuleException(this, "Cannot normalize Text of the document since the SDocument reference is NULL");
		if (sDocument.getSDocumentGraph() != null) {
			// check whether the document has any STextualDS
			List<STextualDS> sTextualDSs = sDocument.getSDocumentGraph().getSTextualDSs();

			// create maps which give fast access to a token which is specified
			// by it's original left/right value
			Hashtable<Integer, SToken> tokensMappedByLeft = new Hashtable<Integer, SToken>();
			Hashtable<Integer, SToken> tokensMappedByRight = new Hashtable<Integer, SToken>();
			for (STextualRelation textRel : sDocument.getSDocumentGraph().getSTextualRelations()) {
				tokensMappedByLeft.put(textRel.getSStart(), textRel.getSToken());
				tokensMappedByRight.put(textRel.getSEnd(), textRel.getSToken());
			}

			// normalize all textual datasources
			for (STextualDS sTextualDS : sTextualDSs) {
				List<Integer> originalToNormalizedMapping = new Vector<Integer>();
				String normalizedText = createOriginalToNormalizedMapping(sTextualDS, originalToNormalizedMapping);
				for (STextualRelation textRel : sDocument.getSDocumentGraph().getSTextualRelations()) {
					if (textRel.getSTextualDS().equals(sTextualDS)) {
						SToken sToken = textRel.getSToken();
						int normalizedTokenStart = originalToNormalizedMapping.get(textRel.getSStart());
						int normalizedTokenEnd = 0;
						if (textRel.getSEnd() >= (originalToNormalizedMapping.size())) {
							if (textRel.getSEnd() >= (originalToNormalizedMapping.size() + 1)) {
								throw new PepperModuleException(this, "This might be a bug of MergerMapper: textRel.getSEnd() >= (originalToNormalizedMapping.size()+1).");
							} else {
								normalizedTokenEnd = originalToNormalizedMapping.get(originalToNormalizedMapping.size() - 1) + 1;
							}
						} else {
							normalizedTokenEnd = originalToNormalizedMapping.get(textRel.getSEnd());
						}
						getContainer().addAlignedToken(sTextualDS, sToken, normalizedTokenStart, normalizedTokenEnd);
					}
				}
				getContainer().addNormalizedText(sDocument, sTextualDS, normalizedText);
			}
		}
	}

	/* *******************************************************************
	 * Aligning STextualDS
	 * ******************************************************************
	 */

	/**
	 * This method tries to align all texts of all {@link SDocument} objects to
	 * the base {@link STextualDS}.
	 * 
	 * @return The data structure which contains all {@link SToken} objects
	 *         contained in the {@link SDocument} which do not have an
	 *         equivalent in the {@link STextualDS} specified as key.
	 */
	private Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> allignAllTexts() {
		Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> nonEquivalentTokenSets = new Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>>();
		for (MappingSubject subj : this.getMappingSubjects()) {
			boolean hasTexts = true;
			SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
			if (sDoc != getContainer().getBaseDocument()) {// ignore the base
															// document and
															// align
															// all other
				if (sDoc.getSDocumentGraph() == null) {
					throw new PepperModuleDataException(this, "Cannot map document '" + SaltFactory.eINSTANCE.getGlobalId(sDoc.getSElementId()) + "', since it does not contain a document-structure.");
				}
				if (sDoc.getSDocumentGraph().getSTextualDSs() != null) {
					// there are possibly texts
					subj.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
					if (sDoc.getSDocumentGraph().getSTextualDSs().size() > 0) {
						// The other document has at least one text
						HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
						for (STextualDS baseText : getContainer().getBaseDocument().getSDocumentGraph().getSTextualDSs()) { // for
							// all
							// texts
							// of
							// the
							// base
							// document
							nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							// initialize the set of nonEquivalent token.
							// Initially, all token do not have an equivalent.
							// in alignTexts, tokens which DO have an equivalent
							// are removed from the set
							if (sDoc.getSDocumentGraph().getSTokens() != null) {
								nonEquivalentTokenInOtherTexts.addAll(sDoc.getSDocumentGraph().getSTokens());
							}
							for (STextualDS otherText : sDoc.getSDocumentGraph().getSTextualDSs()) {
								// align the current base text with all texts of
								// the other document
								Map<SNode, SNode> equivalenceMap = new Hashtable<SNode, SNode>();
								this.alignTexts(baseText, otherText, nonEquivalentTokenInOtherTexts, equivalenceMap);
							}
							// / save all unique token of the other document
							if (nonEquivalentTokenSets.containsKey(baseText)) {
								nonEquivalentTokenSets.get(baseText).put(sDoc, nonEquivalentTokenInOtherTexts);
							} else {
								Hashtable<SDocument, HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
								newTab.put(sDoc, nonEquivalentTokenInOtherTexts);
								nonEquivalentTokenSets.put(baseText, newTab);
							}
						} // for all texts of the base document
					} // The other document has at least one text
					else { // The other document has NO text
						hasTexts = false;
					} // The other document has NO text
				} // there are possibly texts
				else { // The other document has NO text
					hasTexts = false;
				} // The other document has NO text

				if (!hasTexts) {
					HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
					for (STextualDS baseText : getContainer().getBaseDocument().getSDocumentGraph().getSTextualDSs()) {
						nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
						if (sDoc.getSDocumentGraph().getSTokens() != null) {
							// mark all tokens of the other document as unique
							nonEquivalentTokenInOtherTexts.addAll(sDoc.getSDocumentGraph().getSTokens());
						}
						if (nonEquivalentTokenSets.containsKey(baseText)) {
							nonEquivalentTokenSets.get(baseText).put(sDoc, nonEquivalentTokenInOtherTexts);
						} else {
							Hashtable<SDocument, HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
							newTab.put(sDoc, nonEquivalentTokenInOtherTexts);
							nonEquivalentTokenSets.put(baseText, newTab);
						}
					}
				}
			}
		}
		return nonEquivalentTokenSets;
	}

	/**
	 * This method aligns the normalized texts of the given {@link STextualDS}
	 * objects and <b>also</b> aligns the {@link SToken} including the creation
	 * of equivalent {@link SToken} information. If a {@link SToken} has an
	 * equivalent {@link SToken} in the base text, it is removed from the
	 * nonEquivalentTokenInOtherTexts set.
	 * 
	 * @param baseText
	 *            the base {@link STextualDS}
	 * @param otherText
	 *            the other {@link STextualDS}
	 * @param nonEquivalentTokenInOtherTexts
	 *            A HashSet which contains all tokens which do not have an
	 *            equivalent in the base text
	 * @param equivalenceMap
	 *            A map of tokens in the other text with their equivalent token
	 *            in the base text as value
	 * @return true on success and false on failure
	 */
	protected boolean alignTexts(STextualDS baseText, STextualDS otherText, Set<SToken> nonEquivalentTokenInOtherTexts, Map<SNode, SNode> equivalenceMap) {
		if (baseText == null)
			throw new PepperModuleException(this, "Cannot align the Text of the documents since the base SDocument reference is NULL");
		if (otherText == null)
			throw new PepperModuleException(this, "Cannot align the Text of the documents since the other SDocument reference is NULL");

		// TODO REVISE THIS CODE
		boolean returnVal = false;
		// first we need the two normalized texts
		String normalizedBaseText = getContainer().getNormalizedText(baseText);
		String normalizedOtherText = getContainer().getNormalizedText(otherText);

		// set the mapping of the normalized base text to the original base text
		if (getContainer().getBaseTextPositionByNormalizedTextPosition(getContainer().getBaseText(), 0) == -1) {
			getContainer().setBaseTextPositionByNormalizedTextPosition(baseText, this.createBaseTextNormOriginalMapping(getContainer().getBaseText()));
		}

		int offset = -1;

		// set the bigger and smaller text
		STextualDS biggerText = baseText;
		STextualDS smallerText = otherText;

		if (normalizedBaseText.length() >= normalizedOtherText.length()) {
			// if the other text fits into the base text by size
			offset = indexOfOmitChars(normalizedBaseText.toLowerCase(), normalizedOtherText.toLowerCase(), true, ((MergerProperties) getProperties()).getPunctuations());
		} // if the other text fits into the base text by size
		else { // if the base text fits into the other text by size
			offset = indexOfOmitChars(normalizedOtherText.toLowerCase(), normalizedBaseText.toLowerCase(), true, ((MergerProperties) getProperties()).getPunctuations());
			biggerText = otherText;
			smallerText = baseText;
		} // if the base text fits into the other text by size

		if (offset != -1) {// if the normalized bigger text is contained in the
							// normalized smaller text

			returnVal = true;
			// get the tokens of the other text.
			List<SToken> textTokens = new Vector<SToken>();
			for (Edge e : smallerText.getSDocumentGraph().getInEdges(smallerText.getSId())) {
				// get all tokens of the smaller text
				if (e instanceof STextualRelation) {
					textTokens.add(((STextualRelation) e).getSToken());
				}
			} // get all tokens of the smaller text

			for (SToken smallerTextToken : textTokens) {
				// get the aligned token start and length
				int smallerTextTokenStart = getContainer().getAlignedTokenStart(smallerText, smallerTextToken);
				int smallerTextTokenLength = getContainer().getAlignedTokenLength(smallerText, smallerTextToken);

				if (smallerTextTokenStart != -1 && smallerTextTokenLength != -1) {
					// the token of the smaller text has a start and end in the
					// smaller text: get the aligned token from the base
					// document which has the start of offset+startOfOtherToken
					SToken biggerTextToken = getContainer().getAlignedTokenByStart(biggerText, (smallerTextTokenStart + offset));
					if (biggerTextToken != null) {
						// there is some token in the bigger text which has the
						// same start
						if (getContainer().getAlignedTokenLength(biggerText, biggerTextToken) == smallerTextTokenLength) {
							// start and lengths are identical. We found an
							// equivalence class
							// we want to have equivalences: otherTextToken -->
							// baseTextToken
							if (biggerText.equals(baseText)) {
								// if the base text is the bigger text
								getContainer().addTokenMapping(biggerTextToken, smallerTextToken, smallerText);
								equivalenceMap.put(smallerTextToken, biggerTextToken);

								nonEquivalentTokenInOtherTexts.remove(smallerTextToken);
							} // if the base text is the bigger text
							else { // if the base text is the smaller text
									// smallerText = baseText
									// smallerTextToken = baseTextToken
								getContainer().addTokenMapping(smallerTextToken, biggerTextToken, biggerText);
								equivalenceMap.put(biggerTextToken, smallerTextToken);
								nonEquivalentTokenInOtherTexts.remove(biggerTextToken);
							} // if the base text is the smaller text

						} // start and lengths are identical. We found an
							// equivalence class
						else { // start is identical but the length is not. No
								// equivalence
						} // start is identical but the length is not. No
							// equivalence
					} else { // start is not identical. No equivalence
					} // start is not identical. No equivalence

				} else { // the other token has either no start or no length ->
							// ERROR
					throw new PepperModuleException(this, "The SToken " + smallerText.getSId() + " of the STextualDS " + smallerText.getSId() + " has no proper start or length. It was probably not aligned correctly.");
				} // the other token has either no start or no length -> ERROR
			}

		}
		// get base text
		return returnVal;
	}

	/* *******************************************************************
	 * Alignment and Normalization Helper Methods
	 * ******************************************************************
	 */

	/**
	 * This method searches for the first occurence of the stringToSearchFor in
	 * the stringToSearchIn and ommits all chars in the omitCharArray. The index
	 * of the first occurence is returned.
	 * 
	 * @param stringToSearchIn
	 * @param stringToSearchFor
	 * @param omitCharArray
	 * @param useIndexof
	 *            If this flag is set, all omit chars are removed from both
	 *            provided strings and a normal indexOf is used
	 * @return the index on success and -1 on failure
	 */
	protected int indexOfOmitChars(String stringToSearchIn, String stringToSearchFor, boolean useIndexOf, Set<Character> omitChars) {
		/* remove all omit chars from the stringToSearchFor */
		StringBuilder builder = new StringBuilder();
		for (char sourceChar : stringToSearchFor.toCharArray()) {
			if (!omitChars.contains(sourceChar)) {
				builder.append(sourceChar);
			}
		}
		String sourceString = builder.toString();

		if (useIndexOf) {
			builder = new StringBuilder();
			List<Integer> normalizedToOriginalMapping = new Vector<Integer>();
			int start = 0;
			for (char targetChar : stringToSearchIn.toCharArray()) {
				if (!omitChars.contains(targetChar)) { // no omit char
					normalizedToOriginalMapping.add(start);
					builder.append(targetChar);
				} else { // omit char
				}
				start += 1;
			}
			String targetString = builder.toString();
			int index = targetString.indexOf(sourceString);
			if (index != -1) {
				return normalizedToOriginalMapping.get(index);
			} else {
				return index;
			}

		}

		/* Initialize needed structures */
		char c = sourceString.toCharArray()[0];
		int position = 0;
		int retVal = -1;
		boolean found = false;

		// transform the base text into a char array
		char[] charsToSearchIn = stringToSearchIn.toCharArray();
		while (true) {
			// the start position of the string to search
			position = stringToSearchIn.indexOf(c, position);
			retVal = position; // initialize the return value to the found
								// position
			if (position == -1) { // stop, if the first char could not be found
				break;
			}

			// we guess that we found a complete match
			found = true;
			// initialize the count of matched chars
			int successfulMatchCount = 0;
			for (char sourceChar : sourceString.toCharArray()) { // for all
																	// chars of
																	// the
																	// string to
																	// search
																	// / search
																	// the char
																	// in the
																	// target
																	// string
																	// and omit
																	// all omit
																	// chars in
																	// the
																	// target
																	// string
				boolean foundChar = false;
				for (int i = position; i < stringToSearchIn.length(); i++) { // search
																				// the
																				// current
																				// char
																				// and
																				// ignore
																				// all
																				// chars
																				// to
																				// omit
																				// in
																				// the
																				// target
																				// string
					char targetChar = charsToSearchIn[i];
					if (omitChars.contains(targetChar)) { // ignore
						position++;
						continue;
					} // ignore
					else { // do not ignore
						if (targetChar == sourceChar) { // we found the matching
														// char
							successfulMatchCount++;
							foundChar = true;
							position++;
							break;
						} // we found the matching char
						else { // the char is wrong
							foundChar = false;
							// position++;
							break;
						} // the char is wrong
					} // do not ignore
				} // search the current char and ignore all chars to omit in the
					// target string
				if (!foundChar) { // if a char could not be found, stop the
									// search in the current subString
					retVal = -1;
					break;
				} // if a char could not be found, stop the search in the
					// current subString
			}
			if (found) { // if the found flag is still set, we are finished, if
							// the successfulMatchCount is equal to the source
							// string
				if (successfulMatchCount == sourceString.length()) {
					break;
				}
			}
		}

		return retVal;
	}

	/**
	 * This method creates a reverse mapping list for the given Text. If the
	 * given text is normalized including the removal of whitespaces, the
	 * position of characters is changed. But if the position of a character in
	 * the original text is needed, we need more information. This method
	 * generates the needed information.
	 * 
	 * @param sTextualDS
	 *            The {@link STextualDS}
	 * @return A list of integers. The integer at index i specifies the position
	 *         of the i'th character of the normalized text in the original
	 *         text. Example: Let c be the second character in the original text
	 *         and a whitespace the first character in the original text. Since
	 *         the whitespace is removed, c is the first character in the
	 *         normalized text. The first element of the returned list will
	 *         contain the number 2 since c was the second char, originally.
	 */
	protected List<Integer> createBaseTextNormOriginalMapping(STextualDS sTextualDS) {
		/**
		 * Example1: dipl: " this is" 01234567 norm: "thisis" 012345 0->1
		 * 1->2,... Example2: dipl: " thäs is" 01234567 norm: "thaesis" 0123456
		 * 0->1 1->2 2->3 3->3 4->4 5->6 6->7
		 */
		List<Integer> normalizedToOriginalMapping = new Vector<Integer>();
		int start = 0;
		for (char c : sTextualDS.getSText().toCharArray()) {
			String originalString = new String();
			originalString += c;
			String stringToEscape = ((MergerProperties) getProperties()).getEscapeMapping().get(originalString);
			if (stringToEscape == null) {
				normalizedToOriginalMapping.add(start);
			} else {
				if (stringToEscape.length() > 0) {
					for (int i = 0; i < stringToEscape.toCharArray().length; i++) {
						// one char is mapped to many. all chars have the same
						// index in the original text
						normalizedToOriginalMapping.add(start);
					}
				} else { // one char is mapped to the empty string.
							// do nothing
				}
			}
			start += 1;
		}
		return normalizedToOriginalMapping;
	}

	/* ********************************************************************
	 * Mergeability Checks
	 * *******************************************************************
	 */

	/**
	 * This method checks whether the provided documents are mergeable. I.e., it
	 * is checked whether at least one text of the one document is alignible
	 * with at least one text of the other document.
	 * 
	 * @param doc1
	 *            Some {@link SDocument}
	 * @param doc2
	 *            Some {@link SDocument}
	 * @return true, if one of the {@link SDocument} objects is meargeable into
	 *         the other and false, else
	 */
	public boolean isMergeable(SDocument doc1, SDocument doc2) {
		boolean retVal = false;

		List<STextualDS> doc1Texts = doc1.getSDocumentGraph().getSTextualDSs();
		List<STextualDS> doc2Texts = doc2.getSDocumentGraph().getSTextualDSs();
		if (doc1Texts != null && doc2Texts != null) { // both documents should
														// have texts
			if ((!doc1Texts.isEmpty()) && (!doc2Texts.isEmpty())) { // both
																	// documents
																	// do have
																	// at least
																	// one text
				for (STextualDS text1 : doc1Texts) {
					String normalizedText1 = normalizeText(text1, ((MergerProperties) getProperties()).getEscapeMapping());
					for (STextualDS text2 : doc2Texts) {
						String normalizedText2 = normalizeText(text1, ((MergerProperties) getProperties()).getEscapeMapping());
						if (indexOfOmitChars(normalizedText1, normalizedText2, true, ((MergerProperties) getProperties()).getPunctuations()) != -1 || indexOfOmitChars(normalizedText2, normalizedText1, true, ((MergerProperties) getProperties()).getPunctuations()) != -1

						) {
							retVal = true;
							break;
						}
					}
					if (retVal) {
						break;
					}
				}
			} // both documents do have at least one text
			else { // one document does not have a text. We can merge
				retVal = true;
			} // one document does not have a text. We can merge

		} // both documents should have texts
		else { // at least one document obviously does not have a text. We can
				// merge.
			retVal = true;
		} // at least one document obviously does not have a text. We can merge.

		return retVal;
	}

	/* *****************************************************************************
	 * Merging Methods
	 * **********************************************************
	 * ******************
	 */

	protected void mergeTokens(STextualDS baseText, STextualDS otherText, Map<SNode, SNode> equivalenceMap) {
		// We want to merge the tokens of the other text into the base text.
		// first we need the two normalized texts
		String normalizedBaseText = getContainer().getNormalizedText(baseText);
		String normalizedOtherText = getContainer().getNormalizedText(otherText);

		// set the mapping of the normalized base text to the original base text
		if (getContainer().getBaseTextPositionByNormalizedTextPosition(getContainer().getBaseText(), 0) == -1) {
			getContainer().setBaseTextPositionByNormalizedTextPosition(baseText, this.createBaseTextNormOriginalMapping(getContainer().getBaseText()));
		}

		int offset = -1;

		// set the bigger and smaller text
		STextualDS biggerText = baseText;

		if (normalizedBaseText.length() >= normalizedOtherText.length()) {
			// if the other text fits into the base text by size
			offset = indexOfOmitChars(normalizedBaseText.toLowerCase(), normalizedOtherText.toLowerCase(), true, ((MergerProperties) getProperties()).getPunctuations());
		} // if the other text fits into the base text by size
		else { // if the base text fits into the other text by size
			offset = indexOfOmitChars(normalizedOtherText.toLowerCase(), normalizedBaseText.toLowerCase(), true, ((MergerProperties) getProperties()).getPunctuations());
			biggerText = otherText;
		} // if the base text fits into the other text by size

		if (offset != -1) { // one of the texts is alignable to the other
							// next step: get all tokens of the other text
			List<SToken> textTokens = new Vector<SToken>();
			for (Edge e : otherText.getSDocumentGraph().getInEdges(otherText.getSId())) {
				// get all tokens of the other text
				if (e instanceof STextualRelation) {
					textTokens.add(((STextualRelation) e).getSToken());
				}
			} // get all tokens of the other text
			for (SToken otherTextToken : textTokens) {
				// for every token in the other text First, search in the
				// equivalence map for the token
				SToken baseTextToken = (SToken) equivalenceMap.get(otherTextToken);
				if (baseTextToken == null) {
					// The other text token does not have an equivalent token in
					// the base text. Try to create it. get the start and end
					// value of the token in the other text
					int otherTextTokenStart = getContainer().getAlignedTokenStart(otherText, otherTextToken);
					int otherTextTokenLength = getContainer().getAlignedTokenLength(otherText, otherTextToken);
					if (otherTextTokenStart != -1 && otherTextTokenLength != -1) {
						// the token has start and end
						int newStart = 0;
						int newEnd = 0;
						if (biggerText.equals(baseText)) {
							// the base text is the bigger text
							newStart = offset + otherTextTokenStart;
							newEnd = newStart + otherTextTokenLength;
							// set the de-normalized start and end value in the
							// base text for the new token
							newStart = getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, newStart);
							newEnd = getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, newEnd);
							if (newStart < 0) {
								throw new PepperModuleException(this, "Cannot create a token, since the SStart value is '-1' for merging '" + SaltFactory.eINSTANCE.getGlobalId(otherTextToken.getSElementId()) + "' into '" + SaltFactory.eINSTANCE.getGlobalId(baseText.getSDocumentGraph().getSElementId()) + "'.");
							}
							if (newEnd < 0) {
								throw new PepperModuleException(this, "Cannot create a token, since the SEnd value is '-1' for merging '" + SaltFactory.eINSTANCE.getGlobalId(otherTextToken.getSElementId()) + "' ('" + otherTextToken.getSDocumentGraph().getSText(otherTextToken) + "') into '" + SaltFactory.eINSTANCE.getGlobalId(baseText.getSDocumentGraph().getSElementId()) + "'.");
							}
							// create the new token in the base text with the
							// new start and end value
							baseTextToken = baseText.getSDocumentGraph().createSToken(baseText, newStart, newEnd);
						} // the base text is the bigger text
						else { // the base text is the smaller text
								// compute the new start and end
							newStart = otherTextTokenStart - offset;
							newEnd = newStart + otherTextTokenLength;

							if (newStart >= 0 && newEnd <= normalizedBaseText.length()) {
								// the new token would be in the interval of the
								// base text.
								newStart = getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, newStart);
								newEnd = getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, newEnd);
								if (newStart < 0) {
									throw new PepperModuleException(this, "Cannot create a token, since the SStart value is '-1' for merging '" + SaltFactory.eINSTANCE.getGlobalId(otherTextToken.getSElementId()) + "' into '" + SaltFactory.eINSTANCE.getGlobalId(baseText.getSDocumentGraph().getSElementId()) + "'.");
								}
								if (newEnd < 0) {
									throw new PepperModuleException(this, "Cannot create a token, since the SEnd value is '-1' for merging '" + SaltFactory.eINSTANCE.getGlobalId(otherTextToken.getSElementId()) + "' ('" + otherTextToken.getSDocumentGraph().getSText(otherTextToken) + "') into '" + SaltFactory.eINSTANCE.getGlobalId(baseText.getSDocumentGraph().getSElementId()) + "'.");
								}
								baseTextToken = baseText.getSDocumentGraph().createSToken(baseText, newStart, newEnd);
								// mark the new token as equivalent
								equivalenceMap.put(otherTextToken, baseTextToken);
							} // the new token would be in the interval of the
								// base text.

						} // the base text is the smaller text
					} // the token has start and end
				} // The other text token does not have an equivalent token in
					// the base text. Try to create it.
				if (baseTextToken != null) {
					// there already is an equivalent token or a token was
					// created move the annos to the new token
					SaltFactory.eINSTANCE.moveSAnnotations(otherTextToken, baseTextToken);
					SaltFactory.eINSTANCE.moveSMetaAnnotations(otherTextToken, baseTextToken);
				} // there already is an equivalent token or a token was created
			} // for every token in the other text
		}// one of the texts is alignable to the other
			// move the annotations from the other text to the base text
		SaltFactory.eINSTANCE.moveSAnnotations(otherText, baseText);
		SaltFactory.eINSTANCE.moveSMetaAnnotations(otherText, baseText);
	}

	/**
	 * This method merges the Document content of the other {@link SDocument} to
	 * the base {@link SDocument} and uses the set of {@link SToken} which are
	 * contained in the other {@link SDocument} but not in the base
	 * {@link SDocument} to determine which {@link SToken} has no equivalent in
	 * the base {@link SDocument}.
	 * 
	 * @param base
	 * @param other
	 * @param nonEquivalentTokenInOtherTexts
	 * @param equivalenceMap
	 *            Map with tokens of the other document as key and their
	 *            equivalent tokens in the base
	 * @return
	 */
	protected void mergeDocumentStructure(SDocument base, SDocument other) {
		SDocumentGraph fromGraph = other.getSDocumentGraph();
		SDocumentGraph toGraph = base.getSDocumentGraph();
		MergeHandler handler = new MergeHandler(node2NodeMap, fromGraph, toGraph, getContainer().baseText, getContainer());

		EList<SNode> tokens = fromGraph.getSRoots();
		if ((tokens == null) || (tokens.size() == 0)) {
			throw new PepperModuleException(this, "Cannot start the traversing for merging document-structure, since no tokens exist for document '" + SaltFactory.eINSTANCE.getGlobalId(fromGraph.getSDocument().getSElementId()) + "'.");
		}

		logger.trace("[Merger] Merging higher document-structure for [{}, {}]", SaltFactory.eINSTANCE.getGlobalId(base.getSElementId()), SaltFactory.eINSTANCE.getGlobalId(other.getSElementId()));
		fromGraph.traverse(tokens, GRAPH_TRAVERSE_TYPE.TOP_DOWN_DEPTH_FIRST, "merger_" + SaltFactory.eINSTANCE.getGlobalId(base.getSElementId()), handler, false);
		// finally merge pointing relations
		handler.mergeSPointingRelations(fromGraph, toGraph);
		logger.trace("[Merger] Done with merging higher document-structure for [{}, {}]", SaltFactory.eINSTANCE.getGlobalId(base.getSElementId()), SaltFactory.eINSTANCE.getGlobalId(other.getSElementId()));
	}

	class NodeParameters {
		String canonicalClassName;
		Map<String, Integer> outgoingCount;
		Map<String, Integer> inboundCount;

		public NodeParameters(SNode n, SGraph g) {
			canonicalClassName = n.getClass().getCanonicalName();
			addEdges(inboundCount, g.getInEdges(n.getSId()));
			addEdges(outgoingCount, g.getOutEdges(n.getSId()));
		}

		private void addEdges(Map<String, Integer> map, List<Edge> edges) {
			for (Edge e : edges) {
				Integer i;
				if (map.containsKey(e.getClass().getCanonicalName())) {
					i = map.get(e.getClass().getCanonicalName()) + 1;
				} else {
					i = 1;
				}
				inboundCount.put(e.getClass().getCanonicalName(), i);
			}
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof NodeParameters)) {
				return false;
			}
			NodeParameters param = (NodeParameters) other;
			if (!param.canonicalClassName.equals(this.canonicalClassName)) {
				return false;
			}
			if (param.outgoingCount.size() != this.outgoingCount.size()) {
				return false;
			}
			if (!isMapEqual(param.inboundCount, this.inboundCount)) {
				return false;
			}
			if (!isMapEqual(param.outgoingCount, this.outgoingCount)) {
				return false;
			}
			return true;
		}

		private boolean isMapEqual(Map<String, Integer> map1, Map<String, Integer> map2) {
			for (String key : map1.keySet()) {
				if (map1.get(key) != map2.get(key)) {
					return false;
				}
			}
			return true;
		}
	}
}
