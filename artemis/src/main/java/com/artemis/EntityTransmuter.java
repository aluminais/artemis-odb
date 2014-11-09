package com.artemis;

import com.artemis.utils.Bag;

import java.util.BitSet;

public final class EntityTransmuter {
	private final World world;
	private final BitSet additions;
	private final BitSet removals;
	private final Bag<TransmuteOperation> operations;

	private final BitSet bs;

	EntityTransmuter(World world, BitSet additions, BitSet removals) {
		this.world = world;
		this.additions = additions;
		this.removals = removals;
		operations = new Bag<TransmuteOperation>();

		bs = new BitSet();
	}

	public void transmute(Entity e) {
		int compositionId = e.getCompositionId();
		TransmuteOperation operation = operations.safeGet(compositionId);
		if (operation == null) {
			operation = createOperation(e);
			operations.set(compositionId, operation);
		}

		operation.perform(e, world.getComponentManager());
		world.getEntityManager().setIdentity(e, operation);
	}

	private TransmuteOperation createOperation(Entity e) {
		BitSet origin = e.getComponentBits();
		bs.clear();
		bs.or(origin);
		bs.or(additions);
		bs.andNot(removals);
		int compositionId = world.getEntityManager().compositionIdentity(bs);
		return new TransmuteOperation(compositionId, getAdditions(origin), getRemovals(origin));
	}

	private Bag<ComponentType> getAdditions(BitSet origin) {
		ComponentTypeFactory tf = world.getComponentManager().typeFactory;
		Bag<ComponentType> types = new Bag<ComponentType>();
		for (int i = additions.nextSetBit(0); i >= 0; i = additions.nextSetBit(i + 1)) {
			if (!origin.get(i))
				types.add(tf.getTypeFor(i));
		}

		return types;
	}

	private Bag<ComponentType> getRemovals(BitSet origin) {
		ComponentTypeFactory tf = world.getComponentManager().typeFactory;
		Bag<ComponentType> types = new Bag<ComponentType>();
		for (int i = removals.nextSetBit(0); i >= 0; i = removals.nextSetBit(i + 1)) {
			if (origin.get(i))
				types.add(tf.getTypeFor(i));
		}

		return types;
	}

	static class TransmuteOperation {
		private Bag<ComponentType> additions;
		private Bag<ComponentType> removals;
		public final int compositionId;

		public TransmuteOperation(int compositionId, Bag<ComponentType> additions, Bag<ComponentType> removals) {
			this.compositionId = compositionId;
			this.additions = additions;
			this.removals = removals;
		}

		public void perform(Entity e, ComponentManager cm) {
			for (int i = 0, s = additions.size(); s > i; i++)
				cm.create(e, additions.get(i).getType());

			for (int i = 0, s = removals.size(); s > i; i++)
				cm.removeComponent(e, removals.get(i));
		}
	}
}