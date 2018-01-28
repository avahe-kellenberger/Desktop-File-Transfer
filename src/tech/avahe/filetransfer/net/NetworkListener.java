package tech.avahe.filetransfer.net;

import tech.avahe.filetransfer.threading.ThreadSignaller;
import tech.avahe.filetransfer.util.Buffers;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

/**
 * @author Avahe
 */
public abstract class NetworkListener {

    private final CopyOnWriteArraySet<BiConsumer<SocketAddress, ByteBuffer>> dataListeners = new CopyOnWriteArraySet<>();
    private final ThreadSignaller listenerThreadStartedSignaller = new ThreadSignaller();
    private final ThreadSignaller listenerThreadStoppedSignaller = new ThreadSignaller();
    private final Object listenerThreadLock = new Object();
    private Thread listenerThread;
    private boolean shouldBeListening = false;

    /**
     * Prepares the networked data source for reading.
     * @throws IOException An error occurred while preparing the data source.
     */
    protected abstract void prepare() throws IOException;

    /**
     * Reads from a networked data source.
     * @param buffer The byte buffer to fill with read data.
     * @return The remote socket address where the data was received from.
     */
    protected abstract SocketAddress read(final ByteBuffer buffer) throws IOException;

    /**
     * Receives data in a loop from the underlying ByteChannel.
     */
    private void listen() {
        try {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            this.prepare();
            this.listenerThreadStartedSignaller.signal();
            while (true) {
                final SocketAddress remoteAddress = this.read(buffer);
                buffer.flip();
                if (!this.shouldBeListening) {
                    break;
                }
                // Ensures thread isn't interrupted (because we should be listening)
                Thread.interrupted();
                this.notifyDataListeners(remoteAddress, Buffers.copy(buffer));
                buffer.clear();
            }
        } catch (Exception ex) {
            // Silently ignore the exception, as the loop will exit if the connection drops.
        } finally {
            this.onListeningStopped();
        }
    }

    /**
     * Notifies the listeners of incoming data.
     * @param buffer The data buffer to send to all the listeners.
     */
    private void notifyDataListeners(final SocketAddress remoteAddress, final ByteBuffer buffer) {
        this.dataListeners.forEach(listener -> listener.accept(remoteAddress, buffer.asReadOnlyBuffer()));
    }

    /**
     * Invoked when the listening thread exits.
     */
    private void onListeningStopped() {
        this.listenerThreadStoppedSignaller.signal();
        synchronized (this.listenerThreadLock) {
            this.listenerThread = null;
        }
    }

    /**
     * Tells the client to start listening for incoming packets.
     * @param timeout The time (in milliseconds) to wait for the network listener to start listening.
     * @return If the client has started listening after this method call.
     * This method will return false if it was already listening for packets.
     * @throws InterruptedException Thrown if the current thread is interrupted while waiting
     * for the client to start listening.
     */
    public boolean startListening(final long timeout) throws InterruptedException {
        if (this.isListening()) {
            return false;
        }
        synchronized (this.listenerThreadLock) {
            this.shouldBeListening = true;
            this.listenerThread = new Thread(this::listen);
            this.listenerThreadStartedSignaller.reset();
            this.listenerThread.start();
            return this.listenerThreadStartedSignaller.waitForTimeout(timeout);
        }
    }

    /**
     * @return If the client is listening for incoming messages.
     */
    public boolean isListening() {
        synchronized (this.listenerThreadLock) {
            return this.listenerThread != null && this.listenerThread.isAlive() &&
                   !this.listenerThread.isInterrupted() && this.shouldBeListening;
        }
    }

    /**
     * Stops the client from listening to incoming data.
     */
    public void stopListening() {
        if (this.isListening()) {
            synchronized (this.listenerThreadLock) {
                this.shouldBeListening = false;
                this.listenerThread.interrupt();
            }
        }
    }

    /**
     * Stops the client from listening and waits for the listen thread to die.
     * @param timeout The time (in milliseconds) to wait for the listen thread to die.
     * @return Whether the listen thread died within the timeout.
     */
    public boolean stopListening(final long timeout) throws InterruptedException {
        this.listenerThreadStoppedSignaller.reset();
        this.stopListening();
        return this.listenerThreadStoppedSignaller.waitForTimeout(timeout);
    }

    /**
     * Adds a listener to the client, which is notified when data is received.
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.dataListeners.add(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.dataListeners.contains(listener);
    }

    /**
     * Removes a message listener from the client.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeDataListener(final BiConsumer<SocketAddress, ByteBuffer> listener) {
        return this.dataListeners.remove(listener);
    }

}