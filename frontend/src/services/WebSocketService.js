/**
 * WebSocketService
 * 
 * Manages WebSocket connections for real-time updates.
 * Uses SockJS and STOMP for WebSocket communication with the backend.
 * 
 * Requirements: 3.2, 3.3, 10.1, 10.2
 */

import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
    }

    /**
     * Connect to WebSocket server
     * @param {Function} onConnect - Callback when connection is established
     * @param {Function} onError - Callback when connection error occurs
     */
    connect(onConnect, onError) {
        if (this.connected && this.client && this.client.active) {
            console.log('WebSocket already connected');
            if (onConnect) onConnect();
            return;
        }

        // If there's an existing client, clean it up first
        if (this.client) {
            try {
                this.client.deactivate();
            } catch (e) {
                console.warn('Error deactivating existing client:', e);
            }
        }

        try {
            // Create STOMP client with SockJS
            // Use relative URL - Vite proxy will forward to backend
            this.client = new Client({
                webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
                reconnectDelay: 0, // Disable automatic reconnection, we'll handle it manually
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                debug: (str) => {
                    // Only log errors in production
                    if (import.meta.env.MODE === 'development') {
                        console.log('STOMP:', str);
                    }
                },
                onConnect: () => {
                    console.log('WebSocket connected');
                    this.connected = true;
                    this.reconnectAttempts = 0;
                    if (onConnect) onConnect();
                },
                onStompError: (frame) => {
                    console.error('STOMP error:', frame);
                    this.connected = false;
                    if (onError) onError(frame);
                },
                onWebSocketClose: () => {
                    console.log('WebSocket connection closed');
                    this.connected = false;
                    this.handleReconnect(onConnect, onError);
                },
                onWebSocketError: (error) => {
                    console.error('WebSocket error:', error);
                    this.connected = false;
                }
            });

            // Activate the client
            this.client.activate();
        } catch (error) {
            console.error('Failed to create WebSocket connection:', error);
            if (onError) onError(error);
        }
    }

    /**
     * Handle reconnection logic
     * @private
     */
    handleReconnect(onConnect, onError) {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

            setTimeout(() => {
                this.connect(onConnect, onError);
            }, this.reconnectDelay * this.reconnectAttempts);
        } else {
            console.error('Max reconnection attempts reached');
            if (onError) {
                onError(new Error('Failed to reconnect after multiple attempts'));
            }
        }
    }

    /**
     * Subscribe to a topic
     * @param {string} topic - Topic to subscribe to (e.g., '/topic/locations')
     * @param {Function} callback - Callback function to handle messages
     * @returns {string} Subscription ID
     */
    subscribe(topic, callback) {
        if (!this.connected || !this.client) {
            console.error('WebSocket not connected. Cannot subscribe to', topic);
            return null;
        }

        try {
            const subscription = this.client.subscribe(topic, (message) => {
                try {
                    const data = JSON.parse(message.body);
                    callback(data);
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            });

            const subscriptionId = subscription.id;
            this.subscriptions.set(subscriptionId, subscription);
            console.log(`Subscribed to ${topic} with ID ${subscriptionId}`);

            return subscriptionId;
        } catch (error) {
            console.error('Failed to subscribe to topic:', topic, error);
            return null;
        }
    }

    /**
     * Unsubscribe from a topic
     * @param {string} subscriptionId - Subscription ID returned from subscribe()
     */
    unsubscribe(subscriptionId) {
        if (!subscriptionId) return;

        const subscription = this.subscriptions.get(subscriptionId);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(subscriptionId);
            console.log(`Unsubscribed from subscription ${subscriptionId}`);
        }
    }

    /**
     * Send a message to a destination
     * @param {string} destination - Destination to send to (e.g., '/app/location')
     * @param {Object} body - Message body
     */
    send(destination, body) {
        if (!this.connected || !this.client) {
            console.error('WebSocket not connected. Cannot send message to', destination);
            return;
        }

        try {
            this.client.publish({
                destination,
                body: JSON.stringify(body)
            });
        } catch (error) {
            console.error('Failed to send message:', error);
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect() {
        if (this.client) {
            // Unsubscribe from all topics
            this.subscriptions.forEach((subscription, id) => {
                try {
                    this.unsubscribe(id);
                } catch (e) {
                    console.warn('Error unsubscribing:', e);
                }
            });

            // Deactivate the client
            try {
                this.client.deactivate();
            } catch (e) {
                console.warn('Error deactivating client:', e);
            }

            this.connected = false;
            this.client = null;
            this.reconnectAttempts = 0; // Reset reconnect attempts on manual disconnect
            console.log('WebSocket disconnected');
        }
    }

    /**
     * Check if WebSocket is connected
     * @returns {boolean} True if connected
     */
    isConnected() {
        return this.connected;
    }
}

// Export singleton instance
const webSocketService = new WebSocketService();

export default webSocketService;
