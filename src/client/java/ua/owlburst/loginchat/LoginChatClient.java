package ua.owlburst.loginchat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class LoginChatClient implements ClientModInitializer {
	public static int delayedMessagesCount = 0;
	public static final String MOD_ID = "loginchat";
	public static final Logger LOGGER = LoggerFactory.getLogger("loginchat");
	private static void onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
		LoginChatClient.delayedMessagesCount = 0;
		ArrayList<String> serversList = new ArrayList<>(LoginChatConfig.HANDLER.instance().serversList);
		ArrayList<String> commandsList = new ArrayList<>(LoginChatConfig.HANDLER.instance().commandsList);
		LOGGER.info(MessageFormat.format("Server in the list: {0}", serversList.toArray()));
		boolean isSinglePlayer;
		try {
			isSinglePlayer = client.getServer().isSingleplayer();
		} catch (NullPointerException e) {
			isSinglePlayer = false;
		}
		LOGGER.info("Is singleplayer? - " + isSinglePlayer);
		if(!isSinglePlayer) {
			String ip = handler.getConnection().getAddress().toString();
			ip = ip.split("/")[0].replaceAll("\\.$", "");
			if (serversList.contains(ip)) {
				send(client, commandsList);
			} else {
				if (client.player != null) {
					client.player
							.sendMessage(Text.literal("[Login Chat] ").append(Text.translatable("loginchat.chat.ip")).append(Text.of(" "))
							.append(Text.literal(ip)
									.setStyle(Style.EMPTY
											.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ip))
											.withFormatting(Formatting.YELLOW)
											.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("loginchat.chat.clipboard")))
									)
							)
							);
				}
				LOGGER.info("Connecting to the server: {}", ip);
			}
		} else {
			if (LoginChatConfig.HANDLER.instance().isEnabledInSingleplayer) {
				LOGGER.info("Joining the singleplayer world");
				send(client, commandsList);
			}
		}
	}

	private static void send(MinecraftClient client, @NotNull ArrayList<String> commandsList) {
		ExecutorService commandsExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Login Chat"));
		commandsList.forEach(el -> commandsExecutor.submit(new SendCommandTask(client, el)));
	}

	@Override
	public void onInitializeClient() {
		LoginChatConfig.HANDLER.load();
		ClientPlayConnectionEvents.JOIN.register((LoginChatClient::onPlayReady));

	}
}

class SendCommandTask implements Runnable {
	MinecraftClient client;
	String input;

	public SendCommandTask(MinecraftClient client, String input) {
		this.client = client;
		this.input = input;
	}

	public void run() {
		int chatMessagesDelay = LoginChatConfig.HANDLER.instance().chatMessagesDelay;
		if (chatMessagesDelay > 0 && LoginChatClient.delayedMessagesCount <= 0) {
			LoginChatClient.LOGGER.info(MessageFormat.format("Delaying the chat messages by {0} " +
							"milliseconds",
					chatMessagesDelay));
			try {
				sleep(chatMessagesDelay);
				LoginChatClient.delayedMessagesCount++;
			} catch (InterruptedException ignored) {
			}
		}
		if (input.startsWith("/")) {
			input = input.substring(1);
			LoginChatClient.LOGGER.info(MessageFormat.format("Command to execute: {0}", this.input));
			for (int i = 0; i < 5; i++) {
				if (ClientCommandManager.getActiveDispatcher() != null && client.player != null) {
					client.player.networkHandler.sendChatCommand(input);
                    break;
				} else {
					LoginChatClient.LOGGER.error(MessageFormat.format("Unable to execute the command: {0}...", input));
					try {
						sleep(1000);
					} catch (InterruptedException ignored) {
					}
				}
			}
		} else {
			if (client.player != null) {
				LoginChatClient.LOGGER.info(MessageFormat.format("Sending the chat message: {0}", this.input));
				client.player.networkHandler.sendChatMessage(input);
                try {
                    sleep(100);
                } catch (InterruptedException ignored) {
                }
            } else {
				LoginChatClient.LOGGER.warn("Can't send the chat message, can't get the player data");
			}
		}
	}
}
