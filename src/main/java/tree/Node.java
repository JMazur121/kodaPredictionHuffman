package tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Node implements Comparable<Node> {

	//ma znaczenie tylko dla liscia
	private int codedValue;
	private long frequency;
	private final Node left, right;

	public boolean isLeaf() {
		return left == null && right == null;
	}

	@Override
	public int compareTo(Node o) {
		return (int) (this.frequency - o.getFrequency());
	}

}
