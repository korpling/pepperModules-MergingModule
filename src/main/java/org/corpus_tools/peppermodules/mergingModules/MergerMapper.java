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
package org.corpus_tools.peppermodules.mergingModules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.MappingSubject;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleInternalException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SGraph.GRAPH_TRAVERSE_TYPE;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Relation;
import org.corpus_tools.salt.util.SaltUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * A reference to the {@link Merger} object which has invoked this mapper.
	 **/
	private Merger merger = null;

	/**
	 * @return A reference to the {@link Merger} object which has invoked this
	 *         mapper.
	 */
	public Merger getMerger() {
		return merger;
	}

	/**
	 * @param merger
	 *            A reference to the {@link Merger} object which has invoked
	 *            this mapper.
	 */
	public void setMerger(Merger merger) {
		this.merger = merger;
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

	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		if (this.getMappingSubjects().size() != 0) {
			if (logger.isDebugEnabled()) {
				StringBuilder str = new StringBuilder();
				str.append("Start merging corpora: ");
				for (MappingSubject subj : getMappingSubjects()) {
					str.append("'");
					str.append(SaltUtil.getGlobalId(subj.getIdentifier()));
					str.append("' ");
				}
				logger.debug("[Merger] " + str.toString());
			}

			SCorpus baseCorpus = null;
			// emit corpus in base corpus-structure
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getIdentifier().getIdentifiableElement() instanceof SCorpus) {
					SCorpus sCorp = (SCorpus) subj.getIdentifier().getIdentifiableElement();
					if (sCorp.getGraph().equals(getBaseCorpusStructure())) {
						baseCorpus = sCorp;
						break;
					}
				}
			}
			// copy all annotations of corpus
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getIdentifier().getIdentifiableElement() instanceof SCorpus) {
					SCorpus sCorp = (SCorpus) subj.getIdentifier().getIdentifiableElement();
					if (sCorp == baseCorpus) {// corpus is base corpus
						subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					} else {// corpus is not base corpus
						SaltUtil.moveAnnotations(sCorp, baseCorpus);
						SaltUtil.moveMetaAnnotations(sCorp, baseCorpus);
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
					str.append(SaltUtil.getGlobalId(subj.getIdentifier()));
					str.append("' ");
				}
				logger.debug("[Merger] " + str.toString());
			}

			// base subject containing the base document
			MappingSubject baseSubject = chooseBaseDocument();
			if (baseSubject == null) {
				throw new PepperModuleInternalException(this, "No base document could have been computed. ");
			}
			// base document
			SDocument baseDocument = (SDocument) baseSubject.getIdentifier().getIdentifiableElement();
			setBaseDocument(baseDocument);

			// copy all annotations of document
			for (MappingSubject subj : getMappingSubjects()) {
				if (subj.getIdentifier().getIdentifiableElement() instanceof SDocument) {
					SDocument sDoc = (SDocument) subj.getIdentifier().getIdentifiableElement();
					if (sDoc != getBaseDocument()) {
						// document is not base corpus
						SaltUtil.moveAnnotations(sDoc, baseDocument);
						SaltUtil.moveMetaAnnotations(sDoc, baseDocument);
					}
				}
			}

			mergeDocumentStructures(baseSubject);

			// store base subject to delete others from list, since they
			// already
			// have been deleted
			MappingSubject baseSubj = null;
			// set base document to completed and remove all the others
			for (MappingSubject subj : getMappingSubjects()) {
				SDocument sDoc = ((SDocument) subj.getIdentifier().getIdentifiableElement());
				if (sDoc != getBaseDocument()) {
					subj.setMappingResult(DOCUMENT_STATUS.DELETED);
				} else {
					subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					baseSubj = subj;
				}
			}
			getMappingSubjects().clear();
			getMappingSubjects().add(baseSubj);
		}
		if (getMerger() != null) {
			getMerger().releaseMergerMapper();
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
		if (baseSubject != null) {
			// This is only for the JUnit tests
			if (getBaseDocument() == null) {
				setBaseDocument((SDocument) baseSubject.getIdentifier().getIdentifiableElement());
			}
			getContainer().setBaseDocument(getBaseDocument());
		}
		SDocument baseDocument = (SDocument) baseSubject.getIdentifier().getIdentifiableElement();

		if ((baseSubject.getDocumentController() != null) && (getPepperMapperController() != null)) {
			logger.trace("[Merger] Try to wake up base document {}. ", baseSubject.getDocumentController().getGlobalId());
			// awake document
			getPepperMapperController().getPermissionForProcessDoument(baseSubject.getDocumentController());
			baseSubject.getDocumentController().awake();
			logger.trace("[Merger] Successfully woke up base document {}. ", baseSubject.getDocumentController().getGlobalId());
		}

		// normalize all texts of base document, therefore the base document
		// needs to be woken up
		normalizePrimaryTexts(baseDocument);

		// merge two document-structures pairwise
		for (MappingSubject subj : this.getMappingSubjects()) {
			// for all documents
			SDocument otherDocument = (SDocument) subj.getIdentifier().getIdentifiableElement();
			if (otherDocument != getBaseDocument()) {
				if ((subj.getDocumentController() != null) && (getPepperMapperController() != null)) {
					logger.trace("[Merger] Try to wake up other document {}. ", subj.getDocumentController().getGlobalId());
					// awake document
					getPepperMapperController().getPermissionForProcessDoument(subj.getDocumentController());
					subj.getDocumentController().awake();
					logger.trace("[Merger] Successfully woke up other document {}. ", subj.getDocumentController().getGlobalId());
				}

				normalizePrimaryTexts(otherDocument);

				logger.debug("[Merger] Start merging of base document '{}' with {}. ", SaltUtil.getGlobalId(baseDocument.getIdentifier()), SaltUtil.getGlobalId(subj.getIdentifier()));
				// merge the document content
				mergeDocumentStructures(baseDocument, otherDocument);

				// frees memory from other document
				if (!isTestMode) {
					getContainer().finishDocument(otherDocument);
				}
				if (subj.getDocumentController() != null) {
					getMerger().done(otherDocument.getIdentifier(), DOCUMENT_STATUS.DELETED);
				}
			}
		}

		// frees memory from base document
		if (!isTestMode) {
			getContainer().finishDocument(baseDocument);
		}

		if (logger.isDebugEnabled()) {
			StringBuilder debug = new StringBuilder();

			if (matchingTexts.size() > 0) {
				debug.append("[Merger] mergable texts:\n");
				int i = 1;
				for (Pair<Pair<String, String>, Pair<String, String>> pair : matchingTexts) {
					if (i > 1) {
						debug.append("\n");
					}
					i++;
					String baseId = pair.getLeft().getLeft();
					String otherId = pair.getRight().getLeft();
					String format = "\t%-" + (baseId.length() > otherId.length() ? baseId.length() : otherId.length()) + "s: ";
					debug.append("<base> \t");
					debug.append(String.format(format, baseId));
					debug.append(pair.getLeft().getRight().substring("<base>".length()));
					debug.append("\n");
					debug.append("<other>\t");
					debug.append(String.format(format, otherId));
					debug.append(pair.getRight().getRight());
					debug.append("\n");
				}
			}
			if (noMatchingTexts.size() > 0) {
				debug.append("[Merger] NOT mergable texts:\n");
				for (Pair<String, String> text : noMatchingTexts) {
					if (text.getRight().startsWith("<base>")) {
						debug.append("<base> ");
						debug.append("\t");
						debug.append(text.getLeft());
						debug.append("\t");
						debug.append(text.getRight().substring("<base>".length()));
						debug.append("\n");
					} else {
						debug.append("<other>");
						debug.append("\t");
						debug.append(text.getLeft());
						debug.append("\t");
						debug.append(text.getRight());
						debug.append("\n");
					}
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
	 * mapping partner for the current node.
	 * 
	 * @param baseDoc
	 *            {@link SDocument} in which all elements from otherDoc are
	 *            inserted
	 * @param otherDoc
	 *            {@link SDocument} to be mapped into baseDoc
	 */
	private void mergeDocumentStructures(SDocument baseDoc, SDocument otherDoc) {
		if ((baseDoc.getDocumentGraph() != null) && (otherDoc.getDocumentGraph() != null)) {
			int initialSize = getBaseDocument().getDocumentGraph().getNodes().size();
			if (otherDoc.getDocumentGraph().getNodes().size() > initialSize) {
				initialSize = otherDoc.getDocumentGraph().getNodes().size();
			}

			node2NodeMap = new HashMap<>(initialSize);
			boolean alignedTexts = false;
			if (otherDoc.getDocumentGraph().getTextualDSs() != null) {
				// there should be texts
				logger.trace("[Merger] " + "Aligning the texts of {} with text in base document. ", SaltUtil.getGlobalId(otherDoc.getIdentifier()));

				Set<SToken> nonEquivalentTokensOfOtherText = new HashSet<>();
				nonEquivalentTokensOfOtherText.addAll(otherDoc.getDocumentGraph().getTokens());

				// align all texts and create the nonEquivalentTokenSets
				// / base text -- < Other Document -- nonEquivalentTokens >
				alignedTexts = alignAllTexts(getBaseDocument(), otherDoc);
			} else {
				// there are no texts. So, just copy everything into
				// the base document graph
				logger.warn("There is no text in document {} to be merged. Will not copy the tokens!", SaltUtil.getGlobalId(otherDoc.getIdentifier()));
			}

			if (alignedTexts) {
				// if mergable texts have been found

				SDocumentGraph otherGraph = otherDoc.getDocumentGraph();
				SDocumentGraph baseGraph = baseDoc.getDocumentGraph();
				MergeHandler handler = new MergeHandler(node2NodeMap, otherGraph, baseGraph, getContainer());
				handler.setProperties((MergerProperties) getProperties());

				List<SNode> roots = getRoots(otherGraph);
				if ((roots == null) || (roots.size() == 0)) {
					logger.warn("Cannot start the traversing for merging document-structure, since no tokens exist for document '" + SaltUtil.getGlobalId(otherGraph.getDocument().getIdentifier()) + "'.");
				} else {
					logger.trace("[Merger] Merging higher document-structure for [{}, {}]", SaltUtil.getGlobalId(baseDoc.getIdentifier()), SaltUtil.getGlobalId(otherDoc.getIdentifier()));
					otherGraph.traverse(roots, GRAPH_TRAVERSE_TYPE.TOP_DOWN_DEPTH_FIRST, "merger_" + SaltUtil.getGlobalId(baseDoc.getIdentifier()), handler, false);
					// finally merge pointing relations
					handler.mergeSPointingRelations(otherGraph, baseGraph);
					logger.trace("[Merger] Done with merging higher document-structure for [{}, {}]", SaltUtil.getGlobalId(baseDoc.getIdentifier()), SaltUtil.getGlobalId(otherDoc.getIdentifier()));
				}
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
	 * struct1 and span2. Note that undominated tokens are also treated as roots.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<SNode> getRoots(SDocumentGraph other) {
		Set<SNode> retSet = new LinkedHashSet<>();
		List<SRelation> relations = new ArrayList<>();
		relations.addAll((List<SRelation>) (List<? extends SRelation>) other.getSpanningRelations());
		relations.addAll((List<SRelation>) (List<? extends SRelation>) other.getDominanceRelations());
		Set<SNode> notRootElements = new HashSet<>();
		retSet.addAll(other.getTokens()); // initially assume all tokens are roots, they will be removed if found to be dominated
		for (SRelation<SNode, SNode> relation : relations) {
			// mark destination as no root
			if (!notRootElements.contains(relation.getTarget())) {
				notRootElements.add(relation.getTarget());
			}
			// if source is not also a destination
			if ((!notRootElements.contains(relation.getSource())) && (!retSet.contains(relation.getSource()))) {
				retSet.add(relation.getSource());
			}
			// remove wrong stored nodes in retList
			if (retSet.contains(relation.getTarget())) {
				retSet.remove(relation.getTarget());
			}
		}
		List<SNode> retVal = null;
		if (!retSet.isEmpty()) {
			retVal = new ArrayList<>(retSet);
		}
		return (retVal);
	}

	/** the {@link TokenMergeContainer} instance **/
	protected TokenMergeContainer container = null;

	public TokenMergeContainer getContainer() {
		return container;
	}

	/*
	 * *******************************************************************
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
			if (subj.getIdentifier() == null) {
				throw new PepperModuleException(this, "A MappingSubject does not contain a document object. This seems to be a bug. ");
			}
			if (subj.getIdentifier().getIdentifiableElement() instanceof SDocument) {
				SDocument document = (SDocument) subj.getIdentifier().getIdentifiableElement();
				// check if document and document structure is given
				if (document == null) {
					throw new PepperModuleException(this, "A MappingSubject does not contain a document object. This seems to be a bug. ");
				}
				if (getBaseCorpusStructure() == null) {
					// current number of SNodes and SRelations contained in
					// document structure
					int currNumOfElements = 0;
					if (document.getDocumentGraph() != null) {
						currNumOfElements = (document.getDocumentGraph().getNodes().size() + document.getDocumentGraph().getRelations().size());
					} else {
						currNumOfElements = subj.getDocumentController().getSize_nodes() + subj.getDocumentController().getSize_relations();
					}

					if (maxNumOfElements < currNumOfElements) {
						// numOfElements is less than current sum of nodes and
						// relations, current document structure is the bigger
						// one

						maxNumOfElements = currNumOfElements;
						baseSubject = subj;
					}
				} else {
					// take document which is contained in base corpus structure
					if (document.getGraph().equals(getBaseCorpusStructure())) {
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
		if (sDocument == null) {
			throw new PepperModuleException(this, "Cannot normalize Text of the document since the SDocument reference is NULL");
		}
		if (sDocument.getDocumentGraph() != null) {
			// check whether the document has any STextualDS
			List<STextualDS> sTextualDSs = sDocument.getDocumentGraph().getTextualDSs();
			for (STextualDS sTextualDS : sTextualDSs) {
				// normalize all textual datasources
				List<Integer> originalToNormalizedMapping = new ArrayList<>();
				String normalizedText = createOriginalToNormalizedMapping(sTextualDS, originalToNormalizedMapping);
				for (STextualRelation textRel : sDocument.getDocumentGraph().getTextualRelations()) {
					if (textRel.getTarget().equals(sTextualDS)) {
						SToken sToken = textRel.getSource();
						if (textRel.getStart() >= originalToNormalizedMapping.size()) {
							throw new PepperModuleInternalException(this, "Cannot find token " + SaltUtil.getGlobalId(textRel.getSource().getIdentifier()) + " in  'originalToNormalizedMapping' list. ");
						}
						// the start position of current token in normalized
						// text
						int normalizedTokenStart = originalToNormalizedMapping.get(textRel.getStart());
						// the end position of current token in normalized text
						int normalizedTokenEnd = 0;
						if (textRel.getEnd() >= (originalToNormalizedMapping.size())) {
							if (textRel.getEnd() >= (originalToNormalizedMapping.size() + 1)) {
								throw new PepperModuleInternalException(this, "textRel.getEnd() >= (originalToNormalizedMapping.size()+1). ");
							} else {
								normalizedTokenEnd = originalToNormalizedMapping.get(originalToNormalizedMapping.size() - 1) + 1;
							}
						} else {
							normalizedTokenEnd = originalToNormalizedMapping.get(textRel.getEnd());
						}
						getContainer().addAlignedToken(sTextualDS, sToken, normalizedTokenStart, normalizedTokenEnd);
					}
				}
				getContainer().addNormalizedText(sDocument, sTextualDS, normalizedText);
			}
		} else {
			throw new PepperModuleInternalException(this, "Could not compute the normalized text for document '" + SaltUtil.getGlobalId(sDocument.getIdentifier()) + "', because the document contains no document graph. May be it has not been woken up. ");
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
		char[] chr = sTextualDS.getText().toCharArray();
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
				} else {
					// one char is mapped to the empty string.
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
		 * Example1:
		 * 
		 * <pre>
		 * dipl:	" this is" 
		 * 		 01234567 
		 * norm: 	"thisis" 
		 * 			 012345 
		 * 
		 * 0->1
		 * 1->2,... Example2: dipl: " thäs is" 01234567 norm: "thaesis" 0123456
		 * 0->1 1->2 2->3 3->3 4->4 5->6 6->7
		 * </pre>
		 */
		List<Integer> normalizedToOriginalMapping = new ArrayList<>();
		int start = 0;
		char[] chr = sTextualDS.getText().toCharArray();
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
	private List<Pair<Pair<String, String>, Pair<String, String>>> matchingTexts = new ArrayList<>();
	/**
	 * A list of all texts, for which no matching partners have been found to be
	 * reported.
	 **/
	private Set<Pair<String, String>> noMatchingTexts = new HashSet<>();
	/**
	 * A list to store all texts, for which matching partners have been found.
	 * This is used to compute the correct list of {@link #noMatchingTexts}.
	 **/
	private Set<String> matchingTextsIdx = new HashSet<>();

	/**
	 * This method tries to find matching texts in base document and other
	 * document. A cross product is computed.
	 */
	private boolean alignAllTexts(SDocument baseDoc, SDocument otherDoc) {
		// ignore the base document and align all other
		if (otherDoc.getDocumentGraph() == null) {
			throw new PepperModuleDataException(this, "Cannot map document '" + SaltUtil.getGlobalId(otherDoc.getIdentifier()) + "', since it does not contain a document-structure.");
		}
		boolean retVal = false;
		if ((otherDoc.getDocumentGraph().getTextualDSs() != null) && (otherDoc.getDocumentGraph().getTextualDSs().size() > 0)) {
			// The other document has at least one text
			Set<SToken> nonEquivalentTokenInOtherTexts = new HashSet<>();

			for (STextualDS baseText : getBaseDocument().getDocumentGraph().getTextualDSs()) {
				// for all texts of the base document
				nonEquivalentTokenInOtherTexts = new HashSet<>();
				// initialize the set of nonEquivalent token.
				// Initially, all token do not have an equivalent.
				// in alignTexts, tokens which DO have an equivalent
				// are removed from the set
				if (otherDoc.getDocumentGraph().getTokens() != null) {
					nonEquivalentTokenInOtherTexts.addAll(otherDoc.getDocumentGraph().getTokens());
				}
				for (STextualDS otherText : otherDoc.getDocumentGraph().getTextualDSs()) {
					// align the current base text with all texts of
					// the other document
					boolean isAlignable = alignTexts(baseText, otherText, nonEquivalentTokenInOtherTexts, node2NodeMap);
					if (isAlignable) {
						retVal = true;
						Pair<String, String> base = new ImmutablePair<>(baseText.getId(), "<base>" + baseText.getText());
						Pair<String, String> other = new ImmutablePair<>(otherText.getId(), otherText.getText());
						matchingTexts.add(new ImmutablePair<>(base, other));
						matchingTextsIdx.add(SaltUtil.getGlobalId(otherText.getIdentifier()));
						matchingTextsIdx.add(SaltUtil.getGlobalId(baseText.getIdentifier()));
						noMatchingTexts.remove(other);
						noMatchingTexts.remove(base);

						// add matching texts to a list of all matching nodes
						node2NodeMap.put(otherText, baseText);
						mergeTokens(baseText, otherText, node2NodeMap);
					}
					if (!matchingTextsIdx.contains(SaltUtil.getGlobalId(otherText.getIdentifier()))) {
						noMatchingTexts.add(new ImmutablePair<>(otherText.getId(), otherText.getText()));
					}
				}
				if (!matchingTextsIdx.contains(SaltUtil.getGlobalId(baseText.getIdentifier()))) {
					noMatchingTexts.add(new ImmutablePair<>(baseText.getId(), "<base>" + baseText.getText()));
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

		boolean returnVal = false;
		// first we need the two normalized texts
		String normalizedBaseText = getContainer().getNormalizedText(baseText);
		if (normalizedBaseText == null) {
			throw new PepperModuleInternalException(this, "Could not align text '" + SaltUtil.getGlobalId(baseText.getIdentifier()) + "', because a normalized text for base text was not computed. ");
		}
		String normalizedOtherText = getContainer().getNormalizedText(otherText);
		if (normalizedOtherText == null) {
			throw new PepperModuleInternalException(this, "Could not align texts, because a normalized text for other text '" + SaltUtil.getGlobalId(otherText.getIdentifier()) + "' was not computed. ");
		}
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
			List<SToken> textTokens = new ArrayList<>();
			for (Relation e : smallerText.getGraph().getInRelations(smallerText.getId())) {
				// get all tokens of the smaller text
				if (e instanceof STextualRelation) {
					textTokens.add(((STextualRelation) e).getSource());
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
							else {
								// if the base text is the smaller text:
								// smallerText = baseText
								// smallerTextToken = baseTextToken
								getContainer().addTokenMapping(smallerTextToken, biggerTextToken, biggerText);
								equivalenceMap.put(biggerTextToken, smallerTextToken);
								nonEquivalentTokenInOtherTexts.remove(biggerTextToken);
							} // if the base text is the smaller text

						} // start and lengths are identical. We found
							// anequivalence class
					}

				} else {
					// the other token has either no start or no length ->ERROR
					throw new PepperModuleException(this, "The SToken " + smallerText.getId() + " of the STextualDS " + smallerText.getId() + " has no proper start or length. It was probably not aligned correctly.");
				} // the other token has either no start or no length -> ERROR
			}

		}
		// get base text
		return returnVal;
	}

	/*
	 * *******************************************************************
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
			List<Integer> normalizedToOriginalMapping = new ArrayList<>();
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
			List<SToken> textTokens = new ArrayList<>();
			for (Relation e : otherText.getGraph().getInRelations(otherText.getId())) {
				// get all tokens of the other text
				if (e instanceof STextualRelation) {
					textTokens.add(((STextualRelation) e).getSource());
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
								throw new PepperModuleException(this, "Cannot create a token, since the SStart value is '-1' for merging '" + SaltUtil.getGlobalId(otherTextToken.getIdentifier()) + "' into '" + SaltUtil.getGlobalId(baseText.getGraph().getIdentifier()) + "'.");
							}
							if (newEnd < 0) {
								throw new PepperModuleException(this, "Cannot create a token, since the SEnd value is '-1' for merging '" + SaltUtil.getGlobalId(otherTextToken.getIdentifier()) + "' ('" + otherTextToken.getGraph().getText(otherTextToken) + "') into '" + SaltUtil.getGlobalId(baseText.getGraph().getIdentifier()) + "'.");
							}
							// create the new token in the base text with the
							// new start and end value
							baseTextToken = baseText.getGraph().createToken(baseText, newStart, newEnd);
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
									throw new PepperModuleException(this, "Cannot create a token, since the SStart value is '-1' for merging '" + SaltUtil.getGlobalId(otherTextToken.getIdentifier()) + "' into '" + SaltUtil.getGlobalId(baseText.getGraph().getIdentifier()) + "'.");
								}
								if (newEnd < 0) {
									throw new PepperModuleException(this, "Cannot create a token, since the SEnd value is '-1' for merging '" + SaltUtil.getGlobalId(otherTextToken.getIdentifier()) + "' ('" + otherTextToken.getGraph().getText(otherTextToken) + "') into '" + SaltUtil.getGlobalId(baseText.getGraph().getIdentifier()) + "'.");
								}
								baseTextToken = baseText.getGraph().createToken(baseText, newStart, newEnd);
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
					SaltUtil.moveAnnotations(otherTextToken, baseTextToken);
					SaltUtil.moveMetaAnnotations(otherTextToken, baseTextToken);
				} // there already is an equivalent token or a token was created
			} // for every token in the other text
		} // one of the texts is alignable to the other
			// move the annotations from the other text to the base text
		SaltUtil.moveAnnotations(otherText, baseText);
		SaltUtil.moveMetaAnnotations(otherText, baseText);
	}
}
