package org.opennms.gsoc.nodes;

import java.util.ArrayList;

import org.opennms.gsoc.nodes.model.OnmsNode;


public interface NodesServerCommunication {
	ArrayList<OnmsNode> getNodes(String url);
}
