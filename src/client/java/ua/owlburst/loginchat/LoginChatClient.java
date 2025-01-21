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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
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
				if (LoginChatConfig.HANDLER.instance().isListPerServer) send(client, ip);
                else send(client, commandsList);
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
							, false);
					if (LoginChatConfig.HANDLER.instance().isListPerServer) {
						client.player.sendMessage(Text.literal("[Login Chat] ").append(Text.translatable("loginchat.chat.listPerServerEnabled")).append(Text.of(" ")), false);
					}
				}
				LOGGER.info("Connecting to the server: {}", ip);
			}
		} else {
			if (LoginChatConfig.HANDLER.instance().isEnabledInSingleplayer) {
				LOGGER.info("Joining the singleplayer world");
				if (LoginChatConfig.HANDLER.instance().isListPerServer) send(client, "localhost");
				else send(client, commandsList);
			}
		}
	}

	private static void send(MinecraftClient client, @NotNull ArrayList<String> commandsList) {
		ExecutorService commandsExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Login Chat"));
		commandsList.forEach(el -> commandsExecutor.submit(new SendCommandTask(client, el)));
	}

	private static void send(MinecraftClient client, @NotNull String ip) {
		ArrayList<String> commandsList = new ArrayList<>();
		File file = new File(LoginChatConfig.MOD_CONFIG_FOLDER, ip + ".txt");
		if (file.exists()) {
			try (Scanner sc = new Scanner(file)) {
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.startsWith("#") || line.trim().isBlank()) continue;
					LOGGER.info("Message: {}", line);
					commandsList.add(line);
				}
			} catch (FileNotFoundException ignored) {

			}
		}
		send(client, commandsList);
	}

	@Override
	public void onInitializeClient() {
		LoginChatConfig.HANDLER.load();
		LoginChatConfig.MOD_CONFIG_FOLDER.mkdirs();
		if (LoginChatConfig.MOD_CONFIG_FOLDER.isDirectory() && Objects.requireNonNull(LoginChatConfig.MOD_CONFIG_FOLDER.listFiles()).length <= 0) {
			File file = new File(LoginChatConfig.MOD_CONFIG_FOLDER, "mc.example.com.txt");
            try {
                if (file.createNewFile()) {
					try (FileWriter fw = new FileWriter(file)){
						fw.append(Text.literal("""
                                # This is an example of a messages list definition file
                                # The mod will read the messages from this file and send them to the server
                                # if the Messages List Mode is set to PER_SERVER (isListPerServer config option is true)
                                # when trying to connect to mc.example.com, as denoted by the filename.
                                # Below is an example definition of a chat message and a command
                                
                                # To set up the per server config, create a text file in this directory
                                # with a server IP as it's filename and a txt extension
                                # Then put the messages you would like to send to that server inside of it (one message per line)
                                # Then add the server IP to the Servers List. If the server isn't added to the List,
                                # the mod won't send anything even if the corresponding file is present in this directory
                                
                                # The format also supports comments. All the lines that begin with a hash, like this one, will be ignored.
                                # Note that only the full-line comments are supported, if you'll put a hash in the middle of the line,
                                # it won't comment out the rest of the line after itself.
                                
                                # The singleplayer world is represented by `localhost.txt`, to choose the lines that'll be sent in singleplayer,
                                # create the file `localhost.txt` in this directory, put the messages there and tick the 'Enabled in singleplayer'
                                # box in the mod configuration.
                                
                                Hello! I'm an example message that will be sent when connecting to mc.example.com, as per my file name.
                                /say And I am a command that will be executed upon joining the said server after the message above will be sent.
                                # Just like the shared Messages List in the GUI and JSON config, just customised per server!""").getLiteralString());
					}
				}
            } catch (IOException e) {
                LOGGER.error("Exception thrown when trying to create the mod config folder", e);
            }
        }
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
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
            } else {
				LoginChatClient.LOGGER.warn("Can't send the chat message, can't get the player data");
			}
		}
	}
}
