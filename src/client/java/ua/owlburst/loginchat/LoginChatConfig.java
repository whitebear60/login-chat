package ua.owlburst.loginchat;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class LoginChatConfig{
    public static final File MOD_CONFIG_FOLDER = YACLPlatform.getConfigDir().resolve(LoginChatClient.MOD_ID).toFile();
    public static ConfigClassHandler<LoginChatConfig> HANDLER = ConfigClassHandler.createBuilder(LoginChatConfig.class)
            .id(Identifier.tryParse(LoginChatClient.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(LoginChatClient.MOD_ID + ".json"))
                    .build())
            .build();

    @SerialEntry
    public boolean isEnabledInSingleplayer = false;
    @SerialEntry
    public List<String> serversList = new ArrayList<>();
    @SerialEntry
    public List<String> commandsList = new ArrayList<>();

    @SerialEntry
    public boolean isListPerServer = false;

    @SerialEntry
    public int chatMessagesDelay = 0;

    public static Screen getModConfigScreenFactory(Screen parentScreen) {
        return YetAnotherConfigLib.create(LoginChatConfig.HANDLER, (defaults, config, builder) -> builder
                        .title(Text.of("Login Chat Config"))
                        .category(ConfigCategory.createBuilder()
                                .name(Text.of("Login Chat"))
                                .group(ListOption.<String>createBuilder()
                                        .name(Text.translatable("loginchat.config.serverslist"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.serverslist")))
                                        .controller(StringControllerBuilder::create)
                                        .binding(
                                                defaults.serversList,
                                                () -> config.serversList,
                                                (value) -> config.serversList = value
                                        )
                                        .initial("")
                                        .insertEntriesAtEnd(true)
                                        .build())
                                .group(ListOption.<String>createBuilder()
                                        .name(Text.translatable("loginchat.config.messageslist"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.messageslist.desc")))
                                        .controller(StringControllerBuilder::create)
                                        .binding(
                                                defaults.commandsList,
                                                () -> config.commandsList,
                                                (value) -> config.commandsList = value
                                        )
                                        .initial("")
                                        .insertEntriesAtEnd(true)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.translatable("loginchat.config.singleplayer"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.singleplayer.desc")))
                                        .controller(TickBoxControllerBuilder::create)
                                        .binding(defaults.isEnabledInSingleplayer,
                                                () -> config.isEnabledInSingleplayer,
                                                (value) -> config.isEnabledInSingleplayer = value)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("loginchat.config.delay"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.delay" +
                                                ".desc")))
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 10000)
                                                .step(500)
                                                .formatValue(val -> Text.of(MessageFormat.format("{0} ms", val))))
                                        .binding(Binding.generic(defaults.chatMessagesDelay,
                                                () -> config.chatMessagesDelay,
                                                (value) -> config.chatMessagesDelay = value))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.translatable("loginchat.config.messageslist.mode"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.messageslist.mode.desc")))
                                        .controller(booleanOption -> BooleanControllerBuilder.create(booleanOption)
                                                .formatValue(value -> value ? Text.literal("PER_SERVER") : Text.literal("SHARED"))
                                                .coloured(true))
                                        .binding(Binding.generic(defaults.isListPerServer,
                                                () -> config.isListPerServer,
                                                (val) -> config.isListPerServer = val))
                                        .listener((booleanOption, aBoolean) -> {
                                            if (config.isListPerServer && !MOD_CONFIG_FOLDER.exists()) MOD_CONFIG_FOLDER.mkdirs();
                                        })
                                        .build())
                                .option(ButtonOption.createBuilder()
                                        .name(Text.translatable("loginchat.config.openfolder"))
                                        .description(OptionDescription.of(Text.translatable("loginchat.config.openfolder.desc")))
                                        .action((yaclScreen, buttonOption) -> Util.getOperatingSystem().open(MOD_CONFIG_FOLDER))
                                        .build())
                                .build())
                )
                .generateScreen(parentScreen);

    }
}
