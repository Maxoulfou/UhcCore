package com.gmail.val59000mc.game;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.threads.WorldBorderThread;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;

public class UhcWorldBorder {
	private boolean moving;
	private int startSize;
	private int endSize;
	private long timeToShrink;
	private long timeBeforeShrink;
	
	public UhcWorldBorder(){
		FileConfiguration cfg = UhcCore.getPlugin().getConfig();
		moving = cfg.getBoolean("border.moving",true);
		startSize = cfg.getInt("border.start-size",1000);
		endSize = cfg.getInt("border.end-size",0);
		timeToShrink = cfg.getLong("border.time-to-shrink",3600);
		timeBeforeShrink = cfg.getLong("border.time-before-shrink",0);
		
		Bukkit.getLogger().info("[UhcCore] Border start size is "+startSize);
		Bukkit.getLogger().info("[UhcCore] Border end size is "+startSize);
		Bukkit.getLogger().info("[UhcCore] Border moves : "+moving);
		Bukkit.getLogger().info("[UhcCore] Border timeBeforeEnd : "+timeToShrink);
	}

	public int getStartSize() {
		return startSize;
	}

	public void setUpBukkitBorder(){
		Bukkit.getScheduler().runTaskLater(UhcCore.getPlugin(), new Runnable(){

			@Override
			public void run() {
				World overworld = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getOverworldUuid());
				setBukkitWorldBorderSize(overworld,0,0,2*startSize);
				
				World nether = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getNetherUuid());
				if (nether != null) {
					setBukkitWorldBorderSize(nether, 0, 0, startSize);
				}
				
				World end = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getTheEndUuid());
				if (end != null) {
					setBukkitWorldBorderSize(end, 0, 0, 2*startSize);
				}
			}
			
		}, 200);
	}
	
	public void setBukkitWorldBorderSize(World world, int centerX, int centerZ, double edgeSize){
		Validate.notNull(world);

		WorldBorder worldborder = world.getWorldBorder();
		worldborder.setCenter(centerX,centerZ);
		worldborder.setSize(edgeSize);
	}

	public double getCurrentSize(){
		World overworld = Bukkit.getWorld(GameManager.getGameManager().getConfiguration().getOverworldUuid());
		return overworld.getWorldBorder().getSize()/2;
	}

	public void startBorderThread() {
		if(moving){
			Bukkit.getScheduler().runTaskAsynchronously(UhcCore.getPlugin(), new WorldBorderThread(timeBeforeShrink, endSize, timeToShrink));
		}
	}

}
