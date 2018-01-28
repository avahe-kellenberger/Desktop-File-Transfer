package tech.avahe.filetransfer.threading;

/**
 *
 * @author Avahe
 *
 */
public class ThreadSignaller {
	
	private boolean signalled = false;
	
	/**
	 * Signals any threads to wake up that are waiting on this signaller.
	 */
	public synchronized void signal() {
		this.signalled = true;
        this.notifyAll();
    }

	/**
	 * Resets the signaller so no threads that are waiting on this signaller wake up anymore.
	 */
	public synchronized void reset() {
		this.signalled = false;
	}

	/**
	 * Blocks the current thread until signalled.
	 * @throws InterruptedException See {@link Object#wait()}.
	 */
	public synchronized void waitIndefinitely() throws InterruptedException {
		while (!this.signalled) {
			this.wait();
		}
	}

	/**
	 * Blocks the current thread until signalled or the timeout is exceeded.
	 * @param timeout The time in milliseconds.
	 * @throws InterruptedException See {@link Object#wait()}.
	 * @return If the signaller was set, otherwise false.
	 */
	public synchronized boolean waitForTimeout(final long timeout) throws InterruptedException {
		if (timeout <= 0) {
			return false;
		}
		final long startNanos = System.nanoTime();
		final long timeoutNanos = timeout * 1000000;
		while (!this.signalled) {
			final long elapsedNanos = System.nanoTime() - startNanos;
			if (elapsedNanos >= timeoutNanos) {
				return false;
			}
			final long remainingTimeout = timeout - (elapsedNanos / 1000000);
			this.wait(remainingTimeout);
		}
		return true;
	}
	
}