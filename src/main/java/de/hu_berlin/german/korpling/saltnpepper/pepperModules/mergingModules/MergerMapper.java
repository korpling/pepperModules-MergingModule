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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.BasicEList;
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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpanningRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;

/**
 * This class does the real merging, the main task is to merge a set of document
 * graphs.
 * 
 * @author Mario Frank
 * @author Jakob Schmoling
 * @author Florian Zipser
 */
public class MergerMapper extends PepperMapperImpl implements PepperMapper {

	private static final Logger logger = LoggerFactory.getLogger(Merger.MODULE_NAME);

	protected boolean isTestMode = false;

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
	 * The {@link SDocument} which is the base document. That means, that all
	 * document-structures are merged into this document.
	 */
	private SDocument baseDocument = null;

	/**
	 * @return the {@link SDocument} which is the base document. That means,
	 *         that all document-structures are merged into this document.
	 */
	public SDocument getBaseDocument() {
		return baseDocument;
	}

	/**
	 * 
	 * @param baseDocument
	 *            the {@link SDocument} which is the base document. That means,
	 *            that all document-structures are merged into this document.
	 */
	public void setBaseDocument(SDocument baseDocument) {
		this.baseDocument = baseDocument;
	}

	/**
	 * This method is called by the Pepper framework and merges a set of given
	 * {@link SDocumentGraph} objects.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		this.initialize();
		if (this.getMappingSubjects().size() > 1) {

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

			// base subject containing the base document
			MappingSubject baseSubject = chooseBaseDocument();
			if (baseSubject == null) {
				throw new PepperModuleException(this, "This might be a bug, no base document could have been computed.");
			}
			// base document
			SDocument baseDocument= (SDocument) baseSubject.getSElementId().getSIdentifiableElement();
			setBaseDocument(baseDocument);

			//awake document
			getPepperMapperController().getPermissionForProcessDoument(baseSubject.getDocumentController());
			baseSubject.getDocumentController().awake();
			
			// copy all annotations of document
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
					SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
					if (sDoc != getBaseDocument()) {// document is not base
													// corpus
						SaltFactory.eINSTANCE.moveSAnnotations(sDoc, baseDocument);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(sDoc, baseDocument);
					}
				}
			}

			mergeDocumentStructures(baseSubject);

		
			getMappingSubjects().clear();
			baseSubject.setMappingResult(DOCUMENT_STATUS.COMPLETED);
			getMappingSubjects().add(baseSubject);
			
//			// set base document to completed and remove all the others
//			for (MappingSubject subj : getMappingSubjects()) {
//				SDocument sDoc = ((SDocument) subj.getSElementId().getSIdentifiableElement());
//				if (sDoc != getBaseDocument()) {
//					subj.setMappingResult(DOCUMENT_STATUS.DELETED);
//					if (!isTestMode) {
//						getContainer().finishDocument((SDocument) subj.getSElementId().getSIdentifiableElement());
//					}
//				} else {
//					subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
//					if (!isTestMode) {
//						getContainer().finishDocument(sDoc);
//					}
//				}
//			}
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * A map to relate nodes of one graph to nodes to another graph.
	 */
	private Map<SNode, SNode> node2NodeMap = null;

	/**
	 * Merges all document-structures pairwise by calling
	 * {@link #mergeDocumentStructures(SDocument, SDocument)}.
	 */
	public void mergeDocumentStructures(MappingSubject baseSubject) {
		// normalize all texts
		for (MappingSubject subj : this.getMappingSubjects()) {
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
				this.normalizePrimaryTexts(sDoc);
			}
		}

		if (baseSubject != null) {
			// This is only for the JUnit tests

			if (getBaseDocument() == null) {
				setBaseDocument((SDocument) baseSubject.getSElementId().getSIdentifiableElement());
			}
			getContainer().setBaseDocument(getBaseDocument());
		}

