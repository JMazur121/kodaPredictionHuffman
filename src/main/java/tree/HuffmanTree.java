package tree;

import frequency.FrequencyMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@AllArgsConstructor
@Getter
public class HuffmanTree {

	private Node root;

	public HuffmanTree(FrequencyMap map) {
		PriorityQueue<Node> pq = new PriorityQueue<>(512);
		map.getFrequencyMap().forEach((key, value) -> {
			Node node = new Node(key, value, null, null);
			pq.add(node);
		});
		while (pq.size() > 1) {
			Node least = pq.poll();
			Node secondLeast = pq.poll();
			Node parent = new Node(0, least.getFrequency() + secondLeast.getFrequency(), least, secondLeast);
			pq.add(parent);
		}
		root = pq.poll();
	}

	public Map<Integer, String> getCodeBook() {
		Map<Integer, String> codeBook = new HashMap<>(512);
		buildCodeBook(codeBook, root, "");
		return codeBook;
	}

	private void buildCodeBook(Map<Integer, String> book, Node node, String sequence) {
		if (node.isLeaf()) {
			book.put(node.getCodedValue(), sequence);
			return;
		}
		buildCodeBook(book, node.getLeft(), sequence + "0");
		buildCodeBook(book, node.getRight(), sequence + "1");
	}

}
