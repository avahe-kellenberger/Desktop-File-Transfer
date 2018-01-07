package tech.avahe.filetransfer.threading;

/**
 *
 * @author Avahe
 *
 */
public class ThreadSignaller {
	
	private boolean signaled = false;
	
	/**
	 * Notifies the <code>signaled</code> boolean to be set to true,
	 * and notifies all threads waiting on the object.
	 */
	public synchronized void set() {
		this.signaled = true;
        this.notifyAll();
    }

	/**
	 * Blocks the current thread until the <code>signaled</code> boolean is set to true.
	 * @throws InterruptedException See {@link Object#wait()}.
	 */
	public synchronized void waitIndefinitely() throws InterruptedException {
		this.signaled = false;
		do {
			this.wait();
		} while (!this.signaled);
	}

	/**
	 * Blocks the current thread until the <code>signaled</code> boolean is set to true.
	 * @param timeout The time in milliseconds.
	 * @throws InterruptedException See {@link Object#wait()}.
	 */
	public synchronized void waitForTimeout(final long timeout) throws InterruptedException {
		if (timeout <= 0) {
			this.waitIndefinitely();
		} else {
			final long startNanos = System.nanoTime();
			final long timeoutNanos = timeout * 1000000;
			long remainingTimeout = timeout;
			this.signaled = false;
			do {
				this.wait(remainingTimeout);
				if (this.signaled) {
					break;
				}
				final long elapsedNanos = System.nanoTime() - startNanos;
				if (elapsedNanos >= timeoutNanos) {
					break;
				}
				remainingTimeout = timeout - (elapsedNanos / 1000000);
			} while (true);
		}
	}
	
}