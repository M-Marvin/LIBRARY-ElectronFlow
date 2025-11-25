package de.m_marvin.electronflow.components;

import de.m_marvin.electronflow.Component;

public abstract class FourPort extends Component {
	
	protected final int nodeA;
	protected final int nodeB;
	protected final int nodeC;
	protected final int nodeD;
	
	public FourPort(String name, int nodeA, int nodeB, int nodeC, int nodeD) {
		super(name);
		this.nodeA = nodeA;
		this.nodeB = nodeB;
		this.nodeC = nodeC;
		this.nodeD = nodeD;
	}
	
	@Override
	public int[] nodes() {
		return new int[] {
			this.nodeA,
			this.nodeB,
			this.nodeC,
			this.nodeD
		};
	}
	
}
