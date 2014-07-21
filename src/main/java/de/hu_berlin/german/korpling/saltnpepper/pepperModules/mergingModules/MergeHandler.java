package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.GRAPH_TRAVERSE_TYPE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STYPE_NAME;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraphTraverseHandler;
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
 * @author florian
 * 
 */
class MergeHandler implements SGraphTraverseHandler {
	public static final Logger logger= LoggerFactory.getLogger(MergeHandler.class);
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
		SNode otherNode = null;

		if (currNode instanceof SToken) {
			otherNode = mergeNode(currNode, null, STYPE_NAME.STOKEN);
		} else if (currNode instanceof SSpan) {
			otherNode = mergeNode(currNode, STYPE_NAME.SSPANNING_RELATION, STYPE_NAME.SSPAN);
		} else if (currNode instanceof SStructure) {
			otherNode = mergeNode(currNode, STYPE_NAME.SDOMINANCE_RELATION, STYPE_NAME.SSTRUCTURE);
		} else if (currNode instanceof STextualDS) {
			// base text should be merged already
		} else {
			throw new PepperModuleException("Merging not implementet for this node type: " + currNode);
		}

		if (otherNode != null) {
			SaltFactory.eINSTANCE.moveSAnnotations(currNode, otherNode);
			SaltFactory.eINSTANCE.moveSMetaAnnotations(currNode, otherNode);
		}
	}

	/**
	 * 
	 * @param currNode
	 * @param sTypeRelations
	 * @param sTypeNode
	 * @return
	 */
	private SNode mergeNode(SNode currNode, STYPE_NAME sTypeRelations, STYPE_NAME sTypeNode) {
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
			toNode = buildNode(currNode, sTypeNode, childrens);
			moveAnnosForRelations(currNode, toNode);
		}
		node2NodeMap.put(currNode, toNode);

		if (toNode == null){
			throw new PepperModuleException("Cannot go on merging, because the toNode was null. ");
		}
		return toNode;
	}

	/**
	 * Retrieves the {@link SRelation}s between given nodes and moves their {@link SAnnotation} and {@link SMetaAnnotation} objects.
	 * @param fromNode
	 * @param toNode
	 */
	private void moveAnnosForRelations(SNode fromNode, SNode toNode){
		for (SRelation fromSRel: fromNode.getOutgoingSRelations()){
			SNode toChildNode= node2NodeMap.get(fromSRel.getSTarget());
			for (SRelation toSRel: toNode.getOutgoingSRelations()){
				if (toSRel.getSTarget().equals(toChildNode)){
					SaltFactory.eINSTANCE.moveSAnnotations(fromSRel, toSRel);
					SaltFactory.eINSTANCE.moveSMetaAnnotations(fromSRel, toSRel);
					break;
				}
			}
			
			
			
//			System.out.println();
//			List<Edge> edges= toChildNode.getGraph().getEdges(toNode.getSId(), toChildNode.getSId());
//			if (	(edges== null){
//				logger.warn("Cannot find a sRelation matching to SRelation '"+fromSRel.getSId()+"' in toGraph.");
//			}else{
//				SRelation toSRel= (SRelation)edge;
//				SaltFactory.eINSTANCE.moveSAnnotations(fromSRel, toSRel);
//				SaltFactory.eINSTANCE.moveSMetaAnnotations(fromSRel, toSRel);
//			}
		}
	}
	
	/**
	 * Creates a new node in the target graph, which is a copy of the passed <code>currNode</code>.
	 * @param currNode
	 * @param sTypeNode
	 * @param base
	 * @param toNode
	 * @return
	 */
	private SNode buildNode(SNode currNode, STYPE_NAME sTypeNode, List<SNode> base) {
		SNode toNode= null;
		switch (sTypeNode) {
		case STOKEN: {
			toNode = node2NodeMap.get(currNode);
			if (toNode != null) {
				// Match found
			} else {
				// Find the alignment of the current token to create
				// a new one
				int sStart = container.getAlignedTokenStart(baseText, (SToken) currNode);
				int sLength = container.getAlignedTokenLength(baseText, (SToken) currNode);
				toNode = toGraph.createSToken(baseText, sStart, sStart + sLength);
			}
		}
			break;
		case SSPAN: {
			// TODO: better way to cast
			EList<SToken> baseSSpanNodes = new BasicEList<SToken>();
			for (SNode sNode : base) {
				baseSSpanNodes.add((SToken) sNode);
			}
			toNode = toGraph.createSSpan(baseSSpanNodes);
		}
			break;
		case SSTRUCTURE: {
			// TODO: better way to cast
			EList<SStructuredNode> baseStructureNodes = new BasicEList<SStructuredNode>();
			for (SNode sNode : base) {
				baseStructureNodes.add((SStructuredNode) sNode);
			}
			toNode = toGraph.createSStructure(baseStructureNodes);
		}
			break;

		default:
			break;
		}
		return toNode;
	}

	/**
	 * Called by Pepper as callback, when fromGraph is traversed. Currently only
	 * returns <code>true</code> to traverse the entire graph.
	 */
	@Override
	public boolean checkConstraint(GRAPH_TRAVERSE_TYPE traversalType, String traversalId, SRelation edge, SNode currNode, long order) {
		return true;
	}
	
	/**
	 * Returns a list of all direct children of the passed {@link SNode}. The children are retrieved via traversing
	 * of relations of the passed {@link STYPE_NAME}.
	 * @param parent node to who the children are retrieved
	 * @param sTypeRelation type of relations to be traversed
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
	 * Returns a list of nodes that are the parents of every node in the given base
	 * list. Only relations with the given {@link STYPE_NAME} will be considered.
	 * @param children list of nodes whose parents are looked for
	 * @param sTypeNode regarded types of relations
	 * @return a list of parents
	 */
	private List<SNode> getSharedParent(List<SNode> children, STYPE_NAME sTypeNode) {
		ArrayList<SNode> sharedParents = new ArrayList<SNode>();
		if (children.size() > 0){
			// A merge candidate has to be connected to every base node
			for (SRelation baseRelation : children.get(0).getIncomingSRelations()) {
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
		return sharedParents;
	}
}