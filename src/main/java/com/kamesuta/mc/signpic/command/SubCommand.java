package com.kamesuta.mc.signpic.command;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public abstract class SubCommand implements IModCommand {
	private final String name;
	private final List<String> aliases = Lists.newArrayList();
	private SubCommand.PermLevel permLevel;
	private IModCommand parent;
	private final SortedSet<SubCommand> children;

	public static enum PermLevel {
		EVERYONE(0), ADMIN(2);

		int permLevel;

		private PermLevel(final int permLevel) {
			this.permLevel = permLevel;
		}
	}

	public SubCommand(final String name) {
		this.permLevel = SubCommand.PermLevel.EVERYONE;

		this.children = new TreeSet<SubCommand>(new Comparator<SubCommand>() {
			@Override
			public int compare(final SubCommand o1, final SubCommand o2) {
				return o1.compareTo(o2);

			}
		});
		this.name = name;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	public SubCommand addChildCommand(final SubCommand child) {
		child.setParent(this);
		this.children.add(child);
		return this;
	}

	void setParent(final IModCommand parent) {
		this.parent = parent;
	}

	@Override
	public SortedSet<SubCommand> getChildren() {
		return this.children;
	}

	public void addAlias(final String alias) {
		this.aliases.add(alias);
	}

	@Override
	public List<String> getAliases() {
		return this.aliases;
	}

	@Override
	public List<String> getTabCompletions(final MinecraftServer server, final ICommandSender sender, final String[] args, @Nullable final BlockPos pos) {
		return null;
	}

	@Override
	public void execute(final MinecraftServer server, final ICommandSender sender, final String[] args) throws CommandException {
		if (!CommandHelpers.processCommands(sender, this, args))
			processSubCommand(sender, args);
	}

	public List<String> completeCommand(final ICommandSender sender, final String[] args) {
		return CommandHelpers.completeCommands(sender, this, args);
	}

	public void processSubCommand(final ICommandSender sender, final String[] args) throws WrongUsageException {
		CommandHelpers.throwWrongUsage(sender, this);
	}

	public SubCommand setPermLevel(final SubCommand.PermLevel permLevel) {
		this.permLevel = permLevel;
		return this;
	}

	@Override
	public final int getRequiredPermissionLevel() {
		return this.permLevel.permLevel;
	}

	@Override
	public boolean checkPermission(final MinecraftServer server, final ICommandSender sender) {
		return sender.canUseCommand(getRequiredPermissionLevel(), getName());
	}

	@Override
	public boolean isUsernameIndex(final String[] args, final int index) {
		return false;
	}

	@Override
	public String getUsage(final ICommandSender sender) {
		return "/"+getFullCommandString()+" help";
	}

	@Override
	public void printHelp(final ICommandSender sender) {
		CommandHelpers.printHelp(sender, this);
	}

	@Override
	public String getFullCommandString() {
		return this.parent.getFullCommandString()+" "+getName();
	}

	@Override
	public int compareTo(final ICommand command) {
		return getName().compareTo(command.getName());
	}
}