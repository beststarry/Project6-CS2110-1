package student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.naming.LinkLoopException;

import a4.Heap;

//import com.sun.tools.javac.code.Scope.StarImportScope;

import a5.GraphAlgorithms;
import game.Edge;
import game.FindState;
import game.FleeState;
import game.GameState;
import game.NodeStatus;
import game.SewerDiver;
import game.Sewers;
import game.Tile;
import game.Node;

import common.NotImplementedError;

public class DiverMin implements SewerDiver {
	Set<Long>   visited  = new HashSet<Long>();
	/** Get to the ring in as few steps as possible. Once you get there, <br>
	 * you must return from this function in order to pick<br>
	 * it up. If you continue to move after finding the ring rather <br>
	 * than returning, it will not count.<br>
	 * If you return from this function while not standing on top of the ring, <br>
	 * it will count as a failure.
	 *
	 * There is no limit to how many steps you can take, but you will receive<br>
	 * a score bonus multiplier for finding the ring in fewer steps.
	 *
	 * At every step, you know only your current tile's ID and the ID of all<br>
	 * open neighbor tiles, as well as the distance to the ring at each of <br>
	 * these tiles (ignoring walls and obstacles).
	 *
	 * In order to get information about the current state, use functions<br>
	 * currentLocation(), neighbors(), and distanceToRing() in state.<br>
	 * You know you are standing on the ring when distanceToRing() is 0.
	 *
	 * Use function moveTo(long id) in state to move to a neighboring<br>
	 * tile by its ID. Doing this will change state to reflect your new position.
	 *
	 * A suggested first implementation that will always find the ring, but <br>
	 * likely won't receive a large bonus multiplier, is a depth-first walk. <br>
	 * Some modification is necessary to make the search better, in general. */
	@Override
	public void find(FindState state) {
		Heap<Long, Integer> worklist= new Heap<Long, Integer>(new NodeComparator());
		//Stack<Long> worklist = new Stack<Long>();
		System.out.println(state.currentLocation() + "start");
		
		List<Long>  result   = new ArrayList<Long>();
		AllWentNodes gameMap =new AllWentNodes();
		visited.add(state.currentLocation());
		gameMap.addNode(new FindNode(state.currentLocation()), null);
		for (NodeStatus neighbor : state.neighbors()) {
			if (!visited.contains(neighbor.getId())) {
				worklist.add(neighbor.getId(),state.distanceToRing());
				System.out.println(neighbor.getId() + "worklist");
				gameMap.addNode(new FindNode(neighbor.getId()), state.currentLocation());
			}
		}
		
		while (state.distanceToRing()!=0) {
			Long next = worklist.poll();
			System.out.println(next + "next");
			//state.moveTo(next);
			moveToHelper(next, gameMap, (GameState) state);
			visited.add(next);
			
			result.add(next);
			for (NodeStatus neighbor : state.neighbors()) {
				System.out.println(neighbor.getId() + "neighbours");
				if (!visited.contains(neighbor.getId())) {
					if(!worklist.contain(neighbor.getId())) {
						worklist.add(neighbor.getId(),neighbor.getDistanceToTarget());
					}else {
						worklist.changePriority(neighbor.getId(),neighbor.getDistanceToTarget());
					}
					gameMap.addNode(new FindNode(neighbor.getId()), next);
				}
			}
		}
	}
	
	/** Flee the sewer system before the steps are all used, trying to <br>
	 * collect as many coins as possible along the way. Your solution must ALWAYS <br>
	 * get out before the steps are all used, and this should be prioritized above<br>
	 * collecting coins.
	 *
	 * You now have access to the entire underlying graph, which can be accessed<br>
	 * through FleeState. currentNode() and getExit() will return Node objects<br>
	 * of interest, and getNodes() will return a collection of all nodes on the graph.
	 *
	 * You have to get out of the sewer system in the number of steps given by<br>
	 * getStepsRemaining(); for each move along an edge, this number is <br>
	 * decremented by the weight of the edge taken.
	 *
	 * Use moveTo(n) to move to a node n that is adjacent to the current node.<br>
	 * When n is moved-to, coins on node n are automatically picked up.
	 *
	 * You must return from this function while standing at the exit. Failing <br>
	 * to do so before steps run out or returning from the wrong node will be<br>
	 * considered a failed run.
	 *
	 * Initially, there are enough steps to get from the starting point to the<br>
	 * exit using the shortest path, although this will not collect many coins.<br>
	 * For this reason, a good starting solution is to use the shortest path to<br>
	 * the exit. */
	@Override
	public void flee(FleeState state) {
		greedyFlee(state);
	}
	
