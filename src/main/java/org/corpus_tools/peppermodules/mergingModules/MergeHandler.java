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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.GraphTraverseHandler;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SGraph.GRAPH_TRAVERSE_TYPE;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.IdentifiableElement;
import org.corpus_tools.salt.graph.Relation;
import org.corpus_tools.salt.util.SaltUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the merging of higher document-structure, which means the
 * bottom-up traversal. stores all {@link SRelation} objects which have already
 * been traversed, this is necessary, to first detect cycles and second to not
 * traverse a super tree twice, e.g:
 * 
 * <pre>
 *           a
 *           |
 *           b
 *          / \
 *         c   d
 * </pre>
 * 
 * In this sample, the relation between a and b should be traversed only once.
 * With normal bottom-up it would be traversed twice, therefore we need to store
 * already traversed relations to avoid this.
 * 
 * @author Florian Zipser
 * @author Jakob Schmolling
 * 
 */
class MergeHandler implements GraphTraverseHandler {
	public static final Logger logger = LoggerFactory.getLogger(Merger.MODULE_NAME);
	/** graph whose nodes and relations are to copy **/
	private SDocumentGraph otherGraph = null;

	/** graph whose nodes and relations are to copy **/
	public SDocumentGraph getOtherGraph() {
		return otherGraph;
	}

	/** graph whose nodes and relations are to copy **/
	public void setOtherGraph(SDocumentGraph otherGraph) {
		this.otherGraph = otherGraph;
	}

	/** graph into which nodes and relations are copied **/
	private SDocumentGraph baseGraph = null;

	/** graph into which nodes and relations are copied **/
	public SDocumentGraph getBaseGraph() {
		return baseGraph;
	}

	/** graph into which nodes and relations are copied **/
	public void setBaseGraph(SDocumentGraph baseGraph) {
		this.baseGraph = baseGraph;
	}

	private MergerProperties properties = null;

	public MergerProperties getProperties() {
		return properties;
	}

	public void setProperties(MergerProperties properties) {
		this.properties = properties;
	}

	/**
	 * a map to relate nodes contained by otherGraph to nodes from baseGraph,
	 * which are mergable. Key is other node, value is base node.
	 **/
	private Map<SNode, SNode> node2NodeMap = null;
	/**
	 * current used {@link TokenMergeContainer} object, containing all mergable
	 * tokens
	 **/
	private TokenMergeContainer container = null;

	public MergeHandler(Map<SNode, SNode> node2NodeMap, SDocumentGraph otherGraph, SDocumentGraph baseGraph, TokenMergeContainer container) {
		this.node2NodeMap = node2NodeMap;
		setOtherGraph(otherGraph);
		setBaseGraph(baseGraph);
		this.container = container;
	}

