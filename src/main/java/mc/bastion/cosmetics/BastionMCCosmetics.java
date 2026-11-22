package mc.bastion.cosmetics;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.URL;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public final class BastionMCCosmetics extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final Map<UUID, View> views = new HashMap<>();
    private final Map<UUID, Balloon> balloons = new HashMap<>();
    private static final class Balloon {
        final Chicken leashAnchor;
        final ItemDisplay head;
        Balloon(Chicken anchor, ItemDisplay display) { this.leashAnchor=anchor; this.head=display; }
    }
    private File usersFile;
    private FileConfiguration users;
    private long ticks = 0;
    private NamespacedKey cosmeticHatKey;
    private static final String[] CATEGORIES = {"auras", "balloons", "hats"};
    private static final Pattern RGB = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final class View {
        final Inventory inventory;
        final String category;
        View(Inventory inventory, String category) {this.inventory = inventory; this.category = category;}
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        cosmeticHatKey = new NamespacedKey(this, "cosmetic_hat");
        usersFile = new File(getDataFolder(), "players.yml");
        users = YamlConfiguration.loadConfiguration(usersFile);
        Objects.requireNonNull(getCommand("cosmeticos")).setExecutor(this);
        Objects.requireNonNull(getCommand("cosmeticos")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("gemas")).setExecutor(this);
        Objects.requireNonNull(getCommand("gemas")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tickCosmetics, 10L, 3L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Reconstruct visual effects and reapply an owned cosmetic only when safe.
            Bukkit.getScheduler().runTaskLater(this, () -> resumeVisuals(player), 20L);
        }
    }
    @Override public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBalloon(player.getUniqueId());
            restoreHat(player, false);
        }
        balloons.values().forEach(this::safeRemove);
        balloons.clear();
        saveUsers();
    }

    private void saveUsers() {
        try { users.save(usersFile); }
        catch (IOException ex) { getLogger().severe("No se pudo guardar players.yml: " + ex.getMessage()); }
    }
    private String path(Player p) { return "players." + p.getUniqueId() + "."; }
    private String path(UUID id) { return "players." + id + "."; }
    private void touch(Player p) {users.set(path(p) + "last-name", p.getName());}
    private boolean allowed(Player p) {
        for (String world : getConfig().getStringList("settings.allowed-worlds")) {
            if (world.equalsIgnoreCase(p.getWorld().getName())) return true;
        }
        return false;
    }
    private String c(String string) {
        if (string == null) return "";
        Matcher m = RGB.matcher(string);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String h = m.group(1);
            StringBuilder mc = new StringBuilder("§x");
            for (char ch : h.toCharArray()) mc.append('§').append(ch);
            m.appendReplacement(out, Matcher.quoteReplacement(mc.toString()));
        }
        m.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }
    private String msg(String key) {return getConfig().getString("messages."+key, key);}
    private void tell(Player p, String key, String... replacements) {
        String message = msg(key);
        for (int i=0;i+1<replacements.length;i+=2) message = message.replace(replacements[i], replacements[i+1]);
        p.sendMessage(c(msg("prefix") + message));
    }
    private Material material(String path, Material fallback) {
        Material mat = Material.matchMaterial(getConfig().getString(path, fallback.name()));
        return mat == null || !mat.isItem() ? fallback : mat;
    }
    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(c(name));
            meta.setLore(lore.stream().map(this::c).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }
    private int safeSlot(int requested, int inventorySize) {return requested>=0 && requested<inventorySize ? requested : 0;}
    private String categoryName(String cat) {return switch (cat) {case "auras" -> "auras"; case "balloons" -> "globos"; default -> "sombreros";};}
    private ConfigurationSection cosmetics(String cat) {return getConfig().getConfigurationSection("cosmetics."+cat);}
    private int balance(Player player) {
        if (getConfig().getString("settings.gems-source", "internal").equalsIgnoreCase("scoreboard")) {
            Objective obj = gemObjective();
            return obj == null ? 0 : Math.max(0, obj.getScore(player.getName()).getScore());
        }
        return Math.max(0, users.getInt(path(player)+"gems", 0));
    }
    private Objective gemObjective() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        return board.getObjective(getConfig().getString("settings.scoreboard-objective", "gemas"));
    }
    private boolean setBalance(Player p, int value) {
        if (getConfig().getString("settings.gems-source", "internal").equalsIgnoreCase("scoreboard")) {
            Objective obj = gemObjective();
            if (obj == null) return false;
            obj.getScore(p.getName()).setScore(value);
        } else {
            users.set(path(p)+"gems", value);
            touch(p);
            saveUsers();
        }
        return true;
    }
    private boolean isFree(Player p) {return p.isOp() || p.hasPermission("bastioncosmetics.free");}
    private boolean owns(Player p, String cat, String id) {return users.getBoolean(path(p)+"owned."+cat+"."+id);}
    private String equipped(Player p, String cat) {return users.getString(path(p)+"equipped."+cat, "");}
    private void setEquipped(Player p, String cat, String id) {
        users.set(path(p)+"equipped."+cat, id.isBlank()?null:id);
        touch(p);
        saveUsers();
    }
    private List<String> lore(String path) {return getConfig().getStringList(path);}
    private void fill(Inventory inv, String path) {
        Material filler = material(path, Material.BLACK_STAINED_GLASS_PANE);
        ItemStack pane = item(filler, " ", List.of());
        for (int slot=0; slot<inv.getSize(); slot++) inv.setItem(slot, pane);
    }

    private void mainMenu(Player player) {
        if (!allowed(player)) {tell(player,"only-world");return;}
        String path = "menus.main.";
        int size = normalizeSize(getConfig().getInt(path+"size", 27));
        Inventory inv = Bukkit.createInventory(null, size, c(getConfig().getString(path+"title")));
        fill(inv,path+"filler-material");
        for (String cat : new String[]{"auras", "balloons", "hats", "pets"}) {
            String entry = path+"sections."+cat+".";
            inv.setItem(safeSlot(getConfig().getInt(entry+"slot"), size), item(material(entry+"material", Material.BOOK),
                getConfig().getString(entry+"name",cat), lore(entry+"lore")));
        }
        views.put(player.getUniqueId(),new View(inv,"main"));
        player.openInventory(inv);
    }
    private int normalizeSize(int n) {return n>=9 && n<=54 && n%9==0 ? n : 36;}
    private void categoryMenu(Player player, String cat) {
        if (!allowed(player)) {tell(player,"only-world");return;}
        if (cosmetics(cat)==null) return;
        int size = normalizeSize(getConfig().getInt("menus.categories.size",36));
        Inventory inv = Bukkit.createInventory(null,size,c(getConfig().getString("menus.categories.title-"+cat,cat)));
        fill(inv,"menus.categories.filler-material");
        ConfigurationSection items = cosmetics(cat);
        for (String id : items.getKeys(false)) {
            String p="cosmetics."+cat+"."+id+".";
            int price=Math.max(0,getConfig().getInt(p+"price",0));
            List<String> lines=new ArrayList<>(lore(p+"lore"));
            boolean owned=owns(player,cat,id), current=id.equals(equipped(player,cat));
            String state;
            if (current) state="equipped";
            else if (owned) state="owned";
            else if (isFree(player)) state="free";
            else if (balance(player)>=price) state="purchasable";
            else state="insufficient";
            for (String line:lore("menus.categories.lore."+state)) lines.add(line.replace("%price%",Integer.toString(price)));
            ItemStack icon = item(material(p+"material",Material.BARRIER), getConfig().getString(p+"name",id),lines);
            if (cat.equals("hats") && icon.getType()==Material.PLAYER_HEAD) icon = applyHeadTexture(icon, p, player);
            if (cat.equals("balloons") && getConfig().getBoolean("settings.show-balloon-heads-in-menu",true)) {
                icon = balloonHead(id,player);
                ItemMeta iconMeta=icon.getItemMeta();
                if (iconMeta != null) {iconMeta.setDisplayName(c(getConfig().getString(p+"name",id)));iconMeta.setLore(lines.stream().map(this::c).toList());icon.setItemMeta(iconMeta);}
            }
            inv.setItem(safeSlot(getConfig().getInt(p+"slot",10),size),icon);
        }
        int back = safeSlot(getConfig().getInt("menus.categories.back-slot",31),size);
        inv.setItem(back,item(material("menus.categories.back-material",Material.ARROW),"&e← Volver",List.of("&7Regresa a las categorías")));
        inv.setItem(size-1,item(Material.EMERALD,"&bGemas: &f"+balance(player),List.of("&7Saldo disponible")));
        views.put(player.getUniqueId(),new View(inv,cat));
        player.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        View view=views.get(player.getUniqueId());
        if (view==null || event.getView().getTopInventory()!=view.inventory) {
            // Protect hat slot against unintended overwrites while equipped.
            if (event.getSlotType()==InventoryType.SlotType.ARMOR && !equipped(player,"hats").isBlank()) {
                if (event.getSlot()==39) event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory()!=view.inventory) return;
        int slot=event.getRawSlot();
        if (slot<0 || slot>=view.inventory.getSize()) return;
        if (view.category.equals("main")) {
            for (String cat:new String[]{"auras","balloons","hats","pets"}) {
                if (slot==getConfig().getInt("menus.main.sections."+cat+".slot",-1)) {
                    if (cat.equals("pets")) {player.closeInventory();tell(player,"upcoming");}
                    else categoryMenu(player,cat);
                    return;
                }
            }
            return;
        }
        if (slot==getConfig().getInt("menus.categories.back-slot",31)) {mainMenu(player);return;}
        ConfigurationSection items=cosmetics(view.category);
        if (items==null)return;
        for (String id:items.getKeys(false)) {
            String cp="cosmetics."+view.category+"."+id;
            if (slot!=getConfig().getInt(cp+".slot",-1)) continue;
            if (event.isRightClick()) {unequip(player,view.category,true);categoryMenu(player,view.category);return;}
            if (!event.isLeftClick())return;
            int price=Math.max(0,getConfig().getInt(cp+".price",0));
            boolean owned=owns(player,view.category,id);
            if (!owned && !isFree(player)) {
                if (balance(player)<price) {player.closeInventory();tell(player,"not-enough-gems");return;}
                if (!setBalance(player,balance(player)-price)) {player.closeInventory();tell(player,"no-scoreboard");return;}
            }
            // Don't take gems or unlock a hat unless it can be equipped safely.
            if (view.category.equals("hats") && !canEquipHat(player)) {
                // Refund purchase if the helmet is unavailable.
                if (!owned && !isFree(player)) setBalance(player,Math.min(1_000_000_000,balance(player)+price));
                player.closeInventory();tell(player,"helmet-slot");return;
            }
            if (!owned) {
                users.set(path(player)+"owned."+view.category+"."+id,true);
                touch(player);saveUsers();
                tell(player,isFree(player)?"free-claimed":"purchased","%cosmetic%",getConfig().getString(cp+".name",id),"%price%",Integer.toString(price));
            }
            equip(player,view.category,id);
            player.closeInventory();
            tell(player,"equipped","%cosmetic%",getConfig().getString(cp+".name",id));
            return;
        }
    }
    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p))return;
        View view=views.get(p.getUniqueId());
        if (view!=null && event.getView().getTopInventory()==view.inventory) {
            for (int raw:event.getRawSlots()) if (raw<view.inventory.getSize()) {event.setCancelled(true);return;}
        }
    }
    @EventHandler public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p))return;
        View view=views.get(p.getUniqueId());
        if (view!=null && view.inventory==event.getInventory()) views.remove(p.getUniqueId());
    }
    private boolean canEquipHat(Player p) {
        if (!equipped(p,"hats").isBlank())return true;
        ItemStack helmet=p.getInventory().getHelmet();
        return helmet==null || helmet.getType().isAir();
    }
    private ItemStack hatItem(String id, Player player) {
        String base="cosmetics.hats."+id+".";
        Material mat=Material.matchMaterial(getConfig().getString(base+"helmet","GOLDEN_HELMET"));
        if(mat==null || !mat.isItem())mat=Material.GOLDEN_HELMET;
        ItemStack stack=new ItemStack(mat);
        if (mat==Material.PLAYER_HEAD) stack=applyHeadTexture(stack,base,player);
        ItemMeta meta=stack.getItemMeta();
        if(meta!=null) {
            meta.setDisplayName(c(getConfig().getString(base+"name",id)));
            meta.getPersistentDataContainer().set(cosmeticHatKey,PersistentDataType.STRING,id);
            stack.setItemMeta(meta);
        }
        return stack;
    }
    private boolean isCosmeticHat(ItemStack helmet) {
        if (helmet==null || helmet.getType().isAir() || !helmet.hasItemMeta()) return false;
        return helmet.getItemMeta().getPersistentDataContainer().has(cosmeticHatKey,PersistentDataType.STRING);
    }
    private void equip(Player p,String cat,String id) {
        if (cat.equals("balloons"))removeBalloon(p.getUniqueId());
        if (cat.equals("hats")) {
            if (!equipped(p,"hats").isBlank())restoreHat(p,false);
            if (!canEquipHat(p))return;
            p.getInventory().setHelmet(hatItem(id,p));
        }
        setEquipped(p,cat,id);
        if (cat.equals("balloons"))spawnBalloon(p);
    }
    private void unequip(Player p,String cat,boolean notify) {
        if (equipped(p,cat).isBlank()) {if(notify)tell(p,"not-equipped");return;}
        if (cat.equals("balloons"))removeBalloon(p.getUniqueId());
        if (cat.equals("hats"))restoreHat(p,false);
        setEquipped(p,cat,"");
        if (notify)tell(p,"unequipped","%category%",categoryName(cat));
    }
    private void restoreHat(Player p,boolean clearRecord) {
        if (equipped(p,"hats").isBlank())return;
        ItemStack helmet=p.getInventory().getHelmet();
        if (isCosmeticHat(helmet))p.getInventory().setHelmet(null);
        if (clearRecord)setEquipped(p,"hats","");
    }
    private void removeBalloon(UUID uuid) {
        Balloon balloon=balloons.remove(uuid);
        if (balloon!=null)safeRemove(balloon);
    }
    private void safeRemove(Balloon balloon) {
        try {if (balloon.head.isValid())balloon.head.remove();}catch(Exception ignored){}
        try {if (balloon.leashAnchor.isValid())balloon.leashAnchor.remove();}catch(Exception ignored){}
    }
    private ItemStack applyHeadTexture(ItemStack skull, String base, Player player) {
        if (skull.getType()!=Material.PLAYER_HEAD) return skull;
        SkullMeta meta=(SkullMeta)skull.getItemMeta();
        String texture=getConfig().getString(base+"head-texture","").trim();
        String owner=getConfig().getString(base+"head-owner","").trim();
        if (!texture.isEmpty()) {
            try {
                // Only texture hashes hosted by Mojang; no arbitrary remote requests.
                if (!texture.matches("[0-9a-fA-F]{32,128}"))throw new IllegalArgumentException("hash inválido");
                PlayerProfile profile=Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures=profile.getTextures();
                textures.setSkin(new URL("https://textures.minecraft.net/texture/"+texture));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (Exception ex) {
                getLogger().warning("Textura incorrecta en "+base+": "+ex.getMessage());
            }
        } else if (owner.equalsIgnoreCase("%player%")) {
            meta.setOwningPlayer(player);
        } else if (!owner.isEmpty()) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }
        skull.setItemMeta(meta);
        return skull;
    }
    private ItemStack balloonHead(String id, Player player) {
        return applyHeadTexture(new ItemStack(Material.PLAYER_HEAD), "cosmetics.balloons."+id+".", player);
    }
    private void spawnBalloon(Player player) {
        removeBalloon(player.getUniqueId());
        if(!allowed(player))return;
        String id=equipped(player,"balloons");
        if(id.isBlank())return;
        Location at=balloonLocation(player);
        Chicken anchor=null;
        ItemDisplay head=null;
        try {
            anchor=player.getWorld().spawn(at,Chicken.class, chick -> {
                chick.setAI(false);
                chick.setGravity(false);
                chick.setSilent(true);
                chick.setInvulnerable(true);
                chick.setCollidable(false);
                chick.setPersistent(false);
                chick.setRemoveWhenFarAway(false);
                chick.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
            });
            head=player.getWorld().spawn(at.clone().add(0,.2,0),ItemDisplay.class, display -> {
                display.setItemStack(balloonHead(id,player));
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                display.setPersistent(false);
                display.setInvulnerable(true);
                display.setInterpolationDuration(3);
                display.setTeleportDuration(3);
            });
            anchor.setLeashHolder(player);
            balloons.put(player.getUniqueId(),new Balloon(anchor,head));
        }catch(Exception ex) {
            getLogger().warning("No se pudo crear globo "+id+": "+ex.getMessage());
            if(head!=null)head.remove();
            if(anchor!=null)anchor.remove();
        }
    }
    private Location balloonLocation(Player p) {
        Location base=p.getLocation().clone();
        Vector forward=base.getDirection().setY(0);
        if(forward.lengthSquared()<0.01) forward=new Vector(1,0,0);
        forward.normalize();
        Vector side=new Vector(-forward.getZ(),0,forward.getX()).multiply(1.05);
        return base.add(side).add(0,2.35,0);
    }
    private void tickCosmetics() {
        ticks+=3;
        int auraPeriod=Math.max(3,getConfig().getInt("settings.aura-period-ticks",6));
        int balloonPeriod=Math.max(3,getConfig().getInt("settings.balloon-period-ticks",3));
        for(Player p:Bukkit.getOnlinePlayers()) {
            if(!allowed(p) || p.isDead()) {
                removeBalloon(p.getUniqueId());
                continue;
            }
            String aura=equipped(p,"auras");
            if(!aura.isEmpty() && ticks%auraPeriod<3) {
                String cp="cosmetics.auras."+aura+".";
                Particle particle;
                try {particle=Particle.valueOf(getConfig().getString(cp+"particle","FLAME"));}
                catch(IllegalArgumentException ex){particle=Particle.FLAME;}
                int count=Math.min(80,Math.max(1,getConfig().getInt(cp+"count",12)));
                double radius=Math.max(.1,Math.min(2.0,getConfig().getDouble(cp+"radius",.58)));
                try {
                    if (aura.equals("fire") || getConfig().getBoolean(cp+"spiral",false)) {
                        // A moving helix makes the aura visible around the whole player, not one small puff.
                        Location base=p.getLocation();
                        for(int i=0;i<count;i++) {
                            double angle=(i*2.399963229728653)+(ticks*.10);
                            double y=.15+(i%12)*.132;
                            p.getWorld().spawnParticle(particle,base.getX()+Math.cos(angle)*radius,base.getY()+y,base.getZ()+Math.sin(angle)*radius,1,0,0,0,0);
                        }
                        if (aura.equals("fire")) {
                            p.getWorld().spawnParticle(Particle.SMALL_FLAME,base.clone().add(0,.85,0),Math.max(3,count/3),radius,.6,radius,.005);
                        }
                    } else {
                        p.getWorld().spawnParticle(particle,p.getLocation().add(0,1,0),count,radius,.75,radius,.015);
                    }
                } catch(Exception ignored) {}
            }
            if(!equipped(p,"balloons").isEmpty()) {
                Balloon balloon=balloons.get(p.getUniqueId());
                if(balloon==null || !balloon.head.isValid() || !balloon.leashAnchor.isValid() || !balloon.head.getWorld().equals(p.getWorld()))spawnBalloon(p);
                else if(ticks%balloonPeriod<3) {
                    Location destination=balloonLocation(p);
                    balloon.leashAnchor.teleport(destination);
                    balloon.head.teleport(destination.clone().add(0,.2,0));
                    if (!balloon.leashAnchor.isLeashed()) balloon.leashAnchor.setLeashHolder(p);
                }
            }
        }
    }
    private void resumeVisuals(Player p) {
        if(!p.isOnline() || !allowed(p))return;
        if(!equipped(p,"balloons").isEmpty())spawnBalloon(p);
        // Hats are removed on quit/disable. Reequip safely if there is no physical helmet.
        String hat=equipped(p,"hats");
        if(!hat.isEmpty() && canReapplyHat(p)) p.getInventory().setHelmet(hatItem(hat,p));
    }
    private boolean canReapplyHat(Player p) {ItemStack helmet=p.getInventory().getHelmet();return helmet==null || helmet.getType().isAir();}
    @EventHandler public void join(PlayerJoinEvent e) {Bukkit.getScheduler().runTaskLater(this,()->resumeVisuals(e.getPlayer()),20);}
    @EventHandler public void quit(PlayerQuitEvent e) {views.remove(e.getPlayer().getUniqueId());removeBalloon(e.getPlayer().getUniqueId());restoreHat(e.getPlayer(),false);saveUsers();}
    @EventHandler public void world(PlayerChangedWorldEvent e) {
        Player p=e.getPlayer();removeBalloon(p.getUniqueId());
        if(!allowed(p))restoreHat(p,false);
        else Bukkit.getScheduler().runTaskLater(this,()->resumeVisuals(p),2);
    }
    @EventHandler public void death(org.bukkit.event.entity.PlayerDeathEvent e) {
        Player p=e.getEntity();
        String hat=equipped(p,"hats");
        if(!hat.isBlank()) {
            ItemStack helmet=p.getInventory().getHelmet();
            if(isCosmeticHat(helmet)) {
                // Avoid duplicating cosmetic helmets in drops.
                e.getDrops().removeIf(drop -> drop.isSimilar(helmet));
                p.getInventory().setHelmet(null);
            }
        }
        removeBalloon(p.getUniqueId());
    }
    @EventHandler public void respawn(PlayerRespawnEvent e) {Bukkit.getScheduler().runTaskLater(this,()->resumeVisuals(e.getPlayer()),5);}
    @EventHandler public void interact(PlayerInteractEvent e) {
        Player p=e.getPlayer();
        if(e.getItem()==null || equipped(p,"hats").isBlank())return;
        Material m=e.getItem().getType();
        if(m.name().endsWith("_HELMET") || m==Material.CARVED_PUMPKIN || m==Material.PLAYER_HEAD) {
            if(e.getAction()==org.bukkit.event.block.Action.RIGHT_CLICK_AIR || e.getAction()==org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) e.setCancelled(true);
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cosmeticos")) {
            if(args.length>0 && args[0].equalsIgnoreCase("reload")) {
                if(!sender.hasPermission("bastioncosmetics.admin")){if(sender instanceof Player p)tell(p,"no-permission");return true;}
                reloadConfig();
                for(Player p:Bukkit.getOnlinePlayers()) {removeBalloon(p.getUniqueId());resumeVisuals(p);}
                sender.sendMessage(c(msg("prefix")+msg("reloaded")));
                return true;
            }
            if(!(sender instanceof Player p)){sender.sendMessage("Solo jugadores pueden abrir el menú.");return true;}
            mainMenu(p);return true;
        }
        if(command.getName().equalsIgnoreCase("gemas")) {
            if(args.length==0) {
                if(!(sender instanceof Player p)) {sender.sendMessage("/gemas add|remove|set <jugador> <cantidad>");return true;}
                tell(p,"gems","%gems%",Integer.toString(balance(p)));return true;
            }
            if(args.length==1 && sender.hasPermission("bastioncosmetics.admin")) {
                Player other=Bukkit.getPlayerExact(args[0]);
                if(other==null) {sender.sendMessage(c(msg("player-not-found")));return true;}
                sender.sendMessage(c(msg("gems-other").replace("%player%",other.getName()).replace("%gems%",Integer.toString(balance(other)))));return true;
            }
            if(!sender.hasPermission("bastioncosmetics.admin")){sender.sendMessage(c(msg("no-permission")));return true;}
            if(args.length!=3 || !(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set"))) {
                sender.sendMessage(c(msg("usage")));return true;
            }
            Player target=Bukkit.getPlayerExact(args[1]);
            if(target==null) {sender.sendMessage(c(msg("player-not-found")));return true;}
            int amount;
            try {amount=Integer.parseInt(args[2]);if(amount<0 || amount>1_000_000_000)throw new NumberFormatException();}
            catch(NumberFormatException ex) {sender.sendMessage(c(msg("invalid-number")));return true;}
            int current=balance(target);
            int next=switch(args[0].toLowerCase(Locale.ROOT)) {
                case "add" -> (int)Math.min(1_000_000_000L,(long)current+amount);
                case "remove" -> Math.max(0,current-amount);
                default -> amount;
            };
            if(!setBalance(target,next)) {sender.sendMessage(c(msg("no-scoreboard")));return true;}
            sender.sendMessage(c(msg("gems-changed").replace("%player%",target.getName()).replace("%gems%",Integer.toString(next))));
            return true;
        }
        return false;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(command.getName().equalsIgnoreCase("cosmeticos")) {
            return args.length==1 && sender.hasPermission("bastioncosmetics.admin") && "reload".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("reload"):List.of();
        }
        if(!sender.hasPermission("bastioncosmetics.admin"))return List.of();
        if(args.length==1)return filter(List.of("add","remove","set"),args[0]);
        if(args.length==2)return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);
        if(args.length==3)return filter(List.of("10","50","100","500","1000"),args[2]);
        return List.of();
    }
    private List<String> filter(List<String> choices,String prefix){return choices.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).toList();}
}