	/**
	 * Moves to neighbor with the most coins unless only enough walks to get to exit or 
	 * all neighbors have zero coins. If no neighbors have gems, it finds a feasible path.
	 * */
	public void greedyFlee(FleeState state) {
		while(state.currentNode()!=state.getExit()) {
			Comparator<Integer> cmp = Comparator.naturalOrder();
			Heap<Node, Integer> coinHeap = new Heap<Node, Integer>(cmp);
			
			for(Edge e: state.currentNode().getExits()) {
				e = new Edge(state.currentNode(),e.getDest(), e.length);
				Node neighbor = e.getOther(state.currentNode());
				
				if(e.length+pathWeight(GraphAlgorithms.shortestPath(neighbor, state.getExit()))
						<= state.stepsLeft()){
							coinHeap.add(neighbor, -neighbor.getTile().coins());
						}
			}
				Node next = coinHeap.poll();
				if(state.currentNode().getTile().coins()>0) state.moveTo(next);
				else {
					Node flee = feasibleFlee(state);
					if(flee == null) {
						fleeShortestPath(state, state.getExit());
					}
					else {
						fleeShortestPath(state, flee);
					}
				}
			}			
	}	
	
	/**
	 * Moves along path by Dijkstra's Shortest Path Algorithm
	 * 
	 * @param
	 * last - the last node of a path
	 * */
	public void fleeShortestPath(FleeState state, Node last) {
		List<Node> path = null;
		path = GraphAlgorithms.shortestPath(state.currentNode(), last);
		
		path.remove(0);
		for(Node n:path) {
			state.moveTo(n);
		}
	}
	
	/**
	 * Finds all the shortest paths to all nodes with coins and returns node with 
	 * the lowest distance:coin ratio that can be reached afterwards.
	 * 
	 * Returns null if no such node exists
	 */
	public Node feasibleFlee(FleeState state) {
		Collection<Node> nodes = state.allNodes();
		Comparator<Double> cmp = Comparator.naturalOrder();
		Heap<Node, Double> coinHeap = new Heap<Node, Double>(cmp);
		HashMap<Node,Double> nodeData = new HashMap<Node, Double>(); 
		
		for (Node n: nodes){
			if(n.getTile().coins()>0) {
				double dist = pathWeight(GraphAlgorithms.shortestPath(state.currentNode(), n));
				nodeData.put(n, dist);
				coinHeap.add(n, dist/n.getTile().coins());
			}
		}
		
		double currentToExit=0;
		int startToExit = Integer.MAX_VALUE;
		Node maxCoin = null;
		boolean isFeasible =false;
		while(!isFeasible) {
			if(coinHeap.size() == 0) return null;
			maxCoin = coinHeap.poll();
			if(maxCoin == state.currentNode()) maxCoin = coinHeap.poll();
			currentToExit = nodeData.get(maxCoin);
			startToExit = pathWeight(GraphAlgorithms.shortestPath(maxCoin, state.getExit()));
			isFeasible = (currentToExit + startToExit)<=state.stepsLeft();
		}
		return maxCoin;
	}
	
	/**
	 * Returns the sum of the weights of the edges on a path
	 * 
	 * @param
	 * path - path of nodes traversed 
	 * */
	public static int pathWeight(List<Node> path) {
		if(path.size()==0) return 0;
		synchronized(path) {
			Iterator<Node> iter = path.iterator();
			Node p = iter.next();
			int s = 0;
			while(iter.hasNext()) {
				Node q = iter.next();
				s = s+p.getEdge(q).length;
				p = q;
			}
			return s;
		}
	}
	
	
	/**
	 * help move to a adjacent & not adjacent node with known shortest path
	 */
	private void moveToHelper(Long id,AllWentNodes gameMap,GameState state) {
		FindNode startNode= gameMap.getNode(state.currentLocation());
		FindNode endNode= gameMap.getNode(id);
		List<FindNode> path=GraphAlgorithms.shortestPath(startNode, endNode);
		System.out.println(path + " path" );
		Iterator<FindNode> loopAllPath=path.iterator();
		Long test=null;
		if(loopAllPath.hasNext())
			test =loopAllPath.next().getId();
		while(loopAllPath.hasNext()) {
			test =loopAllPath.next().getId();
		//	System.out.println(path.iterator().next().getId());
			state.moveTo(test);
		}
	}
	
	
	public static class AllWentNodes extends ArrayList<FindNode>{
		
		/**
		 * Serial UID
		 */
		private static final long serialVersionUID = 1L;
		
		public void addNode(FindNode newNode,Long connectedNodeID) {
			this.add(newNode);
			if(connectedNodeID != null) {
				FindNode connectedNode=getNode(connectedNodeID);
				FindEdge addEdge1= new FindEdge(newNode, connectedNode, 1);
				FindEdge addEdge2= new FindEdge(connectedNode, newNode, 1);
				connectedNode.addFindEdge(addEdge1);
				connectedNode.addFindEdge(addEdge2);
				newNode.addFindEdge(addEdge1);
				newNode.addFindEdge(addEdge2);
			}
			
		}
		
		public FindNode getNode(long id) {
			for(int i=0;i<this.size();i++) {
				if(this.get(i).getId()==id)
					return this.get(i);
			}
			return null;
		}
		
	}
	/**
	 * comparator (give the smallest value)
	 */
	public static class NodeComparator implements Comparator<Integer>{
		
		public NodeComparator() {
			super();
		}
		
		@Override
		public int compare(Integer o1, Integer o2) {
			if(o1==o2)
				return 0;
			return o1<o2?1:-1;
		}
	}
	

}
