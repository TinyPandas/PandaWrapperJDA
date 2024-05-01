package org.panda.jda.command;

import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import org.panda.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class CommandObjectV2 extends ListenerAdapter {

    public static final Logger commandLogger = LoggerFactory.getLogger(CommandObjectV2.class);

    private final String name;
    private final String description;
    private final List<OptionData> optionDataList;
    private final Map<String, SubcommandObjectV2> subcommandMap;
    private final List<SubcommandGroupData> subcommandGroups;
    private final boolean isGuildOnly;
    private Consumer<EventEntities<SlashCommandInteractionEvent>> eventCallback;
    private boolean isAutocomplete = false;
    private Map<String, SubcommandObjectV2> loadedSubcommands;

    public CommandObjectV2(String name, String description) {
        this(name, description, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), false);
    }

    public CommandObjectV2(String name, String description, boolean isGuildOnly) {
        this(name, description, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), isGuildOnly);
    }

    public CommandObjectV2(String name, String description, List<OptionData> optionDataList, Map<String, SubcommandObjectV2> subcommandMap, List<SubcommandGroupData> subcommandGroups, boolean isGuildOnly) {
        this.name = name;
        this.description = description;
        this.optionDataList = new ArrayList<>(optionDataList);
        this.subcommandMap = new HashMap<>(subcommandMap);
        this.subcommandGroups = new ArrayList<>(subcommandGroups);
        this.isGuildOnly = isGuildOnly;
    }

    public void addOptionData(OptionData optionData) {
        optionDataList.add(optionData);
    }

    public void addSubcommand(SubcommandObjectV2 subcommandObjectV2) {
        subcommandMap.put(subcommandObjectV2.getName(), subcommandObjectV2);
        long autocompletableOptions = subcommandObjectV2.getOptions().stream().filter(OptionData::isAutoComplete).count();
        if (autocompletableOptions > 0) {
            setAutocomplete(true);
        }
    }

    public void addSubcommandGroup(SubcommandGroupData subcommandGroupData) {
        subcommandGroups.add(subcommandGroupData);
        subcommandGroupData.getSubcommands().forEach(subcommandData -> {
            long autocompletableOptions = subcommandData.getOptions().stream().filter(OptionData::isAutoComplete).count();
            if (autocompletableOptions > 0) {
                setAutocomplete(true);
            }
        });
    }

    public void setAutocomplete(boolean isAutocomplete) {
        this.isAutocomplete = isAutocomplete;
    }

    public void setEventCallback(Consumer<EventEntities<SlashCommandInteractionEvent>> eventCallback) {
        this.eventCallback = eventCallback;
    }

    public SlashCommandData getSlashCommandImplementation() {
        loadedSubcommands = new HashMap<>(subcommandMap);

        subcommandGroups.forEach(group ->
            group.getSubcommands().forEach(data ->
                loadedSubcommands.put(
                    group.getName().toLowerCase() + "_" + data.getName().toLowerCase(),
                    (SubcommandObjectV2) data)));

        return Commands.slash(name, description)
            .addOptions(optionDataList)
            .addSubcommands(subcommandMap.values())
            .addSubcommandGroups(subcommandGroups)
            .setGuildOnly(isGuildOnly);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        EventEntities<SlashCommandInteractionEvent> entities;
        try {
            entities = new EventEntities<>(event);
        } catch (ValidationException e) {
            commandLogger.warn(e.toEventResponse());
            event.reply("Something went wrong processing your event.").setEphemeral(true).queue();
            return;
        }

        String subcommandName = event.getSubcommandName();
        String subCommandGroup = event.getSubcommandGroup();

        if (Objects.nonNull(subcommandName)) {
            String subcommandKey = subcommandName.toLowerCase();
            if (Objects.nonNull(subCommandGroup)) {
                subcommandKey = subCommandGroup.toLowerCase() + "_" + subcommandKey;
            }

            SubcommandObjectV2 subcommandObjectV2 = loadedSubcommands.get(subcommandKey);

            if (Objects.nonNull(subcommandObjectV2)) {
                subcommandObjectV2.processSlashCommandInteraction(entities);
            } else {
                event.reply("No implementation for " + event.getName() + " -> " + subcommandName).setEphemeral(true).queue();
            }
            return;
        }

        processSlashCommand(entities);

        if (!event.isAcknowledged()) {
            event.reply("No implementation for " + event.getName()).setEphemeral(true).queue();
        }
    }

    public void processSlashCommand(@NotNull EventEntities<SlashCommandInteractionEvent> entities) { }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        EventEntities<CommandAutoCompleteInteractionEvent> entities;
        try {
            entities = new EventEntities<>(event);
        } catch (ValidationException e) {
            commandLogger.warn(e.toEventResponse());
            event.replyChoices(Collections.emptyList()).queue();
            return;
        }

        String subcommandName = event.getSubcommandName();
        if (Objects.nonNull(subcommandName)) {
            SubcommandObjectV2 subcommandObjectV2 = subcommandMap.get(subcommandName);
            if (Objects.nonNull(subcommandObjectV2)) {
                subcommandObjectV2.processAutoCompleteInteraction(entities);
                return;
            }
        }

        processAutoComplete(entities);

        if (!event.isAcknowledged()) {
            event.replyChoices(Collections.emptyList()).queue();
        }
    }

    public void processAutoComplete(@NotNull EventEntities<CommandAutoCompleteInteractionEvent> entities) { }

}
