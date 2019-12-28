package com.gmail.val59000mc.customitems;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.configuration.YamlFile;
import com.gmail.val59000mc.exceptions.ParseException;
import com.gmail.val59000mc.languages.Lang;
import com.gmail.val59000mc.utils.FileUtils;
import com.gmail.val59000mc.utils.JsonItemUtils;
import com.gmail.val59000mc.utils.UniversalMaterial;
import com.gmail.val59000mc.utils.VersionUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class CraftsManager {
	
	private static List<Craft> crafts;
	
	public static synchronized List<Craft> getCrafts(){
		return crafts;
	}

	public static boolean isAtLeastOneCraft() {
		return (getCrafts() != null && getCrafts().size() >= 1);
	}

	public static void loadBannedCrafts(){
		Bukkit.getLogger().info("[UhcCore] Loading banned crafts list");
		
		FileConfiguration cfg = UhcCore.getPlugin().getConfig();
		Set<ItemStack> bannedItems = new HashSet<>();
		for(String itemLine : cfg.getStringList("customize-game-behavior.ban-items-crafts")){

			if (itemLine.startsWith("{") && itemLine.endsWith("}")){
				try {
					bannedItems.add(JsonItemUtils.getItemFromJson(itemLine));
				}catch (ParseException ex){
					Bukkit.getLogger().warning("[UhcCore] Failed to register "+itemLine+" banned craft");
					ex.printStackTrace();
				}
			}else{
				Bukkit.getLogger().warning("[UhcCore] Failed to register "+itemLine+" banned craft, it's using invalid formatting!");
			}
		}

		for (ItemStack item : bannedItems){
			try{
				Iterator<Recipe> recipes = Bukkit.getServer().recipeIterator();

				while (recipes.hasNext()){
					Recipe recipe = recipes.next();
					if (recipe.getResult().isSimilar(item)){
						recipes.remove();
					}
				}
				Bukkit.getLogger().info("[UhcCore] Banned item "+JsonItemUtils.getItemJson(item)+" registered");
			}catch(UnsupportedOperationException e){
				Bukkit.getLogger().warning("[UhcCore] Failed to register "+JsonItemUtils.getItemJson(item)+" banned craft");
				e.printStackTrace();
			}
		}
		
	}

	public static void loadCrafts(){
		Bukkit.getLogger().info("[UhcCore] Loading custom crafts");
		crafts = Collections.synchronizedList(new ArrayList<>());
		FileConfiguration cfg = UhcCore.getPlugin().getConfig();

		// loading golden heads craft if enabled
		if (cfg.getBoolean("customize-game-behavior.enable-golden-heads", false)){
			Bukkit.getLogger().info("[UhcCore] Loading custom craft for golden heads");
			registerGoldenHeadCraft();
		}

		ConfigurationSection customCraftSection = cfg.getConfigurationSection("customize-game-behavior.add-custom-crafts");
		if (customCraftSection == null){
			Bukkit.getLogger().info("[UhcCore] Done loading custom crafts");
			return;
		}

		Set<String> craftsKeys = customCraftSection.getKeys(false);
		for(String name : craftsKeys){
			ConfigurationSection section = cfg.getConfigurationSection("customize-game-behavior.add-custom-crafts."+name);
			if (section == null){
				Bukkit.getLogger().severe("[UhcCore] customize-game-behavior.add-custom-crafts."+name + " section does not exist!");
				continue;
			}
			
			List<ItemStack> recipe = new ArrayList<>();
			ItemStack craftItem;
			int limit;
			boolean defaultName;
			
			try{
				Bukkit.getLogger().info("[UhcCore] Loading custom craft "+name);
				
				// Recipe
				String[] lines = new String[3];
				lines[0] = section.getString("1", "");
				lines[1] = section.getString("2", "");
				lines[2] = section.getString("3", "");
				
				for(int i=0 ; i<3; i++){
					String[] itemsInLine = lines[i].split(" ");
					if(itemsInLine.length != 3)
						throw new IllegalArgumentException("Each line should be formatted like {item} {item} {item}");
					for(int j=0 ; j<3 ;j++){
						if (itemsInLine[j].startsWith("{") && itemsInLine[j].endsWith("}")){
							recipe.add(JsonItemUtils.getItemFromJson(itemsInLine[j]));
						}else{
							throw new IllegalArgumentException("The craft result must be formatted according to the json item format (Use /iteminfo).");
						}
					}
				}
				
				// Craft
				String craftString = section.getString("craft","");

				if (craftString.startsWith("{") && craftString.endsWith("}")){
					craftItem = JsonItemUtils.getItemFromJson(craftString);
				}else {
					throw new IllegalArgumentException("The craft result must be formatted according to the json item format (Use /iteminfo).");
				}
				
				// Limit
				limit = section.getInt("limit",-1);
				defaultName = section.getBoolean("default-name", false);
				Craft craft = new Craft(name, recipe, craftItem, limit, defaultName);
				getCrafts().add(craft);
			}catch(IllegalArgumentException | ParseException e){
				//ignore craft if bad formatting
				Bukkit.getLogger().warning("[UhcCore] Failed to register "+name+" custom craft : syntax error");
				Bukkit.getLogger().warning(e.getMessage());
			}
			
		}
	}

	public static void saveCraft(Craft craft){
		YamlFile cfg = FileUtils.saveResourceIfNotAvailable("config.yml");
		List<ItemStack> recipe = craft.getRecipe();

		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".1",
				JsonItemUtils.getItemJson(recipe.get(0)) + " " +
				JsonItemUtils.getItemJson(recipe.get(1)) + " " +
				JsonItemUtils.getItemJson(recipe.get(2))
		);

		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".2",
				JsonItemUtils.getItemJson(recipe.get(3)) + " " +
				JsonItemUtils.getItemJson(recipe.get(4)) + " " +
				JsonItemUtils.getItemJson(recipe.get(5))
		);

		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".3",
				JsonItemUtils.getItemJson(recipe.get(6)) + " " +
				JsonItemUtils.getItemJson(recipe.get(7)) + " " +
				JsonItemUtils.getItemJson(recipe.get(8))
		);

		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".craft",
				JsonItemUtils.getItemJson(craft.getCraft())
		);

		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".default-name",
				!craft.getCraft().hasItemMeta() && craft.getCraft().getItemMeta().hasDisplayName()
		);

		// limit
		cfg.set(
				"customize-game-behavior.add-custom-crafts." + craft.getName() + ".limit",
				craft.getLimit()
		);

		try {
			cfg.saveWithComments();
		}catch (IOException ex){
			ex.printStackTrace();
		}
	}

	@Nullable
	public static Craft getCraft(ItemStack result) {
		if(result.hasItemMeta() && result.getItemMeta().hasDisplayName()){
			String displayName = result.getItemMeta().getDisplayName();
			for(Craft craft : getCrafts()){
				if(displayName.equals(craft.getCraft().getItemMeta().getDisplayName())){
					return craft;
				}
			}
		}
		return null;
	}

	@Nullable
	public static Craft getCraftByName(String craftName) {
		for(Craft craft : getCrafts()){
			if(craft.getName().equals(craftName)){
				return craft;
			}
		}
		return null;
	}

	@Nullable
	public static Craft getCraftByDisplayName(String craftName){
		for(Craft craft : getCrafts()){
			if(craft.getDisplayItem().getItemMeta().getDisplayName().equals(craftName)){
				return craft;
			}
		}
		return null;
	}
	
	public static void openCraftBookInventory(Player player){
		Validate.notNull(player);

		int maxSlots = 6*9;
		Inventory inv = Bukkit.createInventory(null, maxSlots, ChatColor.GREEN+Lang.DISPLAY_MESSAGE_PREFIX+" "+ChatColor.DARK_GREEN+Lang.ITEMS_CRAFT_BOOK_INVENTORY);
		int slot = 0;
		for(Craft craft : getCrafts()){
			if(slot < maxSlots){
				inv.setItem(slot, craft.getDisplayItem());
				slot++;
			}
		}
		
		player.openInventory(inv);
	}

	public static boolean isCraftItem(ItemStack item) {
		if(item == null || item.getType().equals(Material.AIR)) {
			return false;
		}

		if(item.hasItemMeta() && item.getItemMeta().hasDisplayName()){
			String name = item.getItemMeta().getDisplayName();

			for(Craft craft : getCrafts()){
				 if(name.equals(craft.getDisplayItem().getItemMeta().getDisplayName())
				   && item.getType().equals(craft.getCraft().getType()))
					return true;
			}
		}
		return false;
	}

	public static boolean isCraftBookBackItem(ItemStack item) {
		if(item == null || item.getType().equals(Material.AIR))
			return false;

		if(item.getType().equals(UniversalMaterial.PUFFERFISH.getType()) && item.getItemMeta().getDisplayName().equals(ChatColor.GRAY+Lang.ITEMS_CRAFT_BOOK_BACK))
			return true;
		return false;
	}

	public static void openCraftInventory(Player player, Craft craft) {
		int maxSlots = 6*9;
		Inventory inv = Bukkit.createInventory(null, maxSlots, ChatColor.GREEN+Lang.DISPLAY_MESSAGE_PREFIX+" "+ChatColor.DARK_GREEN+Lang.ITEMS_CRAFT_BOOK_INVENTORY);

		for(int i = 0 ; i < maxSlots-9 ; i++){
			inv.setItem(i, UniversalMaterial.BLACK_STAINED_GLASS_PANE.getStack());
		}
		
		for(int i = maxSlots-9 ; i < maxSlots ; i++){
			inv.setItem(i, UniversalMaterial.WHITE_STAINED_GLASS_PANE.getStack());
		}
		
		// Recipe
		inv.setItem(11, craft.getRecipe().get(0));
		inv.setItem(12, craft.getRecipe().get(1));
		inv.setItem(13, craft.getRecipe().get(2));
		inv.setItem(20, craft.getRecipe().get(3));
		inv.setItem(21, craft.getRecipe().get(4));
		inv.setItem(22, craft.getRecipe().get(5));
		inv.setItem(29, craft.getRecipe().get(6));
		inv.setItem(30, craft.getRecipe().get(7));
		inv.setItem(31, craft.getRecipe().get(8));
		
		// Craft
		inv.setItem(24, craft.getCraft());		

		// Back
		ItemStack back = UniversalMaterial.PUFFERFISH.getStack();
		ItemMeta im = back.getItemMeta();
		im.setDisplayName(ChatColor.GRAY+Lang.ITEMS_CRAFT_BOOK_BACK);
		back.setItemMeta(im);
		inv.setItem(49, back);
		
		player.openInventory(inv);
		
	}

	@SuppressWarnings("deprecation")
	private static void registerGoldenHeadCraft(){
		ItemStack goldenHead = UhcItems.createGoldenHead();
		ShapedRecipe headRecipe = VersionUtils.getVersionUtils().createShapedRecipe(goldenHead, "golden_head");

		headRecipe.shape("GGG", "GHG", "GGG");

		Material material = UniversalMaterial.PLAYER_HEAD.getType();
		MaterialData data = UniversalMaterial.PLAYER_HEAD.getStack().getData();

		headRecipe.setIngredient('G', Material.GOLD_INGOT);

		if (data != null && data.getItemType() == material) {
			headRecipe.setIngredient('H', data);
		}else {
			headRecipe.setIngredient('H', material);
		}

		Bukkit.getServer().addRecipe(headRecipe);
	}

}