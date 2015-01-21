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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.GRAPH_TRAVERSE_TYPE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STYPE_NAME;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraphTraverseHandler;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SIdentifiableElement;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;

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
class MergeHandler implements SGraphTraverseHandler {
	public static final Logger logger = LoggerFactory.getLogger(MergeHandler.class);
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

	/**
	 * a map to relate nodes contained by otherGraph to nodes from baseGraph, which
	 * are mergable. Key is other node, value is base node.
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
		for (SPointingRelation otherRel : otherGraph.getSPointingRelations()) {
			SNode baseSourceNode = node2NodeMap.get(otherRel.getSSource());
			SNode baseTargetNode = node2NodeMap.get(otherRel.getSTarget());

			if (baseSourceNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + otherRel.getSId() + "', because no matching node was found in target graph for source node '" + otherRel.getSSource() + "'. ");
			} else if (baseTargetNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + otherRel.getSId() + "', because no matching node was found in source graph for source node '" + otherRel.getSTarget() + "'. ");
			} else {
				SPointingRelation baseRel = SaltFactory.eINSTANCE.createSPointingRelation();
				baseRel.setSSource(baseSourceNode);
				baseRel.setSTarget(baseTargetNode);
				for (String type : otherRel.getSTypes()) {
					baseRel.addSType(type);
				}
				SaltFactory.eINSTANCE.moveSAnnotations(otherRel, baseRel);
				SaltFactory.eINSTANCE.moveSMetaAnnotations(otherRel, baseRel);
				baseGraph.addSRelation(baseRel);
				copySLayers(otherRel, baseRel);
			}
		}
	}

	/**
	 * set of already visited {@link SRelation}s while traversing, this is
	 * necessary to avoid cycles
	 **/
	private Set<SRelation> visitedRelations = new HashSet<SRelation>();