	/**
	 * Copies all {@link SPointingRelation}s from <code>otherGraph</code> to
	 * <code>baseGraph</code> and even copies their annotations and layers.
	 * 
	 * @param otherGraph
	 *            graph containing the {@link SPointingRelation}s to be copied
	 * @param baseGraph
	 *            target graph
	 */
	public void mergeSPointingRelations(SDocumentGraph otherGraph, SDocumentGraph baseGraph) {
		for (SPointingRelation otherRel : otherGraph.getPointingRelations()) {
			SNode baseSourceNode = node2NodeMap.get(otherRel.getSource());
			SNode baseTargetNode = node2NodeMap.get(otherRel.getTarget());

			if (baseSourceNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + otherRel.getId() + "', because no matching node was found in target graph for source node '" + otherRel.getSource() + "'. ");
			} else if (baseTargetNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + otherRel.getId() + "', because no matching node was found in source graph for source node '" + otherRel.getTarget() + "'. ");
			} else {
				List<SRelation<SNode, SNode>> rels = baseGraph.getRelations(baseSourceNode.getId(), baseTargetNode.getId());
				boolean skip = false;
				if ((rels != null) && (rels.size() > 0)) {
					for (Relation rel : rels) {
						if (rel instanceof SPointingRelation) {
							// there is already a pointing relation between
							// nodes

							if (((SPointingRelation) rel).getType().equals(otherRel.getType())) {
								// skip relation when base graph already
								// contains an
								// equal relation
								skip = true;
								break;
							}
						}
					}
				}
				if (!skip) {
					SPointingRelation baseRel = SaltFactory.createSPointingRelation();
					baseRel.setSource((SStructuredNode) baseSourceNode);
					baseRel.setTarget((SStructuredNode) baseTargetNode);
					baseRel.setType(otherRel.getType());
					SaltUtil.moveAnnotations(otherRel, baseRel);
					SaltUtil.moveMetaAnnotations(otherRel, baseRel);
					baseGraph.addRelation(baseRel);
					copySLayers(otherRel, baseRel);
				}
			}
		}
	}

	/**
	 * set of already visited {@link SRelation}s while traversing, this is
	 * necessary to avoid cycles
	 **/
	private Set<SRelation> visitedRelations = new HashSet<>();

	/**
	 * Called by Pepper as callback, when otherGraph is traversed. Currently
	 * only returns <code>true</code> to traverse the entire graph.
	 */
	@Override
	public boolean checkConstraint(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SRelation sRelation, SNode currNode, long order) {
		boolean retVal = true;
		if (sRelation != null) {
			if (sRelation instanceof SPointingRelation) {
				// in case of relation is pointing relation, ignore it, it will
				// be processed later

				retVal = false;
			} else {
				if (visitedRelations.contains(sRelation)) {
					retVal = false;
				} else {
					visitedRelations.add(sRelation);
				}
			}
		}
		return (retVal);
	}

	/**
	 * Called by Pepper as callback, when otherGraph is traversed. Currently is
	 * empty.
	 */
	@Override
	public void nodeReached(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation sRelation, SNode otherNode, long order) {
	}

	/**
	 * Called by Pepper as callback, when otherGraph is traversed.
	 */
	@Override
	public void nodeLeft(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation relation, SNode otherNode, long order) {
		if (currNode instanceof SToken) {
			mergeNode(currNode, null, SALT_TYPE.STOKEN);
		} else if (currNode instanceof SSpan) {
			mergeNode(currNode, SALT_TYPE.SSPANNING_RELATION, SALT_TYPE.SSPAN);
		} else if (currNode instanceof SStructure) {
			mergeNode(currNode, SALT_TYPE.SDOMINANCE_RELATION, SALT_TYPE.SSTRUCTURE);
		} else if (currNode instanceof STextualDS) {
			// base text should be merged already
		} else {
			throw new PepperModuleException("Merging not implemented for this node type: " + currNode);
		}
	}

	/**
	 * 
	 * @param currNode
	 * @param sTypeRelations
	 * @param sTypeNode
	 * @return
	 */
	private void mergeNode(SNode currNode, SALT_TYPE sTypeRelations, SALT_TYPE sTypeNode) {
		SNode baseNode = null;

		// list of all equivalents to children of current node in base document
		List<SNode> childrens = getChildren(currNode, sTypeRelations);

		if ((currNode instanceof SToken) || (!getProperties().isCopyNodes())) {
			// do not copy all nodes, merge instead

			// list all parents in base document sharing the children
			List<SNode> sharedParents = new ArrayList<>();
			if (childrens.size() > 0) {
				sharedParents = getSharedParent(childrens, sTypeNode);
			}
			if (sharedParents.size() > 0) {
				// an equivalent to current node in base document was found

				baseNode = sharedParents.get(0);
			}
		}
		if (baseNode == null) {
			// no equivalent to currNode in base document was found

			switch (sTypeNode) {
			case STOKEN: {
				baseNode = node2NodeMap.get(currNode);
				if (baseNode != null) {
					// Match found
				} else {
					STextualRelation textRel = null;
					for (SRelation rel : currNode.getOutRelations()) {
						if (rel instanceof STextualRelation) {
							textRel = (STextualRelation) rel;
							break;
						}
					}
					// Find the alignment of the current token to create a new
					// one
					Integer start = container.getAlignedTokenStart((STextualDS) node2NodeMap.get(textRel.getTarget()), (SToken) currNode);
					Integer length = container.getAlignedTokenLength((STextualDS) node2NodeMap.get(textRel.getTarget()), (SToken) currNode);
					if ((start != -1) && (length != -1)) {
						baseNode = baseGraph.createToken((STextualDS) node2NodeMap.get(textRel.getTarget()), start, start + length);
					} else {
						logger.warn("[Merger] Could not create token in target graph matching to node '" + SaltUtil.getGlobalId(currNode.getIdentifier()) + "', because start (" + start + ") or length (" + length + ") was empty. ");
					}
				}
				break;
			}
			case SSPAN: {
				List<SToken> toSTokens = new ArrayList<>();
				for (SNode sNode : childrens) {
					toSTokens.add((SToken) sNode);
				}
				baseNode = baseGraph.createSpan(toSTokens);
				break;
			}
			case SSTRUCTURE: {
				List<SStructuredNode> baseStructureNodes = new ArrayList<>();
				for (SNode sNode : childrens) {
					baseStructureNodes.add((SStructuredNode) sNode);
				}
				baseNode = baseGraph.createStructure(baseStructureNodes);
				break;
			}
			default:
				break;
			}

			moveAnnosForRelations(currNode, baseNode);
		}
		if (baseNode != null) {
			node2NodeMap.put(currNode, baseNode);
			// copies all layers and add node baseNode to them
			copySLayers(currNode, baseNode);
			// moves annotations from currNode to baseNode
			SaltUtil.moveAnnotations(currNode, baseNode);
			SaltUtil.moveMetaAnnotations(currNode, baseNode);
		}
	}

	/**
	 * Copies the {@link SNode} or {@link SRelation} objects passed as
	 * <code>other</code> to all layers, the object passed as <code>base</code>
	 * is connected with. If no such layer exists in target graph, it will be
	 * created and all its annotations will be moved.
	 * 
	 * @param other
	 * @param base
	 */
	private static void copySLayers(IdentifiableElement other, IdentifiableElement base) {
		if ((other instanceof SRelation) && (base instanceof SRelation)) {
			SRelation otherRel = (SRelation) other;
			SRelation baseRel = (SRelation) base;
			if ((otherRel.getLayers() != null) && (otherRel.getLayers().size() != 0)) {
				SDocumentGraph baseGraph = ((SDocumentGraph) baseRel.getGraph());
				Iterator<SLayer> it = otherRel.getLayers().iterator();
				while (it.hasNext()) {
					SLayer otherLayer = it.next();
					List<SLayer> layers = baseGraph.getLayerByName(otherLayer.getName());
					SLayer baseLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						baseLayer = layers.get(0);
					}
					if (baseLayer == null) {
						baseLayer = SaltFactory.createSLayer();
						baseLayer.setName(otherLayer.getName());
						SaltUtil.moveAnnotations(otherLayer, baseLayer);
						SaltUtil.moveMetaAnnotations(otherLayer, baseLayer);
						baseGraph.addLayer(baseLayer);
					}
					baseRel.addLayer(baseLayer);
				}
			}
		} else if ((other instanceof SNode) && (base instanceof SNode)) {
			SNode otherNode = (SNode) other;
			SNode baseNode = (SNode) base;
			if ((otherNode.getLayers() != null) && (otherNode.getLayers().size() != 0)) {
				SDocumentGraph baseGraph = ((SDocumentGraph) baseNode.getGraph());

				for (SLayer otherLayer : otherNode.getLayers()) {
					List<SLayer> layers = baseGraph.getLayerByName(otherLayer.getName());
					SLayer baseLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						baseLayer = layers.get(0);
					}
					if (baseLayer == null) {
						baseLayer = SaltFactory.createSLayer();
						baseLayer.setName(otherLayer.getName());
						SaltUtil.moveAnnotations(otherLayer, baseLayer);
						SaltUtil.moveMetaAnnotations(otherLayer, baseLayer);
						baseGraph.addLayer(baseLayer);
					}
					baseNode.addLayer(baseLayer);
				}
			}
		}
	}

	/**
	 * Retrieves the {@link SRelation}s between given nodes and moves their
	 * {@link SAnnotation} and {@link SMetaAnnotation} objects. Further
	 * {@link SLayer}s will be copied and the stype (see
	 * {@link SRelation#getSTypes()}).
	 * 
	 * @param otherNode
	 * @param baseNode
	 */
	private void moveAnnosForRelations(SNode otherNode, SNode baseNode) {
		if ((otherNode != null) && (baseNode != null)) {
			for (SRelation otherRel : otherNode.getOutRelations()) {
				SNode baseChildNode = node2NodeMap.get(otherRel.getTarget());
				for (SRelation baseRel : baseNode.getOutRelations()) {
					if (baseRel.getTarget().equals(baseChildNode)) {
						SaltUtil.moveAnnotations(otherRel, baseRel);
						SaltUtil.moveMetaAnnotations(otherRel, baseRel);
						baseRel.setType(otherRel.getType());
						copySLayers(otherRel, baseRel);
						break;
					}
				}
			}
		}
	}

	/**
	 * Returns a list of nodes in base document. The returned nodes are
	 * equivalents to the direct children of the passed parent node. The
	 * children are retrieved via traversing of relations of the passed
	 * {@link SALT_TYPE}.
	 * 
	 * @param parent
	 *            node to who the children are retrieved
	 * @param sTypeRelation
	 *            type of relations to be traversed
	 * @return a list of children nodes
	 */
	private List<SNode> getChildren(SNode parent, SALT_TYPE sTypeRelation) {
		List<SNode> children = new ArrayList<>();
		List<SRelation> relations = parent.getOutRelations();
		if (relations != null) {
			for (SRelation<SNode, SNode> relation : relations) {
				if (SALT_TYPE.class2SaltType(relation.getClass()).contains(sTypeRelation)) {
					SNode otherNode = relation.getTarget();
					if (otherNode == null) {
						throw new PepperModuleException("Cannot merge data, because otherBase was null for relation '" + relation + "'. ");
					}
					SNode baseNode = node2NodeMap.get(otherNode);
					children.add(baseNode);
				}
			}
		}
		return children;
	}

	/**
	 * Returns a list of nodes that are the parents of every node in the given
	 * base list. Only relations with the given {@link SALT_TYPE} will be
	 * considered.
	 * 
	 * @param children
	 *            list of nodes whose parents are looked for
	 * @param sTypeNode
	 *            regarded types of relations
	 * @return a list of parents
	 */
	private List<SNode> getSharedParent(List<SNode> children, SALT_TYPE sTypeNode) {
		List<SNode> sharedParents = new ArrayList<>();
		if ((children.size() > 0) && (children.get(0) != null)) {
			List<SRelation> rels = children.get(0).getInRelations();
			if ((rels != null) && (rels.size() > 0)) {
				// A merge candidate has to be connected to every base node
				for (SRelation<SNode, SNode> baseRelation : rels) {
					sharedParents.add(baseRelation.getSource());
				}
				for (SNode baseNode : children) {
					List<SNode> parents = new ArrayList<>();
					for (SRelation<SNode, SNode> sRelation : baseNode.getInRelations()) {
						SNode parent = sRelation.getSource();
						if (SALT_TYPE.class2SaltType(parent.getClass()).contains(sTypeNode)) {
							parents.add(parent);
						}
					}
					sharedParents.retainAll(parents);
				}
			}
		}
		return sharedParents;
	}
}
