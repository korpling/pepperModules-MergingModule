package org.corpus_tools.peppermodules.mergingModules.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.util.SaltUtil;

/**
 * similar to guavas multimap, but can contain values twice (this is because,
 * equal method of two {@link SDocument}s having the same path but belong to
 * different {@link SCorpusGraph}s are the same for equals(), but shouldn't be.)
 **/
public class Multimap {
	private Map<String, List<SNode>> map = null;

	public Multimap() {
		map = new LinkedHashMap<>();
	}

	public void put(String sId, SNode sNode) {
		List<SNode> slot = map.get(sId);
		if (slot == null) {
			slot = new ArrayList<>();
			map.put(sId, slot);
		}
		slot.add(sNode);
	}

	public List<SNode> get(String sId) {
		return (map.get(sId));
	}

	@Override
	public String toString() {
		StringBuilder retVal = new StringBuilder();
		for (String key : map.keySet()) {
			retVal.append(key);
			retVal.append("=");
			List<SNode> sNodes = map.get(key);
			if (sNodes != null) {
				int i = 0;
				for (SNode sNode : sNodes) {
					if (i != 0) {
						retVal.append(", ");
					}
					retVal.append(SaltUtil.getGlobalId(sNode.getIdentifier()));
					i++;
				}
			}
			retVal.append("; ");
		}
		return (retVal.toString());
	}

	public Set<String> keySet() {
		return (map.keySet());
	}
}
