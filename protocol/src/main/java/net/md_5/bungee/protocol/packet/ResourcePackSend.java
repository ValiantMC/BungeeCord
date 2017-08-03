package net.md_5.bungee.protocol.packet;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@NoArgsConstructor
public class ResourcePackSend extends DefinedPacket {
    private String url;
    private String hash;

    public ResourcePackSend(String url, String hash) {
        this.url = url;
        this.hash = hash == null ? Hashing.sha1().hashString(url, Charsets.UTF_8).toString().toLowerCase() : hash;
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

    @Override
    public void read(ByteBuf buf) {
        this.url = readString(buf);
        try {
            this.hash = readString(buf);
        } catch (IndexOutOfBoundsException e) {

        }
    }

    @Override
    public void write(ByteBuf buf) {
        writeString(this.url, buf);
        writeString(this.hash == null ? "" : this.hash, buf);
    }

    @Override
    public String toString() {
        return "ResourcePackSend{" +
                "url='" + url + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
