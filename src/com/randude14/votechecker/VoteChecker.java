package com.randude14.votechecker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VoteChecker extends JavaPlugin {
	public static final char[] specialChars = {'_', '-'};
	private static final Logger logger = Logger.getLogger("Minecraft");
	private static final String ANONYMOUS = "Anonymous";
	private static VoteChecker instance;
	private static Permission perm = null;

	public void onEnable() {
		instance = this;
		if (!loadPerms())
			logger.info(this
					+ " - failed to load permissions from vault, defaulting to bukkit permissions.");
		else
			logger.info(this + " - permissions loaded");
		logger.info(this + " - enabled.");
	}

	public void onDisable() {
		logger.info(this + " - disabled.");
	}

	private boolean loadPerms() {
		RegisteredServiceProvider<Permission> provider = getServer()
				.getServicesManager().getRegistration(Permission.class);
		if (provider == null)
			return false;
		perm = provider.getProvider();
		return perm != null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (label.equalsIgnoreCase("vhelp")) {
				if (!checkPermission(player, Perm.vhelp)) {
					return false;
				}

				getCommands(player);
			}

			else if (label.equalsIgnoreCase("vcheck")) {
				if (!checkPermission(player, Perm.vcheck)) {
					return false;
				}
				String name = (args.length == 0) ? player.getName() : args[0];
				int count = getVoteCount(name);
				player.sendMessage(ChatColor.YELLOW
						+ String.format(
								"%s has voted %d time(s) for this server.",
								name, count));
			}

			else if (label.equalsIgnoreCase("vtop")) {
				if (!checkPermission(player, Perm.vtop)) {
					return false;
				}

				displayTop10(player);
			}

			else if (label.equalsIgnoreCase("vlist")) {
				if (!checkPermission(player, Perm.vlist)) {
					return false;
				}

				int page;

				if (args.length > 0) {
					try {
						page = Integer.parseInt(args[0]);
					} catch (Exception ex) {
						player.sendMessage(ChatColor.RED + args[0]
								+ " is not an integer.");
						return false;
					}

				}

				else {
					page = 1;
				}

				listVoters(player, page);
			}

			else if (label.equalsIgnoreCase("vtotal")) {
				if (!checkPermission(player, Perm.vlist)) {
					return false;
				}

				player.sendMessage(ChatColor.YELLOW
						+ String.format(
								"There are %d total votes for this server.",
								getTotalVoteCount()));
			}

		}

		else {

			if (label.equalsIgnoreCase("vhelp")) {
				getCommands(sender);
			}

			else if (label.equalsIgnoreCase("vcheck")) {
				if (args.length == 0) {
					sender.sendMessage("/vcheck <player>");
					return false;
				}
				int count = getVoteCount(args[0]);
				sender.sendMessage(ChatColor.YELLOW
						+ String.format(
								"%s has voted %d time(s) for this server.",
								args[0], count));
			}

			else if (label.equalsIgnoreCase("vtop")) {
				displayTop10(sender);
			}

			else if (label.equalsIgnoreCase("vlist")) {
				int page;

				if (args.length > 0) {
					try {
						page = Integer.parseInt(args[0]);
					} catch (Exception ex) {
						sender.sendMessage(args[0] + " is not an integer.");
						return false;
					}

				}

				else {
					page = 1;
				}

				listVoters(sender, page);
			}

		}
		return false;
	}

	private void getCommands(Player player) {
		player.sendMessage(ChatColor.GREEN + "--------[" + ChatColor.GOLD
				+ "Commands" + ChatColor.GREEN + "]--------");
		player.sendMessage(ChatColor.YELLOW + "/vhelp - shows this");
		player.sendMessage(ChatColor.YELLOW
				+ "/vcheck <player> - check yours/players amount of votes for this server");
		player.sendMessage(ChatColor.YELLOW + "/vtop - display top 10 voters");
		player.sendMessage(ChatColor.YELLOW
				+ "/vlist <page> - list voters by page");
	}

	private void getCommands(CommandSender sender) {
		sender.sendMessage("--------[Commands]--------");
		sender.sendMessage("/vhelp - shows this");
		sender.sendMessage("/vcheck <player> - check yours/players amount of votes for this server");
		sender.sendMessage("/vtop - display top 10 voters");
		sender.sendMessage("/vlist <page> - list voters by page");
	}

	private void displayTop10(Player player) {
		List<Voter> voters = getVotersList();
		player.sendMessage(ChatColor.GREEN + "--------[" + ChatColor.GOLD
				+ "Top 10 Voters" + ChatColor.GREEN + "]--------");
		for (int cntr = 0; cntr < 10 && cntr < voters.size(); cntr++) {
			Voter voter = voters.get(cntr);
			if(voter.getName().equalsIgnoreCase(ANONYMOUS)) {
				voters.remove(cntr);
				cntr--;
				continue;
			}
			String mess = String.format(
					"%d. %s - voted %d time(s) for this server", cntr + 1,
					voter.getName(), voter.getVotes());
			player.sendMessage(ChatColor.YELLOW + mess);
		}

	}

	private void displayTop10(CommandSender sender) {
		List<Voter> voters = getVotersList();
		sender.sendMessage("--------[Top 10 Voters]--------");
		for (int cntr = 0; cntr < 10 && cntr < voters.size(); cntr++) {
			Voter voter = voters.get(cntr);
			String mess = String.format(
					"%d. %s - voted %d time(s) for this server", cntr + 1,
					voter.getName(), voter.getVotes());
			sender.sendMessage(mess);
		}

	}

	private void listVoters(Player player, int page) {
		List<Voter> voters = getVotersList();
		int len = voters.size();
		int max = (len / 10) + 1;
		if (len % 10 == 0)
			max--;
		if (page > max)
			page = max;
		if (page < 1)
			page = 1;
		player.sendMessage(String.format(
				"%s--------[%sVoters (%d/%d)%s]--------",
				ChatColor.GREEN.toString(), ChatColor.GOLD.toString(), page,
				max, ChatColor.GREEN.toString()));
		for (int cntr = (page * 10) - 10, stop = cntr + 10; cntr < stop
				&& cntr < len; cntr++) {
			Voter voter1 = voters.get(cntr);
			String mess = String.format(
					"%d. %s - voted %d time(s) for this server", cntr + 1,
					voter1.getName(), voter1.getVotes());
			player.sendMessage(ChatColor.YELLOW + mess);
		}

	}

	private void listVoters(CommandSender sender, int page) {
		List<Voter> voters = getVotersList();
		int len = voters.size();
		int max = (len / 10) + 1;
		if (len % 10 == 0)
			max--;
		if (page > max)
			page = max;
		if (page < 1)
			page = 1;
		sender.sendMessage(String.format("--------[Voters (%d/%d)]--------",
				page, max));
		for (int cntr = (page * 10) - 10, stop = cntr + 10; cntr < stop
				&& cntr < len; cntr++) {
			Voter voter = voters.get(cntr);
			String mess = String.format(
					"%d. %s - voted %d time(s) for this server", cntr + 1,
					voter.getName(), voter.getVotes());
			sender.sendMessage(mess);
		}

	}

	private static int getTotalVoteCount() {
		try {
			int count = 0;
			Scanner scan = new Scanner(getLogFile());
			while (scan.hasNextLine()) {
				scan.nextLine();
				count++;
			}
			return count;
		} catch (Exception ex) {
			logger.info(instance + " - error occurred in getTotalVoteCount()");

		}
		return 0;
	}

	public static File getLogFile() {
		Plugin p = Bukkit.getPluginManager().getPlugin("Votifier");
		if (p == null)
			return null;
		return new File(p.getDataFolder(), "votes.log");
	}

	private static List<Voter> getVotersList() {
		List<Voter> voters = new ArrayList<Voter>();
		try {
			Scanner scan = new Scanner(getLogFile());
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String username = getUsername(line);
				boolean found = false;
				for (Voter voter : voters) {
					if (voter.equals(username)) {
						voter.inc();
						found = true;
						break;
					}

				}

				if (!found) {
					voters.add(new Voter(username));
				}

			}
		} catch (Exception ex) {
			logger.info(instance + " - error occurred in getVotersList()");
			return voters;
		}
		Collections.sort(voters);
		return voters;
	}

	public static int getVoteCount(String player) {
		int count = 0;
		try {
			File file = getLogFile();
			if (file == null)
				return 0;
			Scanner scan = new Scanner(file);
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String username = getUsername(line);
				if (username.equals(player))
					count++;
			}
		} catch (IOException ex) {
			logger.info(instance + " - error occurred in getVoteCount()");
		}

		return count;
	}

	private static String getUsername(String line) {
		int userIndex = line.indexOf("username:");
		int stopIndex = line.indexOf("address:");
		if (userIndex < 0 || stopIndex < 0)
			return "";
		userIndex += 9;
		stopIndex -= 1;
		return line.substring(userIndex, stopIndex);
	}
	
	public static String matchPlayer(String player) {
		for(OfflinePlayer op : Bukkit.getOfflinePlayers()) {
			String name = op.getName();
			for(char c : specialChars) {
				name = name.replace(Character.toString(c), "");
				player = player.replace(Character.toString(c), "");
			}
			
			if(name.equalsIgnoreCase(player)) {
				return name;
			}
			
		}
		return player;
	}

	public boolean hasPermission(Player player, String permission) {
		if (perm == null)
			return player.hasPermission(permission);
		else
			return perm.has(player, permission);
	}

	public boolean checkPermission(Player player, String permission) {
		if (!hasPermission(player, permission)) {
			player.sendMessage(ChatColor.RED + "You do not have permission.");
			return false;
		}
		return true;
	}

}
