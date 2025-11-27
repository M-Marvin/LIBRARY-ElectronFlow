package de.m_marvin.electronflow.ltisolver.components;

import de.m_marvin.electronflow.ltisolver.Component;

public abstract class TwoPort extends Component {
	
	protected final int nodeA;
	protected final int nodeB;
	
	public TwoPort(String name, int nodeA, int nodeB) {
		super(name);
		this.nodeA = nodeA;
		this.nodeB = nodeB;
	}
	
	@Override
	public int[] nodes() {
		return new int[] {
			this.nodeA,
			this.nodeB
		};
	}
	
}
