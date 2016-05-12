/**
 * Copyright Esophose 2016
 * While using any of the code provided by this plugin
 * you must not claim it as your own. This plugin may
 * be modified and installed on a server, but may not
 * be distributed to any person by any means.
 */

// Fixed worlds missing from /pp help
// Add style "feet"

package com.esophose.playerparticles;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.esophose.playerparticles.libraries.databases.MySQL;
import com.esophose.playerparticles.libraries.particles.ParticleEffect.ParticleType;
import com.esophose.playerparticles.updater.PluginUpdateListener;
import com.esophose.playerparticles.updater.Updater;

public class PlayerParticles extends JavaPlugin {
	
	/**
	 * The version a new update has, will be null if the config has it disabled or if there is no new version
	 */
	public static String updateVersion = null;
	
	/**
	 * The MySQL connection
	 */
	public static MySQL mySQL = null;
	public static Connection c = null;
	
	/**
	 * Whether or not to use MySQL as determined in the config
	 */
	public static boolean useMySQL = false;
	
	/**
	 * Saves the default config if it doesn't exist
	 * Registers the tab completer and the event listeners
	 * Checks if the config needs to be updated to the new version
	 * Makes sure the database is accessable
	 * Updates the map and styleMap @see ParticleCreator
	 * Starts the particle spawning task
	 * Checks for any updates if checking is enabled in the config
	 */
	public void onEnable(){
		saveDefaultConfig();
		getCommand("pp").setTabCompleter(new ParticleCommandCompleter());
		Bukkit.getPluginManager().registerEvents(new ParticleCreator(), this);
		Bukkit.getPluginManager().registerEvents(new PluginUpdateListener(), this);
		if(getConfig().getDouble("version") < Double.parseDouble(getDescription().getVersion())) {
			File configFile = new File(getDataFolder(), "config.yml");
			configFile.delete();
			saveDefaultConfig();
			reloadConfig();
			getLogger().warning("[PlayerParticles] config.yml has been updated!");
		}
		checkDatabase();
		ParticleCreator.updateMap();
		ParticleCreator.updateStyleMap();
		startTasks();
		
		// Check for an update
		if(shouldCheckUpdates()) {
			getLogger().info("[PlayerParticles] Checking for an update...");
			Updater updater = new Updater(this, 82823, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
			if(Double.parseDouble(updater.getLatestName().replaceAll("PlayerParticles v", "")) > Double.parseDouble(getPlugin().getDescription().getVersion())) {
				updateVersion = updater.getLatestName().replaceAll("PlayerParticles v", "");
				getLogger().info("[PlayerParticles] An update (v" + updateVersion + ") is available! You are running v" + getPlugin().getDescription().getVersion());
			} else {
				getLogger().info("[PlayerParticles] No update was found");
			}
		}
	}
	
	/**
	 * Gets the instance of the plugin running on the server
	 * 
	 * @return The PlayerParticles plugin instance
	 */
	public static Plugin getPlugin(){
		return Bukkit.getPluginManager().getPlugin("PlayerParticles");
	}
	
	/**
	 * Checks the config if the plugin can look for updates
	 * 
	 * @return True if check-updates is set to true in the config
	 */
	public boolean shouldCheckUpdates() {
		return getConfig().getBoolean("check-updates");
	}
	
	/**
	 * Checks if database-enable is true in the config, if it is then continue
	 * Gets the database connection information from the config and tries to connect to the server
	 * Creates a new table if it doesn't exist called playerparticles
	 * Sets useMySQL to true if it connects successfully, and false if it fails or isn't enabled
	 */
	private void checkDatabase() {
		if(getConfig().getBoolean("database-enable")) {
			String hostname = getConfig().getString("database-hostname");
			String port = getConfig().getString("database-port");
			String database = getConfig().getString("database-name");
			String user = getConfig().getString("database-user-name");
			String pass = getConfig().getString("database-user-password");
			mySQL = new MySQL(hostname, port, database, user, pass);
			try {
				c = mySQL.openConnection();
				Statement statement = c.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS playerparticles (player_name VARCHAR(32), particle VARCHAR(32), style VARCHAR(32));");
				useMySQL = true;
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
				getLogger().info("[PlayerParticles] Failed to connect to MySQL Database! Check to see if your config is correct!");
				useMySQL = false;
			}
		}else{
			useMySQL = false;
		}
		getLogger().info("[PlayerParticles] Using mySQL for data storage: " + useMySQL);
	}
	
	/**
	 * Starts the task reponsible for spawning particles
	 * Starts two with 1 tick delay if ticks-per-particle is set to 0.5
	 */
	private void startTasks() {
		double ticks = getConfig().getDouble("ticks-per-particle");
		if(ticks == 0.5){
			new ParticleCreator().runTaskTimer(this, 20, 1);
			new ParticleCreator().runTaskTimer(this, 20, 1);
		}else
			new ParticleCreator().runTaskTimer(this, 20, (long) ticks);
	}
	
	/**
	 * Called when a player does a command and continues if the command is /pp
	 * Executes all the commands and methods 
	 * Does some sorcery 
	 * 
	 * Needs to be rewritten as a separate CommandManager
	 * 
	 * @param sender Who executed the command
	 * @param cmd The command
	 * @param label The command label
	 * @param args The arguments following the command
	 * @return True if everything went as planned (should always be true)
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) return true;
		Player p = (Player) sender;
		if(args.length == 1 && args[0].equalsIgnoreCase("worlds")) {
			String worlds = "";
			if(ConfigManager.getInstance().getDisabledWorlds() == null) {
				MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-disabled-worlds-none")).replace("&", "�"), ChatColor.GREEN);
			}
			for(String s : ConfigManager.getInstance().getDisabledWorlds()) {
				worlds += s + ", ";
			}
			if(worlds.length() > 2) worlds = worlds.substring(0, worlds.length() - 2);
			if(worlds.equals("")) {
				worlds = ((String)getConfig().get("message-disabled-worlds-none")).replace("&", "�");
			}else{
				worlds = ((String)getConfig().get("message-disabled-worlds")).replace("&", "�") + " " + ChatColor.AQUA + worlds;
			}
			MessageManager.getInstance().sendMessage(p, worlds, ChatColor.GREEN);
			return true;
		}
		if(args.length > 1 && args[0].equalsIgnoreCase("style")) {
			String argument = args[1].replace("_", "");
			if(ParticleStyle.styleFromString(argument) != null){
				ParticleStyle style = ParticleStyle.styleFromString(argument);
				if(!PermissionHandler.hasStylePermission(p, style)) {
					MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-no-permission-style")).replace("{STYLE}", ChatColor.AQUA + style.toString().toLowerCase() + ChatColor.RED).replace("&", "�"), ChatColor.RED);
					return true;
				}
				ConfigManager.getStyleInstance().setStyle(style, p);
				ParticleCreator.addStyleMap(p, style);
				MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-now-using-style")).replace("{STYLE}", ChatColor.AQUA + style.toString().toLowerCase() + ChatColor.GREEN).replace("&", "�"), ChatColor.GREEN);
				return true;
			}
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-invalid-type-style")).replace("&", "�") + ChatColor.GREEN + " /pp styles", ChatColor.RED);
			return true;
		}
		if(args.length != 1){
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-invalid-arguments")).replace("&", "�") + ChatColor.GREEN + " /pp list", ChatColor.RED);
			return true;
		}
		String argument = args[0].replace("_", "");
		if(ParticleCreator.particleFromString(argument) != null){
			ParticleType effect = ParticleCreator.particleFromString(argument);
			if(!PermissionHandler.hasPermission(p, effect)){
				MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-no-permission")).replace("{PARTICLE}", ChatColor.AQUA + (effect.equals(ParticleType.RAINBOW) ? "rainbow" : effect.getName().toLowerCase() + ChatColor.RED)).replace("&", "�"), ChatColor.RED);
				return true;
			}
			ConfigManager.getInstance().setParticle(effect, p);
			ParticleCreator.addMap(p, effect);
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-now-using")).replace("{PARTICLE}", ChatColor.AQUA + (effect.equals(ParticleType.RAINBOW) ? "rainbow" : effect.getName().toLowerCase() + ChatColor.GREEN)).replace("&", "�"), ChatColor.GREEN);
			return true;
		}
		if(argument.equalsIgnoreCase("clear")) {
			ConfigManager.getInstance().resetParticle(p);
			ParticleCreator.removeMap(p);
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-cleared-particles")).replace("&", "�"), ChatColor.GREEN);
			return true;
		}
		if(argument.equalsIgnoreCase("version")) {
			MessageManager.getInstance().sendMessage(p, "Running PlayerParticles v" + getDescription().getVersion(), ChatColor.GOLD);
			MessageManager.getInstance().sendMessage(p, "Plugin created by: Esophose", ChatColor.GOLD);
			return true;
		}
		if(argument.equalsIgnoreCase("help")) {
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-available-commands")).replace("&", "�"), ChatColor.GREEN);
			MessageManager.getInstance().sendMessage(p, "list, styles, style, worlds, version, help", ChatColor.AQUA);
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-usage")).replace("&", "�") + ChatColor.AQUA + " /pp <Command>", ChatColor.YELLOW);
			return true;
		}
		if(argument.equalsIgnoreCase("list")) {
			String toSend = ((String)getConfig().get("message-use")).replace("&", "�") + " ";
			for(ParticleType effect : ParticleType.values()){
				if(PermissionHandler.hasPermission(p, effect)){
					toSend = toSend + (effect.equals(ParticleType.RAINBOW) ? "rainbow" : effect.getName().toLowerCase()) + ", ";
					continue;
				}
			}
			if(toSend.equals(getConfig().get("message-use") + " ")){
				MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-no-particles")).replace("&", "�"), ChatColor.RED);
				return true;
			}
			toSend = toSend + "clear";
			MessageManager.getInstance().sendMessage(p, toSend, ChatColor.GREEN);
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-usage")).replace("&", "�") + ChatColor.AQUA + " /pp <Type>", ChatColor.YELLOW);
			return true;
		}
		if(argument.equalsIgnoreCase("style")) {
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-invalid-type-style")).replace("&", "�") + ChatColor.GREEN + " /pp styles", ChatColor.RED);
			return true;
		}
		if(argument.equalsIgnoreCase("styles")) {
			String toSend = ((String)getConfig().get("message-use-style")).replace("&", "�") + " ";
			for(ParticleStyle style : ParticleStyle.values()){
				if(PermissionHandler.hasStylePermission(p, style)){
					toSend = toSend + (style.toString().toLowerCase()) + ", ";
					continue;
				}
			}
			if(toSend.equals(((String)getConfig().get("message-use-style")).replace("&", "�") + " ")) {
				MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-no-styles")).replace("&", "�"), ChatColor.RED);
				return true;
			}
			MessageManager.getInstance().sendMessage(p, toSend, ChatColor.GREEN);
			MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-usage")).replace("&", "�") + ChatColor.AQUA + " /pp style <Type>", ChatColor.YELLOW);
			return true;
		}
		
		MessageManager.getInstance().sendMessage(p, ((String)getConfig().get("message-invalid-type")).replace("&", "�") + ChatColor.GREEN + " /pp list", ChatColor.RED);
		
		return true;
	}
	
}
