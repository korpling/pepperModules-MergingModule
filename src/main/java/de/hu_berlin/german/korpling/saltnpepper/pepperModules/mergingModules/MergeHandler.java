package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.GRAPH_TRAVERSE_TYPE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STYPE_NAME;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraphTraverseHandler;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;

/**
 * This class handles the merging of higher document-structure, which means the bottom-up traversal.
 * @author florian
 *
 */
class MergeHandler implements SGraphTraverseHandler {
	/**
	 * stores all {@link SRelation} objects which have already been
	 * traversed, this is necessary, to first detect cycles and second to
	 * not traverse a super tree twice, e.g:
	 * 
	 * <pre>
	 *           a
	 *           |
	 *           b
	 *          / \
	 *         c   d
	 * </pre>
	 * 
	 * In this sample, the relation between a and b should be traversed only
	 * once. With normal bottom-up it would be traversed twice, therefore we
	 * need to store already traversed relations to avoid this.
	 **/
	private SDocumentGraph fromGraph = null;
	private SDocumentGraph toGraph = null;
	private STextualDS baseText = null;
	private Map<SNode, SNode> node2NodeMap = null;
	private TokenMergeContainer container = null;
	
	public MergeHandler(Map<SNode, SNode> node2NodeMap, SDocumentGraph fromGraph, SDocumentGraph toGraph, STextualDS baseText, TokenMergeContainer container) {
		this.node2NodeMap = node2NodeMap;
		this.baseText = baseText;
		this.fromGraph = fromGraph;
		this.toGraph = toGraph;
		this.container = container;
	}

	public void setBaseText(STextualDS baseText) {
		this.baseText = baseText;

	}

	public SDocumentGraph getFromGraph() {
		return fromGraph;
	}

	public void setFromGraph(SDocumentGraph fromGraph) {
		this.fromGraph = fromGraph;
	}

	public SDocumentGraph getToGraph() {
		return toGraph;
	}

	public void setToGraph(SDocumentGraph toGraph) {
		this.toGraph = toGraph;
	}

	@Override
	public void nodeReached(GRAPH_TRAVERSE_TYPE traversalType,
			String traversalId, SNode currNode, SRelation sRelation,
			SNode fromNode, long order) {

	}

	@Override
	public void nodeLeft(GRAPH_TRAVERSE_TYPE traversalType,
			String traversalId, SNode currNode, SRelation edge,
			SNode fromNode, long order) {
		SNode otherNode = null;

		if (currNode instanceof SToken) {
			otherNode = mergeNode(currNode, null, STYPE_NAME.STOKEN);
		} else if (currNode instanceof SSpan) {
			otherNode = mergeNode(currNode, STYPE_NAME.SSPANNING_RELATION,
					STYPE_NAME.SSPAN);
		} else if (currNode instanceof SStructure) {
			otherNode = mergeNode(currNode, STYPE_NAME.SDOMINANCE_RELATION,
					STYPE_NAME.SSTRUCTURE);
		}

		if (otherNode != null) {
			SaltFactory.eINSTANCE.moveSAnnotations(currNode, otherNode);
			SaltFactory.eINSTANCE.moveSMetaAnnotations(currNode, otherNode);
		}
	}

	private SNode mergeNode(SNode currNode, STYPE_NAME sTypeRelations,
			STYPE_NAME sTypeNode) {
		SNode toNode = null;
		EList<SNode> childrens = getChildren(currNode, sTypeRelations);

		List<SNode> sharedParents = new ArrayList<SNode>();
		if (childrens.size() > 0) {
			sharedParents = getSharedParent(childrens, sTypeNode);			
		}
		if (sharedParents.size() > 0) {
			// TODO: match found, check for annotations?
			toNode = sharedParents.get(0);
		} else {
			toNode = buildNode(currNode, sTypeNode, childrens, toNode);
		}
		node2NodeMap.put(currNode, toNode);

		assert toNode != null;
		return toNode;
	}

	/**
	 * creates a new Node in the target graph
	 * 
	 * @param currNode
	 * @param sTypeNode
	 * @param base
	 * @param toNode
	 * @return
	 */
	private SNode buildNode(SNode currNode, STYPE_NAME sTypeNode,
			EList<SNode> base, SNode toNode) {
		switch (sTypeNode) {
		case STOKEN: {
			toNode = node2NodeMap.get(currNode);
			if (toNode != null) {
				// Match found
			} else {
				// Find the alignment of the current token to create
				// a new one
				int sStart = container.getAlignedTokenStart(baseText,
						(SToken) currNode);
				int sLength = container.getAlignedTokenLength(baseText,
						(SToken) currNode);
				toNode = toGraph.createSToken(baseText, sStart, sStart
						+ sLength);
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
	 * Get all direct children of SNode of the given type SType
	 * 
	 * @param parent
	 * @param sTypeRelation
	 * @return
	 */
	private EList<SNode> getChildren(SNode parent, STYPE_NAME sTypeRelation) {
		EList<SNode> base = new BasicEList<SNode>();
		EList<SRelation> relations = parent.getOutgoingSRelations();
		if (relations != null) {
			for (SRelation relation : relations) {
				if (SaltFactory.eINSTANCE.convertClazzToSTypeName(
						relation.getClass()).contains(sTypeRelation)) {
					SNode fromBase = relation.getSTarget();
					SNode toBase = node2NodeMap.get(fromBase);
					base.add(toBase);
				}
			}
		}
		return base;
	}

	/**
	 * Get a list of Nodes that are the parent of every node in the given
	 * base list. Only relations with the given SType will be considered.
	 * 
	 * @param base
	 * @param sTypeNode
	 * @return
	 */
	private List<SNode> getSharedParent(EList<SNode> base,
			STYPE_NAME sTypeNode) {
		ArrayList<SNode> sharedParents = new ArrayList<SNode>();
		assert base.size() > 0;
		// A merge candidate has to be connected to every base node
		for (SRelation baseRelation : base.get(0).getIncomingSRelations()) {
			sharedParents.add(baseRelation.getSSource());
		}
		for (SNode baseNode : base) {
			ArrayList<SNode> parents = new ArrayList<SNode>();
			for (SRelation sRelation : baseNode.getIncomingSRelations()) {
				SNode parent = sRelation.getSSource();
				if (SaltFactory.eINSTANCE.convertClazzToSTypeName(
						parent.getClass()).contains(sTypeNode)) {
					parents.add(parent);
				}
			}
			sharedParents.retainAll(parents);
		}
		return sharedParents;
	}

	@Override
	public boolean checkConstraint(GRAPH_TRAVERSE_TYPE traversalType,
			String traversalId, SRelation edge, SNode currNode, long order) {
//		if (edge != null) {
//			if (traversedRelations.contains(edge)) {
//				return (false);
//			} else {
//				traversedRelations.add(edge);
//			}
//		}
		return true;
	}
}