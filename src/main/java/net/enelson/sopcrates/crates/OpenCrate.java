package net.enelson.sopcrates.crates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.enelson.sopcrates.SopCrates;
import net.enelson.sopcrates.utils.Utils;

public class OpenCrate {

    private static final float[] SLOT_SCALES = new float[] { 0.3F, 0.6F, 1.2F, 0.6F, 0.3F };

    private final CrateBlock crateBlock;
    private final Player player;

    private final Slot[] slots = new Slot[5];
    private final List<OpenPrize> timeline = new ArrayList<OpenPrize>();

    private BukkitTask tasker;

    private boolean finished = false;
    private boolean rewardGiven = false;
    private boolean started = false;
    private boolean hologramHidden = false;

    private int elapsedTicks = 0;
    private int spinDurationTicks = 120;
    private int centerIndex = 2;
    private int shiftsDone = 0;
    private int totalShifts = 0;

    private int rewardDelayTicks = 10;
    private int settleTicks = 2;

    private OpenPrize finalPrize;

    public OpenCrate(CrateBlock crateBlock, Player player) {
        this.crateBlock = crateBlock;
        this.player = player;
    }

    public void open() {
        if (started || finished) {
            return;
        }
        started = true;

        YamlConfiguration crateConfig = getCrateConfig();
        if (crateConfig == null) {
            player.sendMessage("Р’В§cР СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С‘РЎвЂљРЎРЉ Р С”Р С•Р Р…РЎвЂћР С‘Р С– Р С”Р ВµР в„–РЎРѓР В°.");
            forceStop();
            return;
        }

        ConfigurationSection prizesSection = crateConfig.getConfigurationSection("prizes");
        if (prizesSection == null || prizesSection.getKeys(false).isEmpty()) {
            player.sendMessage("Р’В§cР вЂ™ Р С”Р ВµР в„–РЎРѓР Вµ Р Р…Р ВµРЎвЂљ Р С—РЎР‚Р С‘Р В·Р С•Р Р†.");
            forceStop();
            return;
        }

        this.spinDurationTicks = resolveSpinDurationTicks(crateConfig);
        this.totalShifts = resolveTotalShifts(this.spinDurationTicks);
        this.finalPrize = rollPrize(crateConfig);
        buildTimeline(crateConfig, this.finalPrize);
        createSlots(crateConfig);
        updateVisibleSlots();
        hideHologramIfNeeded();

        this.tasker = new BukkitRunnable() {
            @Override
            public void run() {
                if (finished) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    forceStop();
                    cancel();
                    return;
                }

                elapsedTicks++;
                int targetShift = resolveShiftTarget();
                boolean shifted = false;

                while (shiftsDone < targetShift && shiftsDone < totalShifts) {
                    centerIndex++;
                    shiftsDone++;
                    shifted = true;
                }

                if (shifted) {
                    updateVisibleSlots();
                    playSpinTickSound();
                }

                if (elapsedTicks >= spinDurationTicks && shiftsDone >= totalShifts) {
                    Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            finishOpening();
                        }
                    }, 15L);
                    cancel();
                }
            }
        }.runTaskTimer(SopCrates.getInstance(), 0L, 1L);
    }

    public Player getPlayer() {
        return player;
    }

    public CrateBlock getCrateBlock() {
        return crateBlock;
    }

    public OpenPrize getFinalPrize() {
        return finalPrize;
    }

    public void forceStop() {
        finished = true;

        if (tasker != null) {
            tasker.cancel();
            tasker = null;
        }

        removeSlots();
        restoreHologramIfNeeded();
        SopCrates.getInstance().getCratesManager().removeOpenCrate(this);
    }

    public OpenPrize[] generatePrizes() {
        YamlConfiguration crateConfig = getCrateConfig();
        if (crateConfig == null) {
            return new OpenPrize[0];
        }

        OpenPrize[] prizes = new OpenPrize[100];
        for (int i = 0; i < prizes.length; i++) {
            prizes[i] = randomVisualPrize(crateConfig);
        }
        return prizes;
    }

    public BukkitTask setPlay(int speed) {
        return tasker;
    }

    private void buildTimeline(YamlConfiguration crateConfig, OpenPrize prize) {
        timeline.clear();

        int prefixItems = this.totalShifts + this.centerIndex;
        for (int i = 0; i < prefixItems; i++) {
            timeline.add(randomVisualPrize(crateConfig));
        }

        timeline.add(prize);
        timeline.add(randomVisualPrize(crateConfig));
        timeline.add(randomVisualPrize(crateConfig));

        this.centerIndex = 2;
        this.shiftsDone = 0;
        this.elapsedTicks = 0;
    }

    private int resolveShiftTarget() {
        int effectiveSpinTicks = Math.max(1, this.spinDurationTicks - this.settleTicks);
        double progress = Math.min(1.0D, (double) this.elapsedTicks / (double) effectiveSpinTicks);
        double eased = easeSpinProgress(progress);
        return Math.min(this.totalShifts, (int) Math.floor(eased * this.totalShifts));
    }

    private double easeSpinProgress(double progress) {
        if (progress <= 0.0D) {
            return 0.0D;
        }
        if (progress >= 1.0D) {
            return 1.0D;
        }

        double accelerationPart = 0.18D;
        double accelerationDistance = 0.34D;
        if (progress < accelerationPart) {
            double local = progress / accelerationPart;
            return accelerationDistance * local * local;
        }

        double local = (progress - accelerationPart) / (1.0D - accelerationPart);
        double easedOut = 1.0D - Math.pow(1.0D - local, 2.0D);
        return accelerationDistance + ((1.0D - accelerationDistance) * easedOut);
    }

    private void createSlots(YamlConfiguration crateConfig) {
        removeSlots();

        Location base = getSpinBaseLocation(crateConfig);
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        DirectionVector direction = resolveDirection(crateBlock.getDirection());
        for (int i = -2; i <= 2; i++) {
            Location loc = base.clone().add(direction.x * i, 0.0D, direction.z * i);
            slots[i + 2] = SlotFactory.create(loc, SLOT_SCALES[i + 2]);
        }
    }

    private void updateVisibleSlots() {
        if (!hasAllSlots()) {
            return;
        }

        slots[0].setPrize(copyPrize(timeline.get(centerIndex - 2)));
        slots[1].setPrize(copyPrize(timeline.get(centerIndex - 1)));
        slots[2].setPrize(copyPrize(timeline.get(centerIndex)));
        slots[3].setPrize(copyPrize(timeline.get(centerIndex + 1)));
        slots[4].setPrize(copyPrize(timeline.get(centerIndex + 2)));
    }

    private boolean hasAllSlots() {
        for (Slot slot : slots) {
            if (slot == null) {
                return false;
            }
        }
        return true;
    }

    private void finishOpening() {
        if (finished) {
            return;
        }

        finished = true;

        for (int i = 0; i < slots.length; i++) {
            if (i != 2 && slots[i] != null) {
                slots[i].remove();
                slots[i] = null;
            }
        }

        Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
            @Override
            public void run() {
                playFinalSound();
                spawnFirework();

                if (!rewardGiven) {
                    giveFinalPrize();
                }
            }
        }, rewardDelayTicks + 10L);

        Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
            @Override
            public void run() {
                removeSlots();
                restoreHologramIfNeeded();
                SopCrates.getInstance().getCratesManager().removeOpenCrate(OpenCrate.this);
            }
        }, rewardDelayTicks + 40L);
    }

    private void giveFinalPrize() {
        if (rewardGiven || finalPrize == null) {
            return;
        }

        rewardGiven = true;
        YamlConfiguration crateConfig = getCrateConfig();
        if (crateConfig == null) {
            return;
        }

        try {
            Utils.givePrize(player, crateConfig, "prizes." + finalPrize.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeSlots() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                try {
                    slots[i].remove();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                slots[i] = null;
            }
        }
    }

    private OpenPrize rollPrize(YamlConfiguration crateConfig) {
        ConfigurationSection prizesSection = crateConfig.getConfigurationSection("prizes");
        if (prizesSection == null) {
            throw new IllegalStateException("Prizes section is null for crate " + crateBlock.getCrateName());
        }

        List<String> ids = new ArrayList<String>(prizesSection.getKeys(false));
        int totalWeight = 0;
        for (String id : ids) {
            totalWeight += Math.max(1, crateConfig.getInt("prizes." + id + ".weight", 1));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int current = 0;
        for (String id : ids) {
            current += Math.max(1, crateConfig.getInt("prizes." + id + ".weight", 1));
            if (current >= random) {
                return createPrize(crateConfig, id);
            }
        }

        return createPrize(crateConfig, ids.get(ids.size() - 1));
    }

    private OpenPrize randomVisualPrize(YamlConfiguration crateConfig) {
        ConfigurationSection prizesSection = crateConfig.getConfigurationSection("prizes");
        if (prizesSection == null) {
            throw new IllegalStateException("Prizes section is null for crate " + crateBlock.getCrateName());
        }

        List<String> ids = new ArrayList<String>(prizesSection.getKeys(false));
        String id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
        return createPrize(crateConfig, id);
    }

    private OpenPrize createPrize(YamlConfiguration crateConfig, String prizeId) {
        double chance = Utils.getPrizeChance(crateConfig, prizeId);
        ItemStack item = Utils.getItem(crateConfig, prizeId, chance);
        if (item == null || item.getType() == Material.AIR) {
            item = new ItemStack(Material.BARRIER);
        }
        return new OpenPrize(item, crateBlock.getCrateName(), prizeId, chance);
    }

    private OpenPrize copyPrize(OpenPrize prize) {
        return new OpenPrize(prize.getDisplayItem().clone(), prize.getCrateType(), prize.getId(), prize.getChance());
    }

    private void playSpinTickSound() {
        World world = crateBlock.getLocation().getWorld();
        if (world != null) {
            world.playSound(crateBlock.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7F, 1.8F);
        }
    }

    private void playFinalSound() {
        World world = crateBlock.getLocation().getWorld();
        if (world != null) {
            world.playSound(crateBlock.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            world.playSound(crateBlock.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.9F, 1.2F);
        }
    }

    private void spawnFirework() {
        World world = crateBlock.getLocation().getWorld();
        if (world == null) {
            return;
        }

        YamlConfiguration crateConfig = getCrateConfig();
        Location location = crateBlock.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        location.add(
                crateConfig.getDouble("firework.offset.x", 0.0D),
                crateConfig.getDouble("firework.offset.y", 0.7D),
                crateConfig.getDouble("firework.offset.z", 0.0D)
        );

        try {
            final Firework firework = world.spawn(location, Firework.class);
            firework.setMetadata("nodamage", new FixedMetadataValue(SopCrates.getInstance(), true));
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(0);
            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.YELLOW, Color.ORANGE)
                    .withFade(Color.WHITE)
                    .trail(true)
                    .flicker(true)
                    .build());
            firework.setFireworkMeta(meta);
            Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
                @Override
                public void run() {
                    firework.detonate();
                }
            }, 2L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private YamlConfiguration getCrateConfig() {
        return SopCrates.getInstance().getCratesManager().getCrateConfig(crateBlock.getCrateName());
    }

    private Location getSpinBaseLocation(YamlConfiguration crateConfig) {
        Location base = crateBlock.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        return base.add(
                crateConfig.getDouble("spin.offset.x", 0.0D),
                crateConfig.getDouble("spin.offset.y", 0.95D),
                crateConfig.getDouble("spin.offset.z", 0.0D)
        );
    }

    private int resolveSpinDurationTicks(YamlConfiguration crateConfig) {
        Object raw = crateConfig.get("spin.duration");
        if (raw == null) {
            return resolveLegacySpeedTicks(crateConfig.getString("speed", "medium"));
        }

        if (raw instanceof Number) {
            return Math.max(20, (int) Math.round(((Number) raw).doubleValue() * 20.0D));
        }

        String value = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("ms")) {
                return Math.max(20, (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 2)) / 50.0D));
            }
            if (value.endsWith("t")) {
                return Math.max(20, (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1))));
            }
            if (value.endsWith("s")) {
                return Math.max(20, (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1)) * 20.0D));
            }
            if (value.endsWith("m")) {
                return Math.max(20, (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1)) * 1200.0D));
            }
            return Math.max(20, (int) Math.round(Double.parseDouble(value) * 20.0D));
        } catch (NumberFormatException e) {
            return resolveLegacySpeedTicks(value);
        }
    }

    private int resolveLegacySpeedTicks(String speed) {
        switch (speed.toLowerCase(Locale.ROOT)) {
            case "fast":
                return 80;
            case "slow":
                return 140;
            default:
                return 110;
        }
    }

    private int resolveTotalShifts(int durationTicks) {
        return Math.max(26, (int) Math.round(durationTicks * 0.55D));
    }

    private void hideHologramIfNeeded() {
        if (crateBlock.shouldHideHologramWhileSpinning()) {
            this.hologramHidden = true;
            crateBlock.suppressHologram();
        }
    }

    private void restoreHologramIfNeeded() {
        if (this.hologramHidden) {
            this.hologramHidden = false;
            crateBlock.unsuppressHologram();
        }
    }

    private DirectionVector resolveDirection(double direction) {
        double radians = Math.toRadians(direction);
        double x = -Math.cos(radians);
        double z = -Math.sin(radians);
        return new DirectionVector(x, z);
    }

    private static final class DirectionVector {
        private final double x;
        private final double z;

        private DirectionVector(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
}