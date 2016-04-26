package com.artemis.link;

import com.artemis.ComponentType;
import com.artemis.EntityManager;
import com.artemis.EntitySubscription;
import com.artemis.World;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.Field;

class SingleLinkSite extends LinkSite {

	private final IntBag sourceToTarget = new IntBag();
	private final EntityManager em;

	protected SingleLinkSite(World world,
	                         ComponentType type,
	                         Field field,
	                         EntitySubscription subscription) {

		super(world, type, field, subscription);
		em = world.getEntityManager();
	}


	@Override
	protected void check(int id) {
		int target = sourceToTarget.get(id);
		// -1 == not linked
		if (target != -1 && !em.isActive(target)) {
			// target appears dead, check if it's been updated
			int newTarget = entityReader.readField(mapper.get(id), field, null);
			if (newTarget == target) {
				if (listener != null) listener.onTargetDead(id, target);
			} else {
				sourceToTarget.set(id, newTarget);
				if (listener != null) listener.onTargetChanged(id, target);
			}
		}
	}

	@Override
	protected void insert(int id) {
		int target = entityReader.readField(mapper.get(id), field, null);
		if (target != -1) {
			sourceToTarget.set(id, target);
			listener.onLinkEstablished(id, target);
		}
	}

	@Override
	protected void removed(int id) {
		int target = sourceToTarget.size() > id
			? sourceToTarget.get(id)
			: -1;

		if (target != -1) {
			listener.onLinkKill(id);
			sourceToTarget.set(id, 0);
		}
	}
}
