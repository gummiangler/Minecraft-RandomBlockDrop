package io.github.gummiangler.untitled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class untitled implements ModInitializer {
    private static final Random RANDOM = new Random();
    private static final List<Identifier> DROPPABLE_BLOCKS = new ArrayList<>();
    private static final String MESSAGE = "SiMP - Dein MC Netzwerk! SiMP.Honeyteam.net";
    private static ScheduledExecutorService scheduler;
    final boolean[] worldLoaded = {false};

    @Override
    public void onInitialize() {
        // Fülle die Liste mit allen Block-IDs, die keine "air"-Blöcke sind
        for (Identifier id : Registries.BLOCK.getIds()) {
            Block block = Registries.BLOCK.get(id);
            BlockState state = block.getDefaultState();
            // Füge nur Blöcke hinzu, die nicht "air" sind
            if (!state.isAir()) {
                DROPPABLE_BLOCKS.add(id);
            }
        }

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClient) {

                // Bestimme die Anzahl der gedroppten Items des ursprünglichen Blocks
                List<ItemStack> originalDrops = Block.getDroppedStacks(state, (ServerWorld) world, pos, null, player, player.getMainHandStack());
                int dropCount = originalDrops.stream().mapToInt(ItemStack::getCount).sum();

                System.out.println("Original Drops Count: " + dropCount);
                if (originalDrops.isEmpty()) {
                    System.out.println("No original drops, ensuring at least one drop.");
                }

                // Stelle sicher, dass mindestens 1 Item gedroppt wird
                if (dropCount < 1) {
                    dropCount = 1;
                }

                // Wähle einen zufälligen Block aus, der kein "air" ist
                Identifier randomBlockId = DROPPABLE_BLOCKS.get(RANDOM.nextInt(DROPPABLE_BLOCKS.size()));
                Block randomBlock = Registries.BLOCK.get(randomBlockId);

                System.out.println("Random Block Selected: " + randomBlockId);

                boolean dropSuccess = false;
                for (int i = 0; i < dropCount; i++) {
                    ItemStack randomDrop = new ItemStack(randomBlock.asItem());

                    // Überprüfe, ob das Item kein "air" ist und ob der Drop erfolgreich ist
                    if (!randomDrop.isEmpty() && randomDrop.getCount() > 0) {
                        try {
                            Block.dropStack(world, pos, randomDrop);
                            dropSuccess = true;
                        } catch (Exception e) {
                            System.err.println("Failed to drop item: " + randomDrop.getItem());
                        }
                    }

                    if (!dropSuccess) {
                        // Wenn der Drop nicht erfolgreich war, versuche einen alternativen Block
                        ItemStack fallbackItem = new ItemStack(Registries.BLOCK.get(new Identifier("minecraft", "dirt")).asItem());
                        System.out.println("Dropping fallback Item: " + fallbackItem.getItem());
                        Block.dropStack(world, pos, fallbackItem);
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    if (client.world != null && !worldLoaded[0] && client.getResourceManager() != null) {
                        startMessageScheduler();
                        worldLoaded[0] = true;
                    }
                }
        );

    }

    private void startMessageScheduler() {
        if (worldLoaded[0]) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                MinecraftServer server = MinecraftClient.getInstance().getServer();
                if (server != null) {
                    server.getPlayerManager().broadcast(Text.of(MESSAGE), true);
                }
            }, 0, 30, TimeUnit.SECONDS);
        }
    }
}
