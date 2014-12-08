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
	private SDocumentGraph fromGraph = null;

	/** graph whose nodes and relations are to copy **/
	public SDocumentGraph getFromGraph() {
		return fromGraph;
	}

	/** graph whose nodes and relations are to copy **/
	public void setFromGraph(SDocumentGraph fromGraph) {
		this.fromGraph = fromGraph;
	}

	/** graph into which nodes and relations are copied **/
	private SDocumentGraph toGraph = null;

	/** graph into which nodes and relations are copied **/
	public SDocumentGraph getToGraph() {
		return toGraph;
	}

	/** graph into which nodes and relations are copied **/
	public void setToGraph(SDocumentGraph toGraph) {
		this.toGraph = toGraph;
	}

	/** anchor text of toGraph **/
	private STextualDS baseText = null;

	/** anchor text of toGraph **/
	public void setBaseText(STextualDS baseText) {
		this.baseText = baseText;
	}

	/**
	 * a map to relate nodes contained by fromGraph to nodes from toGraph, which
	 * are mergable
	 **/
	private Map<SNode, SNode> node2NodeMap = null;
	/**
	 * current used {@link TokenMergeContainer} object, containing all mergable
	 * tokens
	 **/
	private TokenMergeContainer container = null;

	public MergeHandler(Map<SNode, SNode> node2NodeMap, SDocumentGraph fromGraph, SDocumentGraph toGraph, STextualDS baseText, TokenMergeContainer container) {
		this.node2NodeMap = node2NodeMap;
		setBaseText(baseText);
		setFromGraph(fromGraph);
		setToGraph(toGraph);
		this.container = container;
		
		System.out.println("MERGING FORM "+ SaltFactory.eINSTANCE.getGlobalId(fromGraph.getSDocument().getSElementId())+"  TO "+SaltFactory.eINSTANCE.getGlobalId(toGraph.getSDocument().getSElementId()));
	}

	/**
	 * Copies all {@link SPointingRelation}s from <code>fromGraph</code> to
	 * <code>toGraph</code> and even copies their annotations and layers.
	 * 
	 * @param fromGraph
	 *            graph containing the {@link SPointingRelation}s to be copied
	 * @param toGraph
	 *            target graph
	 */
	public void mergeSPointingRelations(SDocumentGraph fromGraph, SDocumentGraph toGraph) {
		for (SPointingRelation fromRel : fromGraph.getSPointingRelations()) {
			SNode toSourceNode = node2NodeMap.get(fromRel.getSSource());
			SNode toTargetNode = node2NodeMap.get(fromRel.getSTarget());

			if (toSourceNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + fromRel.getSId() + "', because no matching node was found in target graph for source node '" + fromRel.getSSource() + "'. ");
			} else if (toTargetNode == null) {
				logger.warn("[Merger] Cannot merge SPointingRelation '" + fromRel.getSId() + "', because no matching node was found in source graph for source node '" + fromRel.getSTarget() + "'. ");
			} else {
				SPointingRelation toRel = SaltFactory.eINSTANCE.createSPointingRelation();
				toRel.setSSource(toSourceNode);
				toRel.setSTarget(toTargetNode);
				for (String type : fromRel.getSTypes()) {
					toRel.addSType(type);
				}
				SaltFactory.eINSTANCE.moveSAnnotations(fromRel, toRel);
				SaltFactory.eINSTANCE.moveSMetaAnnotations(fromRel, toRel);
				toGraph.addSRelation(toRel);
				copySLayers(fromRel, toRel);
			}
		}
	}

	/**
	 * set of already visited {@link SRelation}s while traversing, this is
	 * necessary to avoid cycles
	 **/
	private Set<SRelation> visitedRelations = new HashSet<SRelation>();

	/**
	 * Called by Pepper as callback, when fromGraph is traversed. Currently only
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
	 * Called by Pepper as callback, when fromGraph is traversed. Currently is
	 * empty.
	 */
	@Override
	public void nodeReached(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation sRelation, SNode fromNode, long order) {
	}

	/**
	 * Called by Pepper as callback, when fromGraph is traversed.
	 */
	@Override
	public void nodeLeft(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SNode currNode, SRelation edge, SNode fromNode, long order) {
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
		SNode toNode = null;
		List<SNode> childrens = getChildren(currNode, sTypeRelations);

		List<SNode> sharedParents = new ArrayList<SNode>();
		if (childrens.size() > 0) {
			sharedParents = getSharedParent(childrens, sTypeNode);
		}
		if (sharedParents.size() > 0) {
			// TODO: match found, check for annotations?
			toNode = sharedParents.get(0);
		} else {
			switch (sTypeNode) {
			case STOKEN: {
				toNode = node2NodeMap.get(currNode);
				if (toNode != null) {
					// Match found
				} else {
					// Find the alignment of the current token to create a new
					// one
					Integer sStart = container.getAlignedTokenStart(baseText, (SToken) currNode);
					Integer sLength = container.getAlignedTokenLength(baseText, (SToken) currNode);
					if ((sStart != -1) && (sLength != -1)) {
						toNode = toGraph.createSToken(baseText, sStart, sStart + sLength);
					} else {
						logger.warn("Could not create token in target graph matching to node '" + SaltFactory.eINSTANCE.getGlobalId(currNode.getSElementId()) + "', because sStart-value (" + sStart + ") or sLength-value (" + sLength + ") was empty. ");
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
				toNode = toGraph.createSSpan(toSTokens);
				break;
			}
			case SSTRUCTURE: {
				// TODO: better way to cast
				EList<SStructuredNode> baseStructureNodes = new BasicEList<SStructuredNode>();
				for (SNode sNode : childrens) {
					baseStructureNodes.add((SStructuredNode) sNode);
				}
				toNode = toGraph.createSStructure(baseStructureNodes);
				break;
			}
			default:
				break;
			}

			moveAnnosForRelations(currNode, toNode);
		}
		if (toNode != null) {
			node2NodeMap.put(currNode, toNode);
			// copies all layers and add node toNode to them
			copySLayers(currNode, toNode);
			// moves annotations from currNode to toNode
			SaltFactory.eINSTANCE.moveSAnnotations(currNode, toNode);
			SaltFactory.eINSTANCE.moveSMetaAnnotations(currNode, toNode);
		}
	}

	/**
	 * Copies the {@link SNode} or {@link SRelation} objects passed as
	 * <code>from</code> to all layers, the object passed as <code>to</code> is
	 * connected with. If no such layer exists in target graph, it will be
	 * created and all its annotations will be moved.
	 * 
	 * @param from
	 * @param to
	 */
	private static void copySLayers(SIdentifiableElement from, SIdentifiableElement to) {
		if ((from instanceof SRelation) && (to instanceof SRelation)) {
			SRelation fromRel = (SRelation) from;
			SRelation toRel = (SRelation) to;
			if ((fromRel.getSLayers() != null) && (fromRel.getSLayers().size() != 0)) {
				SDocumentGraph toGraph = ((SDocumentGraph) toRel.getSGraph());
				for (SLayer fromLayer : fromRel.getSLayers()) {
					List<SLayer> layers = toGraph.getSLayerByName(fromLayer.getSName());
					SLayer toLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						toLayer = layers.get(0);
					}
					if (toLayer == null) {
						toLayer = SaltFactory.eINSTANCE.createSLayer();
						toLayer.setSName(fromLayer.getSName());
						SaltFactory.eINSTANCE.moveSAnnotations(fromLayer, toLayer);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(fromLayer, toLayer);
						toGraph.addSLayer(toLayer);
					}
					toRel.getSLayers().add(toLayer);
				}
			}
		} else if ((from instanceof SNode) && (to instanceof SNode)) {
			SNode fromNode = (SNode) from;
			SNode toNode = (SNode) to;
			if ((fromNode.getSLayers() != null) && (fromNode.getSLayers().size() != 0)) {
				SDocumentGraph toGraph = ((SDocumentGraph) toNode.getSGraph());

				for (SLayer fromLayer : fromNode.getSLayers()) {
					List<SLayer> layers = toGraph.getSLayerByName(fromLayer.getSName());
					SLayer toLayer = null;
					if ((layers != null) && (!layers.isEmpty())) {
						toLayer = layers.get(0);
					}
					if (toLayer == null) {
						toLayer = SaltFactory.eINSTANCE.createSLayer();
						toLayer.setSName(fromLayer.getSName());
						SaltFactory.eINSTANCE.moveSAnnotations(fromLayer, toLayer);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(fromLayer, toLayer);
						toGraph.addSLayer(toLayer);
					}
					toNode.getSLayers().add(toLayer);
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
	 * @param fromNode
	 * @param toNode
	 */
	private void moveAnnosForRelations(SNode fromNode, SNode toNode) {
		if ((fromNode != null) && (toNode != null)) {
			for (SRelation fromRel : fromNode.getOutgoingSRelations()) {
				SNode toChildNode = node2NodeMap.get(fromRel.getSTarget());
				for (SRelation toRel : toNode.getOutgoingSRelations()) {
					if (toRel.getSTarget().equals(toChildNode)) {
						SaltFactory.eINSTANCE.moveSAnnotations(fromRel, toRel);
						SaltFactory.eINSTANCE.moveSMetaAnnotations(fromRel, toRel);
						List<String> sTypes = fromRel.getSTypes();
						if (sTypes != null) {
							for (String type : sTypes) {
								toRel.addSType(type);
							}
						}
						copySLayers(fromRel, toRel);
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
		EList<SNode> base = new BasicEList<SNode>();
		EList<SRelation> relations = parent.getOutgoingSRelations();
		if (relations != null) {
			for (SRelation relation : relations) {
				if (SaltFactory.eINSTANCE.convertClazzToSTypeName(relation.getClass()).contains(sTypeRelation)) {
					SNode fromBase = relation.getSTarget();
					if (fromBase == null) {
						throw new PepperModuleException("Cannot merge data, because fromBase was null for relation '" + relation + "'. ");
					}
					SNode toBase = node2NodeMap.get(fromBase);
					base.add(toBase);
				}
			}
		}
		return base;
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