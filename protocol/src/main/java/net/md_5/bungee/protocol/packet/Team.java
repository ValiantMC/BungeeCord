package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Team extends DefinedPacket {

    private String name;
    /**
     * 0 - create, 1 remove, 2 info update, 3 player add, 4 player remove.
     */
    private byte mode;
    private String displayName;
    private String prefix;
    private String suffix;
    private String nameTagVisibility;
    private String collisionRule;
    private int color;
    private byte friendlyFire;
    private String[] players;

    /**
     * Packet to destroy a team.
     */
    public Team(String name) {
        this.name = name;
        this.mode = 1;
    }

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        name = readString(buf);
        mode = buf.readByte();
        if (mode == 0 || mode == 2) {
            displayName = readString(buf);
            if (protocolVersion < ProtocolConstants.MINECRAFT_1_13) {
                prefix = readString(buf);
                suffix = readString(buf);
            }
            friendlyFire = buf.readByte();
            nameTagVisibility = readString(buf);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_9) {
                collisionRule = readString(buf);
            }
            color = (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) ? readVarInt(buf) : buf.readByte();
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                prefix = readString(buf);
                suffix = readString(buf);
            }
        }
        if (mode == 0 || mode == 3 || mode == 4) {
            int len = readVarInt(buf);
            players = new String[len];
            for (int i = 0; i < len; i++) {
                players[i] = readString(buf);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        writeString(name, buf);
        buf.writeByte(mode);
        if (mode == 0 || mode == 2) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13 && !displayName.startsWith("{")) {
                writeString(ComponentSerializer.toString(fromLegacyText(displayName, ChatColor.WHITE)), buf);
            } else {
                writeString(displayName, buf);
            }
            if (protocolVersion < ProtocolConstants.MINECRAFT_1_13) {
                writeString(prefix, buf);
                writeString(suffix, buf);
            }
            buf.writeByte(friendlyFire);
            writeString(nameTagVisibility, buf);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_9) {
                writeString(collisionRule, buf);
            }

            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                if (!prefix.startsWith("{") || !suffix.startsWith("{")) {
                    color = getLastColor(prefix);
                    writeVarInt(color, buf);
                    writeString(ComponentSerializer.toString(fromLegacyText(prefix, ChatColor.WHITE)), buf);
                    writeString(ComponentSerializer.toString(fromLegacyText(suffix, ChatColor.WHITE)), buf);
                } else {
                    writeVarInt(color, buf);
                    writeString(prefix, buf);
                    writeString(suffix, buf);
                }
            } else {
                buf.writeByte(color);
            }
        }
        if (mode == 0 || mode == 3 || mode == 4) {
            writeVarInt(players.length, buf);
            for (String player : players) {
                writeString(player, buf);
            }
        }
    }

    // Pulled from VV based on Bungee
    private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

    public int getLastColor(String input) {
        int length = input.length();

        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == ChatColor.COLOR_CHAR && index < length - 1) {
                char c = input.charAt(index + 1);
                ChatColor color = ChatColor.getByChar(c);

                if (color != null) {
                    switch (color) {
                        case MAGIC:
                        case BOLD:
                        case STRIKETHROUGH:
                        case UNDERLINE:
                        case ITALIC:
                        case RESET:
                            break;
                        default:
                            return color.ordinal();
                    }
                }
            }
        }

        return ChatColor.RESET.ordinal();
    }

    public static BaseComponent[] fromLegacyText(String message, ChatColor defaultColor) {
        ArrayList<BaseComponent> components = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        TextComponent component = new TextComponent();
        Matcher matcher = url.matcher(message);

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == ChatColor.COLOR_CHAR) {
                if (++i >= message.length()) {
                    break;
                }
                c = message.charAt(i);
                if (c >= 'A' && c <= 'Z') {
                    c += 32;
                }
                ChatColor format = ChatColor.getByChar(c);
                if (format == null) {
                    continue;
                }
                if (builder.length() > 0) {
                    TextComponent old = component;
                    component = new TextComponent(old);
                    old.setText(builder.toString());
                    builder = new StringBuilder();
                    components.add(old);
                }
                switch (format) {
                    case BOLD:
                        component.setBold(true);
                        break;
                    case ITALIC:
                        component.setItalic(true);
                        break;
                    case UNDERLINE:
                        component.setUnderlined(true);
                        break;
                    case STRIKETHROUGH:
                        component.setStrikethrough(true);
                        break;
                    case MAGIC:
                        component.setObfuscated(true);
                        break;
                    case RESET:
                        format = defaultColor;
                    default:
                        component = new TextComponent();
                        component.setColor(format);
                        // ViaVersion start
                        component.setBold(false);
                        component.setItalic(false);
                        component.setUnderlined(false);
                        component.setStrikethrough(false);
                        component.setObfuscated(false);
                        // ViaVersion end
                        break;
                }
                continue;
            }
            int pos = message.indexOf(' ', i);
            if (pos == -1) {
                pos = message.length();
            }
            if (matcher.region(i, pos).find()) { //Web link handling

                if (builder.length() > 0) {
                    TextComponent old = component;
                    component = new TextComponent(old);
                    old.setText(builder.toString());
                    builder = new StringBuilder();
                    components.add(old);
                }

                TextComponent old = component;
                component = new TextComponent(old);
                String urlString = message.substring(i, pos);
                component.setText(urlString);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        urlString.startsWith("http") ? urlString : "http://" + urlString));
                components.add(component);
                i += pos - i - 1;
                component = old;
                continue;
            }
            builder.append(c);
        }

        component.setText(builder.toString());
        components.add(component);

        return components.toArray(new BaseComponent[0]);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
