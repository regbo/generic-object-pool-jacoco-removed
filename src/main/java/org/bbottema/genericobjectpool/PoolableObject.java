package org.bbottema.genericobjectpool;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import static lombok.AccessLevel.PACKAGE;
import static org.bbottema.genericobjectpool.PoolableObject.PoolStatus.AVAILABLE;

/**
 * A Object Pool Entry which wraps the underlying claimed Object as V.
 *
 * @param <T> the value type
 */
@ToString
public class PoolableObject<T> {

	enum PoolStatus {
		AVAILABLE, CLAIMED, WAITING_FOR_DEALLOCATION, DEALLOCATED
	}

	@ToString.Exclude
	private final SimpleObjectPool<T> pool;
	/**
	 * Contains:
	 * <ul>
	 *     <li>fully allocated object when claimed</li>
	 *     <li>re-allocated object for reuse when claiming again</li>
	 *     <li>de-allocated object for reuse when waiting to be claimed again</li>
	 *     <li>null, in case the object was invalidated and consequently fully deallocated or when the pool shutting down</li>
	 * </ul>
	 */
	@Getter @Nullable private T allocatedObject;
	@Getter private final Date createdOn = new Date();
	/**
	 * Millisecond stamp from {@link System#currentTimeMillis()}, similar to {@link #getCreatedOn()}.
	 */
	private final long creationStampMs;
	private long allocationStampMs;
	@Nullable @Getter @Setter private Long expiryTimestamp;
	/**
	 * Performance optimisation: this field keeps track of the list this poolable object is in, so we don't have to do {@code .contains(object)}
	 * all the time.
	 */
	@NotNull @Getter(PACKAGE) @Setter(PACKAGE) private PoolStatus currentPoolStatus;
	
	PoolableObject(@NotNull SimpleObjectPool<T> pool, @NotNull T allocatedObject) {
		this.pool = pool;
		this.allocatedObject = allocatedObject;
		this.creationStampMs = System.currentTimeMillis();
		this.allocationStampMs = creationStampMs;
		this.currentPoolStatus = AVAILABLE;
	}

	/**
	 * Releases this Object back into the Pool allowing others to access it
	 */
	public void release() {
		pool.releasePoolableObject(this);
	}

	/**
	 * Releases the object from the pool and removes it.  If the key associated with this object no longer has available object(s) to claim against depending on the Pool Configuration then a new
	 * object will be created on the next request.
	 */
	public void invalidate() {
		pool.invalidatePoolableObject(this);
	}
	
	/**
	 * Used internally to make sure that when shutting down the pool, no poolable object still references an
	 * object even if the user still keeps the Poolable object somewhere.
	 */
	void dereferenceObject() {
		allocatedObject = null;
	}
	
	void resetAllocationTimestamp() {
		allocationStampMs = System.currentTimeMillis();
	}
	
	/**
	 * @return The numbers of milliseconds since this object was created.
	 */
	@SuppressWarnings("unused")
	public long ageMs() {
		return System.currentTimeMillis() - creationStampMs;
	}
	
	/**
	 * @return The numbers of milliseconds since this object was allocated last.
	 */
	@SuppressWarnings("WeakerAccess")
	public long allocationAgeMs() {
		return System.currentTimeMillis() -  allocationStampMs;
	}
}