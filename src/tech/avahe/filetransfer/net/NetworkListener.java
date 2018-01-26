package tech.avahe.filetransfer.net;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public abstract class NetworkListener {

    private Thread listenerThread = new Thread(this::listen);
    private final Object listenerThreadLock = new Object();
    private boolean shouldBeListening = false;
    private final CopyOnWriteArraySet<Consumer<String>> messageListeners = new CopyOnWriteArraySet<>();

    /**
     * Receives datagram packets from the MulticastSocket.
     */
    protected abstract void listen();

    /**
     * Sets the listening thread's interrupted state to false.
     */
    protected void clearThreadInterruptedState() {
        synchronized (this.listenerThreadLock) {
            if (this.listenerThread.isInterrupted()) {
                Thread.interrupted();
            }
        }
    }

    /**
     * Notifies the listeners with a message.
     * @param message The message to send to all the listeners.
     */
    protected void notifyMessageListeners(final String message) {
        this.messageListeners.forEach(listener -> listener.accept(message));
    }

    /**
     * Tells the client to start listening for incoming packets.
     * @return If the client has started listening after this method call.port
     * This method will return false if it was already listening for packets.
     */
    public boolean startListening() {
        if (this.isListening()) {
            return false;
        }
        synchronized (this.listenerThreadLock) {
            this.shouldBeListening = true;
            this.listenerThread = this.createListenerThread();
            this.listenerThread.start();
        }
        return true;
    }

    /**
     * Creates a thread which invokes NetworkListener{@link #listen()},
     * then safely shuts down when that method exists.
     * @return A thread which invokes {@link NetworkListener#listen()} and safely shuts down afterward.
     */
    private Thread createListenerThread() {
        return new Thread(() -> {
            try {
                this.listen();
            } finally {
                this.onListeningStopped();
            }
        });
    }

    /**
     * Invoked when the listening thread exits.
     */
    private void onListeningStopped() {
        this.listenerThread = null;
        this.shouldBeListening = false;
    }

    /**
     * @return If the client should be listening, regardless of if it is listening or not.
     */
    protected boolean shouldBeListening() {
        return this.shouldBeListening;
    }

    /**
     * @return If the client is listening for incoming messages.
     */
    public boolean isListening() {
        synchronized (this.listenerThreadLock) {
            return this.listenerThread != null && this.listenerThread.isAlive() && !this.listenerThread.isInterrupted() && this.shouldBeListening;
        }
    }

    /**
     * Stops the client from listening to incoming data.
     * @return If the client was not listening for data prior to this method being called.
     */
    public boolean stopListening() {
        synchronized (this.listenerThreadLock) {
            if (this.isListening()) {
                this.listenerThread.interrupt();
                this.shouldBeListening = false;
                return true;
            }
            return false;
        }
    }

    /**
     * Adds a listener to the client, which is notified when a packet is received.
     * @param listener The listener to add.
     * @return If the listener was added successfully.
     */
    public boolean addMessageListener(final Consumer<String> listener) {
        return this.messageListeners.add(listener);
    }

    /**
     * Checks if a message listener has been added to the client.
     * @param listener The listener to check for.
     * @return If the client contains the listener.
     */
    public boolean containsMessageListener(final Consumer<String> listener) {
        return this.messageListeners.contains(listener);
    }

    /**
     * Removes a message listener from the client.
     * @param listener The listener to remove.
     * @return If the listener was removed successfully.
     */
    public boolean removeMessageListener(final Consumer<String> listener) {
        return this.messageListeners.remove(listener);
    }

}