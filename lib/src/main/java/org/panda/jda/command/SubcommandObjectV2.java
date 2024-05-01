package org.panda.jda.command;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Collections;

public abstract class SubcommandObjectV2 extends SubcommandData {

    public SubcommandObjectV2(String name, String description) {
        super(name, description);
    }

    public void processAutoCompleteInteraction(EventEntities<CommandAutoCompleteInteractionEvent> entities) {
        entities.getEvent().replyChoices(Collections.emptyList()).queue();
    }

    public void processSlashCommandInteraction(EventEntities<SlashCommandInteractionEvent> entities) {
        entities.getEvent().reply("Not yet implemented.").setEphemeral(true).queue();
    }
}