		// merge two document-structures pairwise
		for (MappingSubject subj : this.getMappingSubjects()) {
			//awake document
			getPepperMapperController().getPermissionForProcessDoument(subj.getDocumentController());
			subj.getDocumentController().awake();
			// for all documents
			SDocument otherDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
			if (otherDoc != getBaseDocument()) {
				// merge the document content
				mergeDocumentStructures((SDocument) baseSubject.getSElementId().getSIdentifiableElement(), otherDoc);
			}
			//mark other document to delete it
			System.out.println("----------------> Call done for '"+otherDoc.getSElementId()+"': "+ subj.getMappingResult());
			getPepperMapperController().done(otherDoc.getSElementId(), DOCUMENT_STATUS.DELETED);
		}
		
		if (logger.isDebugEnabled()) {
			StringBuilder debug = new StringBuilder();

			if (matchingTexts.size() > 0) {
				debug.append("[Merger] mergable texts:\n");
				int i = 1;
				for (Pair<STextualDS, STextualDS> pair : matchingTexts) {
					if (i > 1) {
						debug.append("\n");
					}
					i++;
					String baseId = SaltFactory.eINSTANCE.getGlobalId(pair.getLeft().getSElementId());
					String otherId = SaltFactory.eINSTANCE.getGlobalId(pair.getRight().getSElementId());
					String format = "\t%-" + (baseId.length() > otherId.length() ? baseId.length() : otherId.length()) + "s: ";
					debug.append("<base> \t");
					debug.append(String.format(format, baseId));
					debug.append(pair.getLeft().getSText());
					debug.append("\n");
					debug.append("<other>\t");
					debug.append(String.format(format, otherId));
					debug.append(pair.getRight().getSText());
					debug.append("\n");
				}
			}
			if (noMatchingTexts.size() > 0) {
				debug.append("[Merger] NOT mergable texts:\n");
				for (STextualDS text : noMatchingTexts) {
					if (getBaseDocument().equals(text.getSDocumentGraph().getSDocument())){
						debug.append("<base> ");
						debug.append("\t");
					}else{
						debug.append("<other>");
						debug.append("\t");
					}
					debug.append(SaltFactory.eINSTANCE.getGlobalId(text.getSElementId()));
					debug.append("\t");
					debug.append(text.getSText());
					debug.append("\n");
				}
			}
			logger.debug(debug.toString());
		}
	}

	/**
	 * This method merges the Document content of the other {@link SDocument} to
	 * the base {@link SDocument} and uses the set of {@link SToken} which are
	 * contained in the other {@link SDocument} but not in the base
	 * {@link SDocument} to determine which {@link SToken} has no equivalent in
	 * the base {@link SDocument}. <br/>
	 * To merge the other graph into base graph, the other graph is traversed by
	 * top down depth first order. On backtracking (nodeLeft) for each node
	 * (current node) all childs are computed. For these child mapping partners
	 * in base graph are computed, if mapping childs have been found, their
	 * common parent (if there are more than one, the first one is taken) is the
	 * maaping partner for the current node.
	 * 
	 * @param baseDoc
	 * @param otherDoc
	 * @param nonEquivalentTokenInOtherTexts
	 * @param equivalenceMap
	 *            Map with tokens of the other document as key and their
	 *            equivalent tokens in the base
	 * @return
	 */
	private void mergeDocumentStructures(SDocument baseDoc, SDocument otherDoc) {
		int initialSize = getBaseDocument().getSDocumentGraph().getSNodes().size();
		if (otherDoc.getSDocumentGraph().getSNodes().size() > initialSize) {
			initialSize = otherDoc.getSDocumentGraph().getSNodes().size();
		}
		node2NodeMap = new HashMap<SNode, SNode>(initialSize);
		boolean alignedTexts = false;
		if (otherDoc.getSDocumentGraph().getSTextualDSs() != null) {
			// there should be texts
			logger.trace("[Merger] " + "Aligning the texts of {} with text in base document. ", SaltFactory.eINSTANCE.getGlobalId(otherDoc.getSElementId()));

			Set<SToken> nonEquivalentTokensOfOtherText = new HashSet<SToken>();
			nonEquivalentTokensOfOtherText.addAll(otherDoc.getSDocumentGraph().getSTokens());

			// align all texts and create the nonEquivalentTokenSets
			// / base text -- < Other Document -- nonEquivalentTokens >
			alignedTexts = alignAllTexts(getBaseDocument(), otherDoc);
		} else {
			// there are no texts. So, just copy everything into
			// the base document graph
			logger.warn("There is no text in document {} to be merged. Will not copy the tokens!", SaltFactory.eINSTANCE.getGlobalId(otherDoc.getSElementId()));
		}

		if (alignedTexts) {
			// if mergable texts have been found
			
			SDocumentGraph otherGraph = otherDoc.getSDocumentGraph();
			SDocumentGraph baseGraph = baseDoc.getSDocumentGraph();
			MergeHandler handler = new MergeHandler(node2NodeMap, otherGraph, baseGraph, getContainer());
			handler.setProperties((MergerProperties) getProperties());

			EList<SNode> roots = getRoots(otherGraph);
			if ((roots == null) || (roots.size() == 0)) {
				logger.warn("Cannot start the traversing for merging document-structure, since no tokens exist for document '" + SaltFactory.eINSTANCE.getGlobalId(otherGraph.getSDocument().getSElementId()) + "'.");
			} else {
				logger.trace("[Merger] Merging higher document-structure for [{}, {}]", SaltFactory.eINSTANCE.getGlobalId(baseDoc.getSElementId()), SaltFactory.eINSTANCE.getGlobalId(otherDoc.getSElementId()));
				otherGraph.traverse(roots, GRAPH_TRAVERSE_TYPE.TOP_DOWN_DEPTH_FIRST, "merger_" + SaltFactory.eINSTANCE.getGlobalId(baseDoc.getSElementId()), handler, false);
				// finally merge pointing relations
				handler.mergeSPointingRelations(otherGraph, baseGraph);
				logger.trace("[Merger] Done with merging higher document-structure for [{}, {}]", SaltFactory.eINSTANCE.getGlobalId(baseDoc.getSElementId()), SaltFactory.eINSTANCE.getGlobalId(otherDoc.getSElementId()));
			}
		}
	}

	/**
	 * Emits root nodes, which are roots for {@link SDominanceRelation} and
	 * {@link SSpanningRelation} only. For instance for the sample:
	 *
	 * <pre>
	 *       struct1
	 *     //      ||
	 *   span1     ||   span2
	 * 	/    \     ||    |
	 * tok1	tok2  tok3  tok4
	 * </pre>
	 * 
	 * the nodes:
	 * 
	 * struct1 and span2 are returned, even if a pointing relation connects
	 * struct1 and span2.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private EList<SNode> getRoots(SDocumentGraph other) {
		HashSet<SNode> retSet = new LinkedHashSet<SNode>();
		EList<SRelation> relations = new BasicEList<SRelation>();
		relations.addAll((EList<SRelation>) (EList<? extends SRelation>) other.getSSpanningRelations());
		relations.addAll((EList<SRelation>) (EList<? extends SRelation>) other.getSDominanceRelations());
		HashSet<SNode> notRootElements = new HashSet<SNode>();
		for (SRelation relation : relations) {
			// mark destination as no root
			if (!notRootElements.contains(relation.getSTarget()))
				notRootElements.add(relation.getSTarget());
			// if source is not also a destination
			if ((!notRootElements.contains(relation.getSSource())) && (!retSet.contains(relation.getSSource())))
				retSet.add(relation.getSSource());
			// remove wrong stored nodes in retList
			if (retSet.contains(relation.getSTarget()))
				retSet.remove(relation.getSTarget());
		}
		EList<SNode> retVal = null;
		if (!retSet.isEmpty()) {
			retVal = new BasicEList<SNode>(retSet);
		}
		return (retVal);
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
	 * have to be merged in in further processing. <br/>
	 * The base document is the one which is the one contained in the base
	 * corpus structure or if the corpus structure is not set, the one having
	 * the most nodes and relations.
	 * 
	 * @return The base {@link MappingSubject} containing the base document
	 */
	protected MappingSubject chooseBaseDocument() {
		MappingSubject baseSubject = null;
		// maximum number of SNodes and SRelations contained in document
		// structure
		int maxNumOfElements = 0;
		for (MappingSubject subj : getMappingSubjects()) {
			if (subj.getSElementId() == null) {
				throw new PepperModuleException(this, "A MappingSubject does not contain a document object. This seems to be a bug. ");
			}
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument) {
				SDocument sDoc = (SDocument) subj.getSElementId().getSIdentifiableElement();
				// check if document and document structure is given
				if (sDoc == null) {
					throw new PepperModuleException(this, "A MappingSubject does not contain a document object. This seems to be a bug. ");
				} 
//				else if (sDoc.getSDocumentGraph() == null) {
//					logger.warn("The document '" + SaltFactory.eINSTANCE.getGlobalId(sDoc.getSElementId()) + "' does not contain a document structure. Therefore it was ignored. ");
//					continue;
//				}
				if (getBaseCorpusStructure() == null) {
					// current number of SNodes and SRelations contained in
					// document structure
					int currNumOfElements = 0;
					if (sDoc.getSDocumentGraph()!= null){
						currNumOfElements = (sDoc.getSDocumentGraph().getSNodes().size() + sDoc.getSDocumentGraph().getSRelations().size());
					}else{
						currNumOfElements = subj.getDocumentController().getSize_nodes() + subj.getDocumentController().getSize_relations();
					}
					
//					int currNumOfElements = (sDoc.getSDocumentGraph().getSNodes().size() + sDoc.getSDocumentGraph().getSRelations().size());
					if (maxNumOfElements < currNumOfElements) {
						// numOfElements is less than current sum of nodes and
						// relations, current document structure is the bigger
						// one

						maxNumOfElements = currNumOfElements;
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
		return baseSubject;
	}

	/**
	 * Normalizes all primary texts of the given {@link SDocument}. The
	 * normalized text corresponding to its original is added to the
	 * {@link TokenMergeContainer}. Also each token corresponding to its start
	 * and end position in the normalized text is added to the
	 * {@link TokenMergeContainer}. <br/>
	 * Note: only the normalization is done. The equivalent {@link SToken} are
	 * not determined in any way. For this functionality, you need to use
	 * {@link alignDocuments}.
	 * 
	 * @param sDocument
	 *            the {@link SDocument} for which the textual layer should be
	 *            normalized.
	 */
	protected void normalizePrimaryTexts(SDocument sDocument) {
		if (sDocument == null)
			throw new PepperModuleException(this, "Cannot normalize Text of the document since the SDocument reference is NULL");
		if (sDocument.getSDocumentGraph() != null) {
			// check whether the document has any STextualDS
			List<STextualDS> sTextualDSs = sDocument.getSDocumentGraph().getSTextualDSs();
			for (STextualDS sTextualDS : sTextualDSs) {
				// normalize all textual datasources

				List<Integer> originalToNormalizedMapping = new ArrayList<Integer>();
				String normalizedText = createOriginalToNormalizedMapping(sTextualDS, originalToNormalizedMapping);
				for (STextualRelation textRel : sDocument.getSDocumentGraph().getSTextualRelations()) {
					if (textRel.getSTextualDS().equals(sTextualDS)) {
						SToken sToken = textRel.getSToken();
						if (textRel.getSStart() >= originalToNormalizedMapping.size()) {
							throw new PepperModuleException(this, "Cannot find token " + SaltFactory.eINSTANCE.getGlobalId(textRel.getSToken().getSElementId()) + " in  'originalToNormalizedMapping' list. This might be a bug. ");
						}
						// the start position of current token in normalized
						// text
						int normalizedTokenStart = originalToNormalizedMapping.get(textRel.getSStart());
						// the end position of current token in normalized text
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

	/**
	 * Creates a normalized text from the given {@link STextualDS} object and
	 * returns it. Further a mapping passed as
	 * <code>originalToNormalizedMapping</code>from the given text to the
	 * normalized text is created.
	 * 
	 * @param sTextualDS
	 * @param originalToNormalizedMapping
	 * @return
	 */
	private String createOriginalToNormalizedMapping(STextualDS sTextualDS, List<Integer> originalToNormalizedMapping) {
		StringBuilder normalizedTextBuilder = new StringBuilder();
		int start = 0;
		char[] chr = sTextualDS.getSText().toCharArray();
		for (char c : chr) {
			String stringToEscape = ((MergerProperties) getProperties()).getEscapeMapping().get(String.valueOf(c));
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
		// add an additional entry for the position after the last character
		// (imagine an empty token beginning and ending at last position of the
		// text). This is necessary, because text positions are positions
		// BETWEEN characters.
		originalToNormalizedMapping.add(start++);

		String normalizedText = normalizedTextBuilder.toString();
		return normalizedText;
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
		List<Integer> normalizedToOriginalMapping = new ArrayList<Integer>();
		int start = 0;
		char[] chr = sTextualDS.getSText().toCharArray();
		for (char c : chr) {
			String stringToEscape = ((MergerProperties) getProperties()).getEscapeMapping().get(String.valueOf(c));
			if (stringToEscape == null) {
				normalizedToOriginalMapping.add(start);
			} else {
				if (stringToEscape.length() > 0) {
					char[] chr2 = stringToEscape.toCharArray();
					for (int i = 0; i < chr2.length; i++) {
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
		// add an additional entry for the position after the last character
		// (imagine an empty token beginning and ending at last position of the
		// text). This is necessary, because text positions are positions
		// BETWEEN characters.
		normalizedToOriginalMapping.add(start++);
		return normalizedToOriginalMapping;
	}
	/** A list of all pairs of matching texts to be reported. **/
	private List<Pair<STextualDS, STextualDS>> matchingTexts = new ArrayList<Pair<STextualDS, STextualDS>>();
	/** A list of all texts, for which no matching partners have been found to be reported. **/
	private Set<STextualDS> noMatchingTexts = new HashSet<STextualDS>();
	/** A list to store all texts, for which matching partners have been found. This is used to compute the correct list of {@link #noMatchingTexts}.**/
	private Set<STextualDS> matchingTextsIdx = new HashSet<STextualDS>();

	/**
	 * This method tries to find matching texts in base document and other
	 * document. A cross product is computed.
	 */
	private boolean alignAllTexts(SDocument baseDoc, SDocument otherDoc) {
		// ignore the base document and align all other
		if (otherDoc.getSDocumentGraph() == null) {
			throw new PepperModuleDataException(this, "Cannot map document '" + SaltFactory.eINSTANCE.getGlobalId(otherDoc.getSElementId()) + "', since it does not contain a document-structure.");
		}
		boolean retVal = false;
		if ((otherDoc.getSDocumentGraph().getSTextualDSs() != null) && (otherDoc.getSDocumentGraph().getSTextualDSs().size() > 0)) {
			// The other document has at least one text
			HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();

			for (STextualDS baseText : getBaseDocument().getSDocumentGraph().getSTextualDSs()) {
				// for all texts of the base document
				nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
				// initialize the set of nonEquivalent token.
				// Initially, all token do not have an equivalent.
				// in alignTexts, tokens which DO have an equivalent
				// are removed from the set
				if (otherDoc.getSDocumentGraph().getSTokens() != null) {
					nonEquivalentTokenInOtherTexts.addAll(otherDoc.getSDocumentGraph().getSTokens());
				}
				for (STextualDS otherText : otherDoc.getSDocumentGraph().getSTextualDSs()) {
					// align the current base text with all texts of
					// the other document
					boolean isAlignable = alignTexts(baseText, otherText, nonEquivalentTokenInOtherTexts, node2NodeMap);
					if (isAlignable) {
						retVal = true;
						matchingTexts.add(new ImmutablePair<STextualDS, STextualDS>(baseText, otherText));
						matchingTextsIdx.add(otherText);
						matchingTextsIdx.add(baseText);
						noMatchingTexts.remove(otherText);
						noMatchingTexts.remove(baseText);

						// add matching texts to a list of all matching nodes
						node2NodeMap.put(otherText, baseText);
						mergeTokens(baseText, otherText, node2NodeMap);
					}
					if (!matchingTextsIdx.contains(otherText)){
						noMatchingTexts.add(otherText);
					}
				}
				if (!matchingTextsIdx.contains(baseText)){
					noMatchingTexts.add(baseText);
				}
			} // for all texts of the base document
		} // The other document has at least one text
		return (retVal);
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
		if (baseText == null) {
			throw new PepperModuleException(this, "Cannot align the Text of the documents since the base SDocument reference is NULL");
		}
		if (otherText == null) {
			throw new PepperModuleException(this, "Cannot align the Text of the documents since the other SDocument reference is NULL");
		}

		// TODO REVISE THIS CODE
		boolean returnVal = false;
		// first we need the two normalized texts
		String normalizedBaseText = getContainer().getNormalizedText(baseText);
		String normalizedOtherText = getContainer().getNormalizedText(otherText);
		// set the mapping of the normalized base text to the original base text
		if (getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, 0) == -1) {
			getContainer().setBaseTextPositionByNormalizedTextPosition(baseText, this.createBaseTextNormOriginalMapping(baseText));
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

		if (offset != -1) {
			// if the normalized smaller text is contained in the normalized
			// bigger text
			returnVal = true;
			// get the tokens of the other text.
			List<SToken> textTokens = new ArrayList<SToken>();
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

						} // start and lengths are identical. We found
							// anequivalence class
					}

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
	 *            String in which is searched
	 * @param stringToSearchFor
	 *            String which is to search
	 * @param useIndexof
	 *            If this flag is set, all omit chars are removed from both
	 *            provided strings and a normal indexOf is used
	 * @param omitCharArray
	 * @return the index on success and -1 on failure
	 */
	protected int indexOfOmitChars(String stringToSearchIn, String stringToSearchFor, boolean useIndexOf, Set<Character> omitChars) {
		/* remove all omit chars from the stringToSearchFor */
		StringBuilder builder = new StringBuilder();
		char[] chr = stringToSearchFor.toCharArray();
		for (char sourceChar : chr) {
			if (!omitChars.contains(sourceChar)) {
				builder.append(sourceChar);
			}
		}
		String sourceString = builder.toString();

		if (useIndexOf) {
			builder = new StringBuilder();
			List<Integer> normalizedToOriginalMapping = new ArrayList<Integer>();
			int start = 0;
			char[] chr2 = stringToSearchIn.toCharArray();
			for (char targetChar : chr2) {
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
			char[] chr2 = sourceString.toCharArray();
			for (char sourceChar : chr2) {
				// for all chars of the string to search the char in the target
				// string and omit all omit chars in the target string
				boolean foundChar = false;
				for (int i = position; i < stringToSearchIn.length(); i++) {
					// search the current char and ignore all chars to omit in
					// the target string
					char targetChar = charsToSearchIn[i];
					if (omitChars.contains(targetChar)) {
						// ignore
						position++;
						continue;
					} // ignore
					else { // do not ignore
						if (targetChar == sourceChar) {
							// we found the matching char
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
			if (found) {
				// if the found flag is still set, we are finished, if the
				// successfulMatchCount is equal to the source string
				if (successfulMatchCount == sourceString.length()) {
					break;
				}
			}
		}

		return retVal;
	}

	/**
	 * 
	 * @param baseText
	 * @param otherText
	 * @param equivalenceMap
	 */
	protected void mergeTokens(STextualDS baseText, STextualDS otherText, Map<SNode, SNode> equivalenceMap) {
		// We want to merge the tokens of the other text into the base text.
		// first we need the two normalized texts
		String normalizedBaseText = getContainer().getNormalizedText(baseText);
		String normalizedOtherText = getContainer().getNormalizedText(otherText);

		// set the mapping of the normalized base text to the original base text
		if (getContainer().getBaseTextPositionByNormalizedTextPosition(baseText, 0) == -1) {
			getContainer().setBaseTextPositionByNormalizedTextPosition(baseText, this.createBaseTextNormOriginalMapping(baseText));
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
			List<SToken> textTokens = new ArrayList<SToken>();
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
}
