package me.haydenb.assemblylinemachines.packets;

import java.util.HashMap;
import java.util.Set;
import java.util.function.*;

import com.mojang.datafixers.util.Pair;

import me.haydenb.assemblylinemachines.AssemblyLineMachines;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class HashPacketImpl {

	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(AssemblyLineMachines.MODID, "primary"), () -> "1", "1"::equals, "1"::equals);
	public static int ID = 0;
	
	
	
	public static class PacketData{
		
		private final HashMap<String, Pair<Integer, Object>> map;
		private final String title;
		
		public PacketData(String title) {
			this.map = new HashMap<>();
			this.title = title;
		}
		
		public void writeString(String key, String value) {
			map.put(key, new Pair<>(0, value));
		}
		
		public void writeInteger(String key, Integer value) {
			map.put(key, new Pair<>(1, value));
		}
		
		public void writeBoolean(String key, Boolean value) {
			map.put(key, new Pair<>(2, value));
		}
		
		public void writeFloat(String key, Float value) {
			map.put(key, new Pair<>(3, value));
		}
		
		public void writeBlockPos(String key, BlockPos value) {
			map.put(key, new Pair<>(4, value));
		}
		
		public void writeItemStack(String key, ItemStack value) {
			map.put(key, new Pair<>(5, value));
		}
		
		public void writeResourceLocation(String key, ResourceLocation value) {
			map.put(key, new Pair<>(6, value));
		}
		
		public void writeDouble(String key, Double value) {
			map.put(key, new Pair<>(7, value));
		}
		
		public Pair<Integer, Object> get(String key) {
			return map.get(key);
		}
		
		public <T> T get(String key, Class<T> clazz) {
			return clazz.cast(map.get(key).getSecond());
		}
		
		public int getMapSize() {
			return map.size();
		}
		public String getCategory() {
			return title;
		}
		
		public Set<String> getKeySet() {
			return map.keySet();
		}
	}
	public static class DecoderConsumer implements Function<PacketBuffer, PacketData>{

		@Override
		public PacketData apply(PacketBuffer t) {
			
			PacketData pd = new PacketData(t.readString(32767));
			
			int max = t.readInt();
			for(int i = 0; i < max; i++) {
				
				String key = t.readString(32767);
				int id = t.readInt();
				
				if(id == 0) {
					pd.writeString(key, t.readString(32767));
				}else if(id == 1) {
					pd.writeInteger(key, t.readInt());
				}else if(id == 2) {
					pd.writeBoolean(key, t.readBoolean());
				}else if(id == 3) {
					pd.writeFloat(key, t.readFloat());
				}else if(id == 4) {
					pd.writeBlockPos(key, t.readBlockPos());
				}else if(id == 5) {
					pd.writeItemStack(key, t.readItemStack());
				}else if(id == 6) {
					pd.writeResourceLocation(key, t.readResourceLocation());
				}else if(id == 7) {
					pd.writeDouble(key, t.readDouble());
				}
			}
			return pd;
		}
		
	}
	public static class EncoderConsumer implements BiConsumer<PacketData, PacketBuffer>{

		@Override
		public void accept(PacketData t, PacketBuffer u) {
			
			
			u.writeString(t.getCategory());
			u.writeInt(t.getMapSize());
			
			for(String k : t.getKeySet()) {
				u.writeString(k);
				Pair<Integer, Object> v = t.get(k);
				Integer id = v.getFirst();
				Object data = v.getSecond();
				u.writeInt(id);
				
				if(id == 0) {
					u.writeString((String) data);
				}else if(id == 1) {
					u.writeInt((Integer) data);
				}else if(id == 2) {
					u.writeBoolean((Boolean) data);
				}else if(id == 3) {
					u.writeFloat((Float) data);
				}else if(id == 4) {
					u.writeBlockPos((BlockPos) data);
				}else if(id == 5) {
					u.writeItemStack((ItemStack) data);
				}else if(id == 6) {
					u.writeResourceLocation((ResourceLocation) data);
				}else if(id == 7) {
					u.writeDouble((Double) data);
				}
				
			}
			
		}
		
	}
	
	public static class MessageHandler implements BiConsumer<PacketData, Supplier<NetworkEvent.Context>>{

		@Override
		public void accept(PacketData t, Supplier<Context> u) {
			u.get().enqueueWork(new Runnable() {
				
				@Override
				public void run() {
					
					BiConsumer<PacketData, World> x = PacketTargetMethods.PACKET_TARGETS.get(t.title);
					if(x != null) {
						if(u.get().getSender() != null) {
							x.accept(t, u.get().getSender().world);
						}else {
							x.accept(t, null);
						}
						
					}else {
						AssemblyLineMachines.LOGGER.warn("Received packet with no method target: " + t.title + ". Look out for injection possibilities.");
					}
					
					
				}
			});
			u.get().setPacketHandled(true);
			
		}
		
	}
}
