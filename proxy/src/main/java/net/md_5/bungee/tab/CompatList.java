package net.md_5.bungee.tab;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.PlayerListItem;

import java.util.Collection;

public class CompatList extends TabList {

    private boolean sentPing;
    private String name;

    public CompatList(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public void onUpdate(PlayerListItem playerListItem) {
        if(this.name.equals("") || this.name.equals(" ")) return;
        sendGM();
        if (playerListItem.getAction() == PlayerListItem.Action.UPDATE_GAMEMODE) {
            for (PlayerListItem.Item i : playerListItem.getItems()) {
                if (!i.getUuid().equals(this.player.getUniqueId())) {
                    return;
                }
            }
            this.player.unsafe().sendPacket(playerListItem);
        }
    }

    @Override
    public void onPingChange(int ping) {
        sendGM();
    }

    public void sendGM() {
        if(this.name.equals("") || this.name.equals(" ")) return;
        for (ProxiedPlayer p : BungeeCord.getInstance().getPlayers()) {
            if (!p.getUUID().equals(this.player.getUUID())) {
                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.UPDATE_GAMEMODE);
                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(player.getUniqueId());
                item.setUsername(player.getName());
                item.setDisplayName(player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_8 ? ComponentSerializer.toString(TextComponent.fromLegacyText(p.getDisplayName())) : p.getDisplayName
                        ());
                if (player.getServer() != null && p.getServer() != null) {
                    if (player.getServer().getInfo().equals(p.getServer().getInfo())) {
                        item.setGamemode(0);
                        System.out.println();
                    } else {
                        item.setGamemode(3);
                    }
                } else {
                    item.setGamemode(0);
                }
                packet.setItems(new PlayerListItem.Item[]
                        {
                                item
                        });
                p.unsafe().sendPacket(packet);
            }
        }
    }

    @Override
    public void onServerChange() {
        sendGM();
    }

    @Override
    public void onConnect() {
        this.sentPing = false;
        this.name = player.getDisplayName();
        if(this.name.equals("") || this.name.equals(" ")) return;
        PlayerListItem playerListItem = new PlayerListItem();
        playerListItem.setAction(PlayerListItem.Action.ADD_PLAYER);
        Collection<ProxiedPlayer> players = BungeeCord.getInstance().getPlayers();
        PlayerListItem.Item[] items = new PlayerListItem.Item[players.size()];
        playerListItem.setItems(items);
        int i = 0;
        for (ProxiedPlayer p : players) {
            if(p == null) continue;
            PlayerListItem.Item item = items[i++] = new PlayerListItem.Item();
            item.setUuid(p.getUniqueId());
            item.setUsername(p.getName());
            if (p.getDisplayName().equals("") || p.getDisplayName().equals(" ")) continue;
            item.setDisplayName(player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_8 ? ComponentSerializer.toString(TextComponent.fromLegacyText(p.getDisplayName())) : p.getDisplayName
                    ());
            LoginResult loginResult = ((UserConnection) p).getPendingConnection().getLoginProfile();
            if (loginResult != null) {
                String[][] props = new String[loginResult.getProperties().length][];
                for (int j = 0; j < props.length; j++) {
                    props[j] = new String[]
                            {
                                    loginResult.getProperties()[j].getName(),
                                    loginResult.getProperties()[j].getValue(),
                                    loginResult.getProperties()[j].getSignature()
                            };
                }
                item.setProperties(props);
            } else {
                item.setProperties(new String[0][0]);
            }
            if (player.getServer() != null && p.getServer() != null) {
                if (player.getServer().equals(p.getServer())) {
                    item.setGamemode(0);
                } else {
                    item.setGamemode(3);
                }
            } else {
                item.setGamemode(0);
            }
            item.setPing(p.getPing());
        }
        if (player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_8) {
            player.unsafe().sendPacket(playerListItem);
            playerListItem.setAction(PlayerListItem.Action.UPDATE_DISPLAY_NAME);
            player.unsafe().sendPacket(playerListItem);
        } else {
            // Split up the packet
            for (PlayerListItem.Item item : playerListItem.getItems()) {
                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.ADD_PLAYER);

                packet.setItems(new PlayerListItem.Item[]
                        {
                                item
                        });
                player.unsafe().sendPacket(packet);
            }
        }
        for (ProxiedPlayer p : BungeeCord.getInstance().getPlayers()) {
            if(p == null) continue;
            PlayerListItem packet = new PlayerListItem();
            packet.setAction(PlayerListItem.Action.ADD_PLAYER);
            if (this.name.equals("") || this.name.equals(" ")) continue;
            PlayerListItem.Item item = new PlayerListItem.Item();
            item.setUuid(player.getUniqueId());
            item.setUsername(player.getName());
            //item.setDisplayName(ComponentSerializer.toString(TextComponent.fromLegacyText("im a fucking duck ok")));
            item.setDisplayName(p.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_8 ? ComponentSerializer.toString(TextComponent.fromLegacyText(this.name)) : this.name);
            // BEFORE
            LoginResult loginResult = ((UserConnection) player).getPendingConnection().getLoginProfile();
            //AFTER
//			LoginResult loginResult = ((UserConnection) p).getPendingConnection().getLoginProfile();
            if (loginResult != null) {
                String[][] props = new String[loginResult.getProperties().length][];
                for (int j = 0; j < props.length; j++) {
                    props[j] = new String[]
                            {
                                    loginResult.getProperties()[j].getName(),
                                    loginResult.getProperties()[j].getValue(),
                                    loginResult.getProperties()[j].getSignature()
                            };
                }
                item.setProperties(props);
            } else {
                item.setProperties(new String[0][0]);
            }
            item.setGamemode(0);
            item.setPing(player.getPing());
            packet.setItems(new PlayerListItem.Item[]
                    {
                            item
                    });
            p.unsafe().sendPacket(packet);

        }

    }

    @Override
    public void onDisconnect() {
        if(this.name.equals("") || this.name.equals(" ")) return;

        for (ProxiedPlayer p : BungeeCord.getInstance().getPlayers()) {
            if(p == null) continue;
            PlayerListItem packet = new PlayerListItem();
            packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
            PlayerListItem.Item item = new PlayerListItem.Item();
            item.setUuid(player.getUniqueId());
            item.setUsername(player.getName());
            packet.setItems(new PlayerListItem.Item[]
                    {
                            item
                    });
            p.unsafe().sendPacket(packet);
        }
    }
}
