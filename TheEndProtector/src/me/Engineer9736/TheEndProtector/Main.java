package me.Engineer9736.TheEndProtector;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {
	
	private ArrayList<Location> endCrystalLocations;
	
	private World theEnd;
	
	/*private enum TheEndStage {
		PEACEFUL,
		FIGHTACTIVE,
		DRAGONKILLED,
		NOPLAYERSLEFT
	};
	
	/*kill @e[type=minecraft:ender_dragon]
	  
	*/
	
	//private TheEndStage CurrentStage;
	
	@Override
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
		theEnd = Bukkit.getServer().getWorld("world_the_end");
		
		// The allowed End Crystal placement locations. The Y axis is ignored as the Exit Portal is
		// generated at the height of the terrain.
		endCrystalLocations = new ArrayList<Location>();
		for (int i = -1; i <= 1; i++) {
			endCrystalLocations.add(new Location(theEnd, i, 0, 3));
			endCrystalLocations.add(new Location(theEnd, i, 0, -3));
			
			endCrystalLocations.add(new Location(theEnd, 3, 0, i));
			endCrystalLocations.add(new Location(theEnd, -3, 0, i));
		}
		endCrystalLocations.add(new Location(theEnd, -2, 0, 2));
		endCrystalLocations.add(new Location(theEnd, -2, 0, -2));
		endCrystalLocations.add(new Location(theEnd, 2, 0, -2));
		endCrystalLocations.add(new Location(theEnd, 2, 0, 2));
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("removedragon")) {
			removeDragon();
		}
		
		if (label.equalsIgnoreCase("killdragon")) {
			killDragon();
		}
		
		if (label.equalsIgnoreCase("spawndragon")) {
			Location l = new Location(theEnd, 0, 80, 0);
			EnderDragon dragon = theEnd.spawn(l, EnderDragon.class);
			dragon.setAI(true);
		}
		
		return true;
	}
	
	@EventHandler
	public void onPlace(BlockBreakEvent event) {
		Player p = event.getPlayer();
		
		event.setCancelled(shouldBlockEventBeCancelled(p, event.getBlock()));
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event) {
		Player p = event.getPlayer();

		event.setCancelled(shouldBlockEventBeCancelled(p, event.getBlock()));
	}
	
	// If End Crystals are placed on obsidian, then remove it again. Players can only place End Crystals on bedrock
	// (which is what the Exit Portal is made of)
	// Taken from https://www.spigotmc.org/threads/ender-crystal-place-event.132757/
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		
		// If the event is not regarding a right mouseclick on a block, then do nothing.
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
	    	return;
	    }
	    
	    // If the event is not regarding a click on an obsidian block, then do nothing.
	    if (event.getClickedBlock().getType() != Material.OBSIDIAN) {
	    	return;
	    }
	    
	    // If the event is not regarding placing an End Crystal, then do nothing.
        if (event.getMaterial() != Material.END_CRYSTAL) {
        	return;
        }
        
        // 
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
            	
            	// Get all entities nearby the player.
                List<Entity> entities = event.getPlayer().getNearbyEntities(4, 4, 4);

                // Loop through these entities.
                for (Entity entity : entities) {
                	
                	// If the entity type is not an End Crystal then continue to the next iteration.
                    if (entity.getType() != EntityType.ENDER_CRYSTAL) {
                    	continue;
                    }
                    
                    EnderCrystal crystal = (EnderCrystal) entity;
                    Block belowCrystal = crystal.getLocation().getBlock().getRelative(BlockFace.DOWN);

                    if (event.getClickedBlock().equals(belowCrystal)) { // Here is your EnderCrystal entity
                    	belowCrystal.breakNaturally();
                    	entity.remove();
                        break;
                    }
                }
            }
        });
	}
	
	// When the dragon dies, rollback 300 radius from 0,0
	@EventHandler
	public void onEnderDragonDeath(EntityDeathEvent e){
	     if(e.getEntity() instanceof EnderDragon){
	    	 getLogger().info("Dragon has died");
	    	 
	    	 CoreProtectAPI api = getCoreProtect();
	    	 if (api == null) {
	    		 getLogger().info("Could not connect to CoreProtect");
	    		 return;
	    	 }
	    	 
	    	 /*api.performRollback(int time,
	    			 List<String> restrict_users,
	    			 List<String> exclude_users,
	    			 List<Object> restrict_blocks,
	    			 List<Object> exclude_blocks,
	    			 List<Integer> action_list,
	    			 int radius,
	    			 Location radius_location);*/
	    }
	}
	
	private boolean shouldBlockEventBeCancelled(Player p, Block block) {
		// If the BlockEvent is not regarding The End, then do nothing.
		if (block.getWorld().getEnvironment() != Environment.THE_END) {
			getLogger().info("BlockEvent -> shouldBlockEventBeCancelled: Environment is not the end.");
			return false;
		}
		
		// If the BlockEvent was outside the main island, then do not block it.
		if (!locationIsMainIsland(block.getLocation())) {
			getLogger().info("BlockEvent -> shouldBlockEventBeCancelled: Event was not on the main island so not cancelled.");
			return false;
		}
		
		// If the dragon is alive, then do not block BlockEvents.
		if (dragonIsAlive()) {
			getLogger().info("BlockEvent -> shouldBlockEventBeCancelled: Dragon is alive, so not cancelled.");
			return false;
		}
		
		getLogger().info("BlockEvent -> shouldBlockEventBeCancelled: BlockEvent cancelled.");
		p.sendMessage(ChatColor.RED + "As long as the Ender Dragon is not alive, you can only place End Crystals on the Exit Portal to spawn the Ender Dragon.");
		return true;
	}
	
	private boolean locationIsEndCrystalLocation(Location l) {
		for (Location crystalLocation : endCrystalLocations) {
			if (crystalLocation.getX() == l.getX() && crystalLocation.getZ() == l.getZ()) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean locationIsMainIsland(Location l) {
		return l.getX() < 150 && l.getX() > -150 && l.getZ() < 150 && l.getZ() > -150;
	}
	
	private boolean dragonIsAlive(){
        List<LivingEntity> Vivos = theEnd.getLivingEntities();
        for (LivingEntity j : Vivos) {
            if (j instanceof EnderDragon) {
            	return true;
            }
           
        }
        
        return false;
    }
	
	private void removeDragon(){
        List<LivingEntity> Vivos = theEnd.getLivingEntities();
        for (LivingEntity j : Vivos) {
            if (j instanceof EnderDragon) {
            	j.remove();
            };
           
        }
    }
	
	private void killDragon(){
        List<LivingEntity> Vivos = theEnd.getLivingEntities();
        for (LivingEntity j : Vivos) {
            if (j instanceof EnderDragon) {
            	j.setHealth(0);
            };
           
        }
    }
	
	private CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");
     
        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 6) {
            return null;
        }

        return CoreProtect;
}
}