	/**
	 * Called by Pepper as callback, when otherGraph is traversed. Currently only
	 * returns <code>true</code> to traverse the entire graph.
	 */
	@Override
	public boolean checkConstraint(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SRelation sRelation, SNode currNode, long order) {
		if (sRelation != null) {
			if (visitedRelations.contains(sRelation)) {
				return (false);
			} else {
				visitedRelations.add(sRelation);
			}
		}

		return true;
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
	public void nodeLeft(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation edge, SNode otherNode, long order) {
		if (currNode instanceof SToken) {
			mergeNode(currNode, null, STYPE_NAME.STOKEN);
		} else if (currNode instanceof SSpan) {
			mergeNode(currNode, STYPE_NAME.SSPANNING_RELATION, STYPE_NAME.SSPAN);
		} else if (currNode instanceof SStructure) {
			mergeNode(currNode, STYPE_NAME.SDOMINANCE_RELATION, STYPE_NAME.SSTRUCTURE);
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
	private void mergeNode(SNode currNode, STYPE_NAME sTypeRelations, STYPE_NAME sTypeNode) {
		SNode baseNode = null;
		List<SNode> childrens = getChildren(currNode, sTypeRelations);

		List<SNode> sharedParents = new ArrayList<SNode>();
		if (childrens.size() > 0) {
			sharedParents = getSharedParent(childrens, sTypeNode);
		}
		if (sharedParents.size() > 0) {
			// TODO: match found, check for annotations?
			baseNode = sharedParents.get(0);
		} else {
			switch (sTypeNode) {
			case STOKEN: {
				baseNode = node2NodeMap.get(currNode);
				if (baseNode != null) {
					// Match found
				} else {
					STextualRelation textRel= null;
					for (SRelation rel: currNode.getOutgoingSRelations()){
						if (rel instanceof STextualRelation){
							textRel= (STextualRelation)rel;
							break;
						}
					}
					// Find the alignment of the current token to create a new
					// one
					Integer sStart = container.getAlignedTokenStart((STextualDS)node2NodeMap.get(textRel.getSTextualDS()), (SToken) currNode);
					Integer sLength = container.getAlignedTokenLength((STextualDS)node2NodeMap.get(textRel.getSTextualDS()), (SToken) currNode);
					if ((sStart != -1) && (sLength != -1)) {
						baseNode = baseGraph.createSToken((STextualDS)node2NodeMap.get(textRel.getSTextualDS()), sStart, sStart + sLength);
					} else {
						logger.warn("[Merger] Could not create token in target graph matching to node '" + SaltFactory.eINSTANCE.getGlobalId(currNode.getSElementId()) + "', because sStart-value (" + sStart + ") or sLength-value (" + sLength + ") was empty. ");
					}
				}
				break;
			}
			case SSPAN: {
				// TODO: better way to cast
				EList<SToken> toSTokens = new BasicEList<SToken>();
				for (SNode sNode : childrens) {
					toSTokens.add((SToken) sNode);
				}
				baseNode = baseGraph.createSSpan(toSTokens);
				break;
			}
			case SSTRUCTURE: {
				// TODO: better way to cast
				EList<SStructuredNode> baseStructureNodes = new BasicEList<SStructuredNode>();
				for (SNode sNode : childrens) {
					baseStructureNodes.add((SStructuredNode) sNode);
				}
				baseNode = baseGraph.createSStructure(baseStructureNodes);
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
			SaltFactory.eINSTANCE.moveSAnnotations(currNode, baseNode);
			SaltFactory.eINSTANCE.moveSMetaAnnotations(currNode, baseNode);
		}
	}

	/**
	 * Copies the {@link SNode} or {@link SRelation} objects passed as
	 * <code>other</code> to all layers, the object passed as <code>base</code> is
	 * connected with. If no such layer exists in target graph, it will be
	 * created and all its annotations will be moved.
	 * 
	 * @param other
	 * @param base
	 */
	private static void copySLayers(SIdentifiableElement other, SIdentifiableElement base) {
		if ((other instanceof SRelation) && (base instanceof SRelation)) {
			SRelation otherRel = (SRelation) other;
			SRelation baseRel = (SRelation) base;
			if ((otherRel.getSLayers() != null) && (otherRel.getSLayers().size() != 0)) {
				SDocumentGraph baseGraph = ((SDocumentGraph) baseRel.getSGraph());
				for (SLayer otherLayer : otherRel.getSLayers()) {
					List<SLayer> layers = baseGraph.getSLayerByName(otherLayer.getSName());
					SLayer baseLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						baseLayer = layers.get(0);
					}
					if (baseLayer == null) {
						baseLayer = SaltFactory.eINSTANCE.createSLayer();
						baseLayer.setSName(otherLayer.getSName());
						SaltFactory.eINSTANCE.moveSAnnotations(otherLayer, baseLayer);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(otherLayer, baseLayer);
						baseGraph.addSLayer(baseLayer);
					}
					baseRel.getSLayers().add(baseLayer);
				}
			}
		} else if ((other instanceof SNode) && (base instanceof SNode)) {
			SNode otherNode = (SNode) other;
			SNode baseNode = (SNode) base;
			if ((otherNode.getSLayers() != null) && (otherNode.getSLayers().size() != 0)) {
				SDocumentGraph baseGraph = ((SDocumentGraph) baseNode.getSGraph());

				for (SLayer otherLayer : otherNode.getSLayers()) {
					List<SLayer> layers = baseGraph.getSLayerByName(otherLayer.getSName());
					SLayer baseLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						baseLayer = layers.get(0);
					}
					if (baseLayer == null) {
						baseLayer = SaltFactory.eINSTANCE.createSLayer();
						baseLayer.setSName(otherLayer.getSName());
						SaltFactory.eINSTANCE.moveSAnnotations(otherLayer, baseLayer);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(otherLayer, baseLayer);
						baseGraph.addSLayer(baseLayer);
					}
					baseNode.getSLayers().add(baseLayer);
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
			for (SRelation otherRel : otherNode.getOutgoingSRelations()) {
				SNode baseChildNode = node2NodeMap.get(otherRel.getSTarget());
				for (SRelation baseRel : baseNode.getOutgoingSRelations()) {
					if (baseRel.getSTarget().equals(baseChildNode)) {
						SaltFactory.eINSTANCE.moveSAnnotations(otherRel, baseRel);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(otherRel, baseRel);
						List<String> sTypes = otherRel.getSTypes();
						if (sTypes != null) {
							for (String type : sTypes) {
								baseRel.addSType(type);
							}
						}
						copySLayers(otherRel, baseRel);
						break;
					}
				}
			}
		}
	}

	/**
	 * Returns a list of all direct children of the passed {@link SNode}. The
	 * children are retrieved via traversing of relations of the passed
	 * {@link STYPE_NAME}.
	 * 
	 * @param parent
	 *            node to who the children are retrieved
	 * @param sTypeRelation
	 *            type of relations to be traversed
	 * @return a list of children nodes
	 */
	private List<SNode> getChildren(SNode parent, STYPE_NAME sTypeRelation) {
		EList<SNode> baseList = new BasicEList<SNode>();
		EList<SRelation> relations = parent.getOutgoingSRelations();
		if (relations != null) {
			for (SRelation relation : relations) {
				if (SaltFactory.eINSTANCE.convertClazzToSTypeName(relation.getClass()).contains(sTypeRelation)) {
					SNode otherNode = relation.getSTarget();
					if (otherNode == null) {
						throw new PepperModuleException("Cannot merge data, because otherBase was null for relation '" + relation + "'. ");
					}
					SNode baseNode = node2NodeMap.get(otherNode);
					baseList.add(baseNode);
				}
			}
		}
		return baseList;
	}

	/**
	 * Returns a list of nodes that are the parents of every node in the given
	 * base list. Only relations with the given {@link STYPE_NAME} will be
	 * considered.
	 * 
	 * @param children
	 *            list of nodes whose parents are looked for
	 * @param sTypeNode
	 *            regarded types of relations
	 * @return a list of parents
	 */
	private List<SNode> getSharedParent(List<SNode> children, STYPE_NAME sTypeNode) {
		ArrayList<SNode> sharedParents = new ArrayList<SNode>();
		if ((children.size() > 0) && (children.get(0) != null)) {
			List<SRelation> rels = children.get(0).getIncomingSRelations();
			if ((rels != null) && (rels.size() > 0)) {
				// A merge candidate has to be connected to every base node
				for (SRelation baseRelation : rels) {
					sharedParents.add(baseRelation.getSSource());
				}
				for (SNode baseNode : children) {
					ArrayList<SNode> parents = new ArrayList<SNode>();
					for (SRelation sRelation : baseNode.getIncomingSRelations()) {
						SNode parent = sRelation.getSSource();
						if (SaltFactory.eINSTANCE.convertClazzToSTypeName(parent.getClass()).contains(sTypeNode)) {
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
