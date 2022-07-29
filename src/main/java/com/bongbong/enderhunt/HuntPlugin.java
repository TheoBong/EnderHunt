package com.bongbong.enderhunt;

import com.google.gson.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

public final class HuntPlugin extends JavaPlugin implements Listener {
    EggState eggState;
    Location eggPos;
    UUID playerHolder;

    Location origin;

    ArrayList<PotionEffect> buffEffects = new ArrayList<>();

    @Override
    public void onEnable() {
        origin = new Location(Bukkit.getWorlds().get(2), 0, Bukkit.getWorlds().get(2).getHighestBlockYAt(0, 0) + 1, 0);

        buffEffects.add(new PotionEffect(PotionEffectType.REGENERATION, 600, 0, false, false, false));
        buffEffects.add(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 0, false, false, false));

        getServer().getPluginManager().registerEvents(this,this);

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {
            switch (eggState){
                case INVENTORY:
                    Player player = Bukkit.getPlayer(playerHolder);
                    if (player != null){
                        eggPos = player.getLocation();
                        World world = player.getWorld();
                        world.spawnParticle(Particle.PORTAL, eggPos.getX(), eggPos.getY() + 0.5, eggPos.getZ(), 3, 0.3,0.6, 0.3, 0.03);

                        player.addPotionEffects(buffEffects);
                    }
                    break;
                case DROPPED_ITEM:
                    for (World world : Bukkit.getWorlds()){
                        for (Entity entity : world.getEntities()){
                            if (entity instanceof Item){
                                if (((Item)entity).getItemStack().getType().equals(Material.DRAGON_EGG)){
                                    if (entity.getLocation().getY() < -1){
                                        entity.remove();
                                        origin.getBlock().setType(Material.DRAGON_EGG);
                                        eggPos = origin;
                                        eggState = EggState.BLOCK;
                                        break;
                                    }
                                    eggPos = entity.getLocation();
                                    world.spawnParticle(Particle.REVERSE_PORTAL, eggPos.getX(), eggPos.getY() + 0.25, eggPos.getZ(), 2, 0.1, 0.1, 0.1, 0.1);
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ItemStack item : player.getInventory()){
                    if (item != null) {
                        if (item.getType().equals(Material.COMPASS)) {
                            CompassMeta meta = (CompassMeta) item.getItemMeta();

                            if (eggPos != null) {
                                meta.setLodestone(eggPos);
                                meta.setLodestoneTracked(false);
                                item.setItemMeta(meta);
                            }
                        }
                    }
                }
            }
        }, 0, 1);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event){
        Item item = event.getEntity();
        if (item.getItemStack().getType().equals(Material.DRAGON_EGG)){
            eggState = EggState.DROPPED_ITEM;
            item.setInvulnerable(true);
            playerHolder = null;
        }
    }

    @EventHandler
    public void onItemPickUp(EntityPickupItemEvent event){
        if (event.getItem().getItemStack().getType().equals(Material.DRAGON_EGG)){
            if (!(event.getEntity() instanceof Player)){
                event.setCancelled(true);
            } else {
                eggState = EggState.INVENTORY;
                playerHolder = event.getEntity().getUniqueId();
            }
        }
    }

    @EventHandler
    public void onHopperPickUp(InventoryPickupItemEvent event){
        if (event.getItem().getItemStack().getType().equals(Material.DRAGON_EGG)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if (event.getCurrentItem() != null) {
            if ((event.getCurrentItem().getType().equals(Material.DRAGON_EGG) || event.getCursor().getType().equals(Material.DRAGON_EGG)) && ((!event.getClickedInventory().getType().equals(InventoryType.PLAYER)) || event.getClick().isShiftClick())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event){
        if (event.getOldCursor().getType().equals(Material.DRAGON_EGG)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Block block = event.getBlock();
        if (block.getType().equals(Material.DRAGON_EGG)){
            playerHolder = null;
            eggPos = event.getBlockPlaced().getLocation();
            eggState = EggState.BLOCK;
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event){
        if (event.getEntity().getItemStack().getType().equals(Material.DRAGON_EGG)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEggTeleport(BlockFromToEvent event){
        if (event.getBlock().getType().equals(Material.DRAGON_EGG)){
            eggPos = event.getToBlock().getLocation();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if(event.getItem() != null) {
            if (!player.isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getItem().getType().equals(Material.DRAGON_EGG)){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if (event.getPlayer().getUniqueId().equals(playerHolder)){
            Player player = event.getPlayer();
            player.getInventory().remove(Material.DRAGON_EGG);
            eggPos.getWorld().dropItem(eggPos, new ItemStack(Material.DRAGON_EGG, 1));
            eggState = EggState.DROPPED_ITEM;
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event){
        if (event.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.DRAGON_EGG) || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.DRAGON_EGG)){
            if (event.getPlayer().getUniqueId().equals(playerHolder)){
                event.setCancelled(true);
            }
        }
    }
}

enum EggState {
    INVENTORY,
    DROPPED_ITEM,
    BLOCK,
}