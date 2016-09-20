/**
 * Copyright Esophose 2016
 * While using any of the code provided by this plugin
 * you must not claim it as your own. This plugin may
 * be modified and installed on a server, but may not
 * be distributed to any person by any means.
 */

package com.esophose.playerparticles.manager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.esophose.playerparticles.PlayerParticles;

public class MessageManager {

	/**
	 * The instance of the MessageManager, we only need one of these
	 */
	private static MessageManager instance = new MessageManager();
	/**
	 * Values contained in the config used for custom messages
	 */
	private boolean messagesEnabled, prefixEnabled;
	/**
	 * The prefix to place before all sent messages contained in the config
	 */
	private String messagePrefix;

	/**
	 * Sets up all the above variables with values from the plugin config
	 */
	private MessageManager() {
		this.messagesEnabled = PlayerParticles.getPlugin().getConfig().getBoolean("messages-enabled");
		this.prefixEnabled = PlayerParticles.getPlugin().getConfig().getBoolean("use-message-prefix");
		this.messagePrefix = PlayerParticles.getPlugin().getConfig().getString("message-prefix");
		this.messagePrefix = ChatColor.translateAlternateColorCodes('&', this.messagePrefix);
	}

	/**
	 * Gets the instance of the MessageManager
	 * 
	 * @return The instance of the MessageManager
	 */
	public static MessageManager getInstance() {
		return instance;
	}

	/**
	 * Sends a message to a player
	 * 
	 * @param player The player to send the message to
	 * @param message The message to send to the player
	 * @param color The chat color to put before the message
	 */
	public void sendMessage(Player player, String message, ChatColor color) {
		if (!this.messagesEnabled) return;
		if (this.prefixEnabled) {
			message = this.messagePrefix + color + " " + message;
		} else {
			message = color + message;
		}
		player.sendMessage(message);
	}

	/**
	 * Sends a player a message without any custom coloring
	 * This will become the default in v4.1
	 * 
	 * @param player The player to send the message to
	 * @param message The message to send to the player
	 */
	public void sendMessage(Player player, String message) {
		if (!this.messagesEnabled) return;
		if (this.prefixEnabled) {
			message = this.messagePrefix + message;
		}
		player.sendMessage(message);
	}

	/**
	 * Gets a message from the config with the formatting specified
	 * 
	 * @param target The string to find in the config
	 * @param replacement The replacement for {TYPE}, can be null
	 * @return The message requested with the applied formatting
	 */
	public static String getMessageFromConfig(String target, String replacement) {
		String message = ChatColor.translateAlternateColorCodes('&', PlayerParticles.getPlugin().getConfig().getString(target));
		if (replacement != null) message = message.replaceAll("\\{TYPE\\}", replacement);
		return message;
	}

}