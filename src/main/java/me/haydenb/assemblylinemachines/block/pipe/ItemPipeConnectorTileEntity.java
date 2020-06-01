package me.haydenb.assemblylinemachines.block.pipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import me.haydenb.assemblylinemachines.block.pipe.ItemPipeConnectorTileEntity.ItemPipeConnectorContainer;
import me.haydenb.assemblylinemachines.block.pipe.PipeBase.Target;
import me.haydenb.assemblylinemachines.block.pipe.PipeProperties.PipeConnOptions;
import me.haydenb.assemblylinemachines.misc.TileEntityALMMachine;
import me.haydenb.assemblylinemachines.misc.TileEntityALMMachine.*;
import me.haydenb.assemblylinemachines.misc.Utils;
import me.haydenb.assemblylinemachines.misc.Utils.Pair;
import me.haydenb.assemblylinemachines.packets.HashPacketImpl;
import me.haydenb.assemblylinemachines.packets.HashPacketImpl.PacketData;
import me.haydenb.assemblylinemachines.registry.Registry;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class ItemPipeConnectorTileEntity extends TileEntityALMMachine<ItemPipeConnectorContainer>
		implements IPipeConnector, ITickableTileEntity {

	private boolean inputMode = true;
	private boolean outputMode = true;
	private boolean nearestFirst = true;
	private boolean rrMode = false;
	private boolean whitelist = false;
	private int priority = 0;
	
	private int timer = 0;
	
	private final TreeMap<Target, IPipeConnector> targets = new TreeMap<>(new Comparator<Target>() {

		@Override
		public int compare(Target o1, Target o2) {
			return o1.priority.compareTo(o2.priority);
		}
	});
	
	public ItemPipeConnectorTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn, 12, "Item Pipe Connector", Registry.getContainerId("pipe_connector_item"), ItemPipeConnectorContainer.class);
	}

	public ItemPipeConnectorTileEntity() {
		this(Registry.getTileEntity("pipe_connector_item"));
	}

	@Override
	public void updateTargets(PipeBase<?> pb) {
		if(!world.isRemote) {
			targets.clear();
			pb.pathToNearest(world, pos, new ArrayList<>(), pos, 1, targets);
			this.sendUpdates();
		}
		
	}
	
	@Override
	public int getPriority() {
		return priority;
	}
	
	@Override
	public void read(CompoundNBT compound) {
		super.read(compound);
		
		if(compound.contains("assemblylinemachines:input")) {
			inputMode = compound.getBoolean("assemblylinemachines:input");
		}
		if(compound.contains("assemblylinemachines:output")) {
			outputMode = compound.getBoolean("assemblylinemachines:output");
		}
		if(compound.contains("assemblylinemachines:nearest")) {
			nearestFirst = compound.getBoolean("assemblylinemachines:nearest");
		}
		if(compound.contains("assemblylinemachines:whitelist")) {
			whitelist = compound.getBoolean("assemblylinemachines:whitelist");
		}
		if(compound.contains("assemblylinemachines:rr")) {
			rrMode = compound.getBoolean("assemblylinemachines:rr");
		}
		if(compound.contains("assemblylinemachines:priority")) {
			priority = compound.getInt("assemblylinemachines:priority");
		}
	}
	
	@Override
	public CompoundNBT write(CompoundNBT compound) {
		
		compound.putBoolean("assemblylinemachines:input", inputMode);
		compound.putBoolean("assemblylinemachines:output", outputMode);
		compound.putBoolean("assemblylinemachines:nearest", nearestFirst);
		compound.putBoolean("assemblylinemachines:rr", rrMode);
		compound.putBoolean("assemblylinemachines:whitelist", whitelist);
		compound.putInt("assemblylinemachines:priority", priority);
		return super.write(compound);
	}
	
	@Override
	public void tick() {
		if(!world.isRemote) {
			if(outputMode == true) {
				if(timer++ == 40) {
					timer = 0;
					IItemHandler output = connectToOutput();
					if(output != null) {
						int max = 10;
						for(int i = 0; i < output.getSlots(); i++) {
							ItemStack extr = output.extractItem(i, max, false);
							int origSize = extr.getCount();
							if(extr != ItemStack.EMPTY) {
								for(Target t : targets.descendingKeySet()) {
									IPipeConnector ipc = targets.get(t);
									if(ipc != null) {
										extr = ipc.attemptAcceptItem(extr);
										if(extr == ItemStack.EMPTY) {
											break;
										}
									}
								}
							}
							
							if(extr.getCount() < origSize) {
								if(extr != ItemStack.EMPTY) {
									output.insertItem(i, extr, false);
								}
								break;
							}
								
						}
					}
					
				}
			}
		}
	}
	
	@Override
	public ItemStack attemptAcceptItem(ItemStack stack) {
		if(inputMode == false) {
			return stack;
		}
		
		IItemHandler output = connectToOutput();
		
		if(output == null) {
			return stack;
		}
		
		for(int i = 0; i < output.getSlots(); i++) {
			stack = output.insertItem(i, stack, false);
			if(stack == ItemStack.EMPTY) {
				break;
			}
		}
		
		return stack;
		
		
	}
	
	@Override
	public FluidStack attemptAcceptLiquid(FluidStack stack) {
		return stack;
	}
	
	@Override
	public int attemptAcceptPower(int power) {
		return power;
	}
	
	private IItemHandler connectToOutput() {
		
		for(Direction d : Direction.values()) {
			if(getBlockState().get(PipeProperties.DIRECTION_BOOL.get(d)) == PipeConnOptions.CONNECTOR) {
				TileEntity te = world.getTileEntity(pos.offset(d));
				if(te != null) {
					return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, d.getOpposite()).orElse(null);
				}
				
			}
		}
		
		return null;
	}

	public static class ItemPipeConnectorContainer extends ContainerALMBase<ItemPipeConnectorTileEntity>{
		
		private static final Pair<Integer, Integer> PLAYER_INV_POS = new Pair<>(8, 84);
		private static final Pair<Integer, Integer> PLAYER_HOTBAR_POS = new Pair<>(8, 142);
		
		public ItemPipeConnectorContainer(final int windowId, final PlayerInventory playerInventory, final ItemPipeConnectorTileEntity tileEntity) {
			super(Registry.getContainerType("pipe_connector_item"), windowId, 2, tileEntity, playerInventory, PLAYER_INV_POS, PLAYER_HOTBAR_POS);
			for (int row = 0; row < 3; ++row) {
				for (int col = 0; col < 3; ++col) {
					this.addSlot(new FalseSlot(tileEntity, (row * 3) + col, 55 + (18 * col), 21 + (18 * row)));
				}
			}
			
			for(int row = 0; row < 3; ++row) {
				this.addSlot(new Slot(tileEntity, row + 9, 149, 21 + (row * 18)));
			}
			
		}
		
		public ItemPipeConnectorContainer(final int windowId, final PlayerInventory playerInventory, final PacketBuffer data) {
			this(windowId, playerInventory, Utils.getTileEntity(playerInventory, data, ItemPipeConnectorTileEntity.class));
		}
		
		public static class FalseSlot extends Slot{

			public int slotindex;
			public FalseSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
				super(inventoryIn, index, xPosition, yPosition);
				this.slotindex = index;
			}
			
			
			@Override
			public int getSlotStackLimit() {
				return 1;
			}
			
			
			@Override
			public boolean isItemValid(ItemStack stack) {
				if(!stack.hasTag() && !stack.isDamaged()) {
					return true;
				}
				return false;
			}
			
		}
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ItemPipeConnectorScreen extends ScreenALMBase<ItemPipeConnectorContainer>{
		
		ItemPipeConnectorTileEntity tsfm;
		ItemPipeConnectorContainer container;
		private final HashMap<String, Pair<ItemPipeButton, SupplierWrapper>> b;
		
		public ItemPipeConnectorScreen(ItemPipeConnectorContainer screenContainer, PlayerInventory inv,
				ITextComponent titleIn) {
			super(screenContainer, inv, titleIn, new Pair<>(175, 165), new Pair<>(11, 6), new Pair<>(11, 73), "pipe_connector_item", false);
			tsfm = screenContainer.tileEntity;
			container = screenContainer;
			b = new HashMap<>();
		}
		
		@Override
		protected void init() {
			super.init();
			this.renderTitles = false;
			int x = (this.width - this.xSize) / 2;
			int y = (this.height - this.ySize) / 2;
			b.put("input", new Pair<>(new ItemPipeButton(x+32, y+20, 177, 1, null, (button) -> {
				
				sendPipeUpdatePacket(tsfm.pos, "input");
			}), new SupplierWrapper("Input Enabled", "Input Disabled", () -> tsfm.inputMode)));
			b.put("output", new Pair<>(new ItemPipeButton(x+43, y+20, 177, 11, null, (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "output");
			}), new SupplierWrapper("Output Enabled", "Output Disabled", () -> tsfm.outputMode)));
			b.put("target", new Pair<>(new ItemPipeButton(x+32, y+31, 177, 21, null, (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "nearest");
			}), new SupplierWrapper("Nearest First", "Farthest First", () -> tsfm.nearestFirst)));
			b.put("rr", new Pair<>(new ItemPipeButton(x+43, y+31, 177, 31, null, (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "roundrobin");
			}), new SupplierWrapper("Round-Robin Enabled", "Round-Robin Disabled", () -> tsfm.rrMode)));
			b.put("filter", new Pair<>(new ItemPipeButton(x+43, y+65, 177, 41, null, (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "whitelist");
			}), new SupplierWrapper("Whitelist", "Blacklist", () -> tsfm.whitelist)));
			b.put("priorityup", new Pair<>(new ItemPipeButton(x+111, y+32, "Priority Increase", (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "priorityup");
				
			}),null));
			b.put("prioritydown", new Pair<>(new ItemPipeButton(x+111, y+43, "Priority Decrease", (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "prioritydown");
				
			}),null));
			b.put("priorityzero", new Pair<>(new ItemPipeButton(x+111, y+54, "Priority Reset", (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "priorityzero");
			}),null));
			b.put("refresh", new Pair<>(new ItemPipeButton(x+158, y+10, "Refresh Pipes", (button) -> {
				sendPipeUpdatePacket(tsfm.pos, "refresh");
			}),null));
			for(Pair<ItemPipeButton, SupplierWrapper> bb : b.values()) {
				this.addButton(bb.x);
			}
			
			
		}
		
		@Override
		protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
			super.drawGuiContainerForegroundLayer(mouseX, mouseY);
			for(Pair<ItemPipeButton, SupplierWrapper> bb : b.values()) {
				if(mouseX >= bb.x.x && mouseX <= bb.x.x + 8 && mouseY >= bb.x.y && mouseY <= bb.x.y + 8) {
					int x = (this.width - this.xSize) / 2;
					int y = (this.height - this.ySize) / 2;
					if(bb.y != null) {
						this.renderTooltip(bb.y.getTextFromSupplier(), mouseX - x, mouseY - y);
					}else {
						this.renderTooltip(bb.x.getMessage(), mouseX - x, mouseY - y);
					}
					
					break;
				}
			}
			
			this.font.drawString(this.title.getFormattedText(), 11, 6, 4210752);
		}
		
		@Override
		protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
			super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
			int x = (this.width - this.xSize) / 2;
			int y = (this.height - this.ySize) / 2;
			for(Pair<ItemPipeButton, SupplierWrapper> bb : b.values()) {
				
				if(bb.y != null && bb.y.supplier.get()) {
					super.blit(bb.x.x, bb.x.y, bb.x.blitx, bb.x.blity, 8, 8);
				}
				
			}
			
			this.drawCenteredString(this.font, this.tsfm.priority + "", x + 133, y + 43, 0xffffff);
		}
		
		
	}
	
	public static class ItemPipeButton extends Button{
		private final int blitx;
		private final int blity;
		public ItemPipeButton(int widthIn, int heightIn, int blitx, int blity, String text, IPressable onPress) {
			super(widthIn, heightIn, 8, 8, text, onPress);
			this.blitx = blitx;
			this.blity = blity;
		}
		
		public ItemPipeButton(int widthIn, int heightIn, String text, IPressable onPress) {
			this(widthIn, heightIn, 0, 0, text, onPress);
		}
		
		@Override
		public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		}
		
		
		
	}
	
	private static class SupplierWrapper {
		
		private final String trueText;
		private final String falseText;
		private final Supplier<Boolean> supplier;
		
		public SupplierWrapper(String trueText, String falseText, Supplier<Boolean> supplier) {
			this.trueText = trueText;
			this.falseText = falseText;
			this.supplier = supplier;
		
		}
		
		public String getTextFromSupplier() {
			if(supplier.get()) {
				return trueText;
			}else {
				return falseText;
			}
		}
		
	}
	
	public static void sendPipeUpdatePacket(BlockPos pos, String button) {
		
		PacketData pd = new PacketData("item_pipe_gui");
		pd.writeBlockPos("location", pos);
		pd.writeString("button", button);
		
		HashPacketImpl.INSTANCE.sendToServer(pd);
	}
	
	public static void updateDataFromPacket(PacketData pd, World world) {
		
		if(pd.getCategory().equals("item_pipe_gui")){
			TileEntity te = world.getTileEntity(pd.get("location", BlockPos.class));
			if(te != null && te instanceof ItemPipeConnectorTileEntity) {
				ItemPipeConnectorTileEntity ipcte = (ItemPipeConnectorTileEntity) te;
				
				String b = pd.get("button", String.class);
				
				if(b.equals("input")) {
					
					ipcte.inputMode = !ipcte.inputMode;
					
				}else if(b.equals("output")) {
					ipcte.outputMode = !ipcte.outputMode;
				}else if(b.equals("nearest")) {
					if(!ipcte.rrMode) {
						ipcte.nearestFirst = !ipcte.nearestFirst;
					}
				}else if(b.equals("roundrobin")) {
					
					ipcte.rrMode = !ipcte.rrMode;
					
					if(ipcte.rrMode == true) {
						ipcte.nearestFirst = true;
					}
				}else if(b.equals("whitelist")) {
					ipcte.whitelist = !ipcte.whitelist;
					
				}else if(b.equals("priorityup")) {
					ipcte.priority++;
				}else if(b.equals("prioritydown")) {
					ipcte.priority--;
				}else if(b.equals("priorityzero")) {
					ipcte.priority = 0;
				}else if(b.equals("refresh")) {
					ipcte.updateTargets((PipeBase<?>) Registry.getBlock("item_pipe"));
				}
				
				ipcte.sendUpdates();
			}
		}
	}
}
