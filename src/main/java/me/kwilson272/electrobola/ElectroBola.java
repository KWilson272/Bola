package me.kwilson272.electrobola;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ColoredParticle;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ElectroBola extends ChiAbility implements AddonAbility {

    private static final String CONFIG_PATH = "ExtraAbilities.KWilson272.ElectroBola.";

    private static final ColoredParticle BOLA_ROPE_PARTICLE =
            new ColoredParticle(Color.fromRGB(150, 125, 75), 0.6f);
    private static final ColoredParticle IMMO_ROPE_PARTICLE =
            new ColoredParticle(Color.fromRGB(150, 125, 75), 0.8f);
    private static final ColoredParticle BALL_PARTICLE =
            new ColoredParticle(Color.fromRGB(70, 70, 70), 0.8f);
    private static final ColoredParticle SHOCK_PARTICLE_1 =
            new ColoredParticle(Color.fromRGB(50, 215, 255), 0.7f);
    private static final ColoredParticle SHOCK_PARTICLE_2 =
            new ColoredParticle(Color.fromRGB(0, 125, 255), 0.7f);

    private static ElectroBolaListener electroBolaListener;

    @Attribute("OnHit" + Attribute.COOLDOWN)
    private long onHitCooldown;
    @Attribute("OnMiss" + Attribute.COOLDOWN)
    private long onMissCooldown;
    @Attribute(Attribute.SPEED)
    private double speed;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute("Affect" + Attribute.RADIUS)
    private double affectRadius;
    @Attribute("Collision" + Attribute.RADIUS)
    private double collisionRadius;
    private double verticalSensitivity;
    private double horizontalSensitivity;

    private double iterSpeed;
    private double rangeCounter;
    private Location location;
    private Vector direction;

    private boolean isTravelling;
    private long removalTime;
    private LivingEntity stunnedEntity;

    private double animAngle;
    private long soundTime;
    private long soundInterval;
    private float soundPitch;

    private List<Vector> ropeVecs;
    private List<ElectricityArc> arcs;

    public ElectroBola(Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBinds(this) || hasTravelingBola(player)) {
            return;
        }

        onHitCooldown = getConfig().getLong(CONFIG_PATH + "OnHitCooldown");
        onMissCooldown = getConfig().getLong(CONFIG_PATH + "OnMissCooldown");
        speed = getConfig().getDouble(CONFIG_PATH + "Speed");
        range = getConfig().getDouble(CONFIG_PATH + "Range");
        duration = getConfig().getLong(CONFIG_PATH + "StunDuration");
        affectRadius = getConfig().getDouble(CONFIG_PATH + "AffectRadius");
        collisionRadius = getConfig().getDouble(CONFIG_PATH + "CollisionRadius");
        verticalSensitivity = Math.min(1, Math.max(0, getConfig().getDouble(CONFIG_PATH + "VerticalSensitivity") / 100));
        horizontalSensitivity = Math.min(1, Math.max(0, getConfig().getDouble(CONFIG_PATH + "HorizontalSensitivity") / 100));

        iterSpeed = calculateIterspeed();
        rangeCounter = 0;
        // Keep sensitivity consistent through all speeds
        verticalSensitivity *= (iterSpeed / speed);
        horizontalSensitivity *= (iterSpeed / speed);

        location = player.getEyeLocation();
        direction = location.getDirection();
        isTravelling = true;

        animAngle = 0.0;
        soundTime = 0;
        // Adjust sound effects to give a better feel for speed
        soundInterval = Math.min(150, (long) ((1.5 / speed) * 50));
        soundPitch = (float) Math.min(1.0, Math.max(0.4f, (speed / 1.5)));

        ropeVecs = new ArrayList<>();
        arcs = new ArrayList<>();

        start();
    }

    private boolean hasTravelingBola(Player player) {
        for (ElectroBola electroBola : CoreAbility.getAbilities(player, ElectroBola.class)) {
            if (electroBola.isTravelling) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the max amount of blocks the location will advance per iteration.
     * This is done to ensure support for all speeds, while also maintaining a relatively
     * consistent appearance between speeds.
     *
     * @return a double value between 0.5 & 0.9
     */
    private double calculateIterspeed() {
        if (speed <= 0.9) {
            return speed;
        } else {
            int toRound = ((int) speed) + 5; // Ensure we round up, but never above the nearest 10
            double tenMod = Math.round(toRound / 10.0);
            return speed / (Math.floor(speed) + (tenMod + 1));
        }
    }

    @Override
    public void progress() {
        if (!isTravelling) {
            if (System.currentTimeMillis() > removalTime || stunnedEntity.isDead()
                    || stunnedEntity instanceof Player immoPlayer && !immoPlayer.isOnline()) {
                remove();
                return;
            }

            if (ThreadLocalRandom.current().nextInt(6) == 0) {
                createArc();
            }
            arcs.removeIf(arc -> !arc.draw());

            double centerY = stunnedEntity.getHeight() / 2;
            double yOffset = stunnedEntity.getHeight() / 5;
            Location midLoc = stunnedEntity.getLocation().add(0, centerY, 0);
            Location lowLoc = midLoc.clone().subtract(0, yOffset, 0);
            Location highLoc = midLoc.clone().add(0, yOffset, 0);
            ropeVecs.forEach(vec -> {
                IMMO_ROPE_PARTICLE.display(midLoc.clone().add(vec), 1, 0, 0, 0);
                IMMO_ROPE_PARTICLE.display(lowLoc.clone().add(vec), 1, 0, 0, 0);
                IMMO_ROPE_PARTICLE.display(highLoc.clone().add(vec), 1, 0, 0, 0);
            });

        } else if (!bPlayer.canBendIgnoreBinds(this) || !advanceLocation()) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    private void createArc() {
        double length = (stunnedEntity.getHeight() + stunnedEntity.getWidth()) / 2;
        arcs.add(new ElectricityArc(stunnedEntity, length));
    }

    private boolean advanceLocation() {
        if (System.currentTimeMillis() > soundTime) {
            soundTime = System.currentTimeMillis() + soundInterval;
            Sound sound = isWater(location.getBlock()) ?
                    Sound.ENTITY_BOAT_PADDLE_WATER : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            location.getWorld().playSound(location, sound, 1, soundPitch);
        }

        for (double i = 0; i < speed; i += iterSpeed) {
            rangeCounter += iterSpeed;
            if (rangeCounter > range || GeneralMethods.isSolid(location.getBlock())) {
                return false;
            }
            if (isLava(location.getBlock())) {
                ParticleEffect.FLAME.display(location, 5, 0.5, 0.5, 0.5, 0.0125);
                ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location, 0, 0, 1, 0, 0.1);
                ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location, 0, 0, 1, 0, 0.1);
                location.getWorld().playSound(location, Sound.BLOCK_CANDLE_EXTINGUISH, 1.5f, 1);
                location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.2f, 1);
                return false;
            }

            direction = getDirection();
            location.add(direction.clone().multiply(iterSpeed));
            if (hitsDiagonal()) {
                return false;
            }
            drawBola();

            if (affectEntity()) {
                location.getWorld().playSound(stunnedEntity, Sound.BLOCK_MUD_BREAK, 1, 1.2f);
                location.getWorld().playSound(stunnedEntity, Sound.ENTITY_HORSE_BREATHE, 1, 0.5f);
                location.getWorld().spawnParticle(Particle.FLASH, stunnedEntity.getLocation(), 1);
                initializeRopeCircle();
                createArc();

                removalTime = System.currentTimeMillis() + duration;
                isTravelling = false;

                bPlayer.addCooldown(this);
                break;
            }
        }
        return true;
    }

    // Calculates a vector using weighted components of two vectors to simulate 'sensitivity'
    private Vector getDirection() {
        Vector targDir = player.getEyeLocation().getDirection();
        double oldVertAngle = Math.asin(direction.getY());
        double targVertAngle = Math.asin(targDir.getY());

        double oldVertWeight = 1 - verticalSensitivity;
        double targVertWeight = verticalSensitivity;
        double finalVertAngle = (oldVertAngle * oldVertWeight) + (targVertAngle * targVertWeight);

        double xzMag = Math.cos(finalVertAngle); //Ensure our returned vector is a unit vector
        double y = Math.sin(finalVertAngle);

        double oldHorizWeight = 1 - horizontalSensitivity;
        double targHorizWeight = horizontalSensitivity;
        double x = (oldHorizWeight * direction.getX()) + (targHorizWeight * targDir.getX());
        double z = (oldHorizWeight * direction.getZ()) + (targHorizWeight * targDir.getZ());

        return new Vector(x, 0, z).normalize().multiply(xzMag).setY(y);
    }

    // Custom method bc PK's is too sensitive
    private boolean hitsDiagonal() {
        double vertAngle = Math.atan2(direction.getX(), direction.getZ());
        double horizAngle = Math.toRadians(90) + Math.asin(direction.getY());
        double xzMag = Math.cos(horizAngle);
        double y = Math.sin(horizAngle);
        double x = xzMag * Math.sin(vertAngle);
        double z = xzMag * Math.cos(vertAngle);

        Vector ortho1 = new Vector(x, y, z).multiply(iterSpeed);
        Vector ortho2 = GeneralMethods.rotateVectorAroundVector(direction, ortho1, 90);

        Block above = location.clone().add(ortho1).getBlock();
        Block below = location.clone().subtract(ortho1).getBlock();
        Block side1 = location.clone().add(ortho2).getBlock();
        Block side2 = location.clone().subtract(ortho2).getBlock();

        return (GeneralMethods.isSolid(above) && GeneralMethods.isSolid(below)) ||
                (GeneralMethods.isSolid(side1) && GeneralMethods.isSolid(side2));
    }

    private boolean affectEntity() {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, affectRadius)) {
            if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                stunnedEntity = livingEntity;
                MovementHandler mh = new MovementHandler(stunnedEntity, this);
                mh.stopWithDuration(duration / 50, Element.CHI.getColor() + "* Immobilized *");
                return true;
            }
        }
        return false;
    }

    private void drawBola() {
        animAngle += 20;
        animAngle %= 360;

        double vertAngle = Math.atan2(direction.getX(), direction.getZ());
        double horizAngle = Math.toRadians(90) + Math.asin(direction.getY());
        double xzMag = Math.cos(horizAngle);
        double y = Math.sin(horizAngle);
        double x = xzMag * Math.sin(vertAngle);
        double z = xzMag * Math.cos(vertAngle);

        Vector rotAxis = new Vector(x, y, z);
        Vector stringDir = GeneralMethods.rotateVectorAroundVector(rotAxis, direction, animAngle);
        double iterLength = 0.1;
        stringDir.multiply(iterLength);

        for (int i = 0; i < 2; i++) {
            Location loc = location.clone();
            for (double j = 0.0; j < affectRadius; j += iterLength) {
                BOLA_ROPE_PARTICLE.display(loc, 1, 0, 0, 0);
                loc.add(stringDir);
            }

            stringDir.multiply(-1); // To Create the opposing bola string
            BALL_PARTICLE.display(loc, 5, 0.1, 0.1, 0.1);
            if (isWater(loc.getBlock())) {
                ParticleEffect.WATER_BUBBLE.display(loc, 1, 0, 0, 0);
            }
            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                SHOCK_PARTICLE_1.display(loc, 1, 0.2, 0.2, 0.2);
                SHOCK_PARTICLE_2.display(loc, 1, 0.2, 0.2, 0.2);
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0, 0, 0, 0);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEEHIVE_WORK, 1, 0.8F);
            }
        }
    }

    private void initializeRopeCircle() {
        double radius = stunnedEntity.getWidth() / 2;
        int points = (int) ((2 * Math.PI * radius) / 0.1);
        int pointCount = Math.min(30, points);

        for (int i = 0; i <= pointCount; i++) {
            double angle = Math.toRadians(360 * ((double) i / pointCount));
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            ropeVecs.add(new Vector(x, 0, z));
        }
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        boolean exists = getConfig().contains(CONFIG_PATH + "Enabled");
        return !exists || getConfig().getBoolean(CONFIG_PATH + "Enabled");
    }

    @Override
    public long getCooldown() {
        return isTravelling ? onMissCooldown : onHitCooldown;
    }

    @Override
    public double getCollisionRadius() {
        return collisionRadius;
    }

    @Override
    public String getName() {
        return "ElectroBola";
    }

    @Override
    public Location getLocation() {
        return isTravelling ? location : null; // no collisions when the ability has immobilized
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "KWilson272";
    }

    @Override
    public void load() {
        electroBolaListener = new ElectroBolaListener();
        Bukkit.getPluginManager().registerEvents(electroBolaListener, ProjectKorra.plugin);

        getConfig().addDefault(CONFIG_PATH + "Enabled", true);
        getConfig().addDefault(CONFIG_PATH + "OnHitCooldown", 12000);
        getConfig().addDefault(CONFIG_PATH + "OnMissCooldown", 8500);
        getConfig().addDefault(CONFIG_PATH + "Speed", 1.5);
        getConfig().addDefault(CONFIG_PATH + "Range", 25.0);
        getConfig().addDefault(CONFIG_PATH + "StunDuration", 2100);
        getConfig().addDefault(CONFIG_PATH + "AffectRadius", 1.0);
        getConfig().addDefault(CONFIG_PATH + "CollisionRadius", 1.0);
        getConfig().addDefault(CONFIG_PATH + "VerticalSensitivity", 10.0);
        getConfig().addDefault(CONFIG_PATH + "HorizontalSensitivity", 30.0);
        ConfigManager.defaultConfig.save();

        String langPath = "Abilities.Chi.ElectroBola.";
        FileConfiguration langConfig = ConfigManager.languageConfig.get();
        langConfig.addDefault(langPath + "Description", "Chiblockers can throw an " +
                "electrified bola at their enemies to immobilize them!");
        langConfig.addDefault(langPath + "Instructions", "Sneak to throw!");
        ConfigManager.languageConfig.save();
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(electroBolaListener);
    }

    public class ElectricityArc {

        private static final double LENGTH_INCREMENT = 0.15;

        private double curLength;
        private double maxLength;
        private LivingEntity entity;

        private Vector particleDir;
        private Vector axis;
        private double rotIncrement;

        private ElectricityArc(LivingEntity entity, double maxLength) {
            this.curLength = 0;
            this.maxLength = maxLength;
            this.entity = entity;

            this.particleDir = new Vector(ThreadLocalRandom.current().nextDouble(-1, 1),
                    ThreadLocalRandom.current().nextDouble(-1, 1),
                    ThreadLocalRandom.current().nextDouble(-1, 1));
            Vector arcVec = new Vector(ThreadLocalRandom.current().nextDouble(-1, 1),
                    ThreadLocalRandom.current().nextDouble(-1, 1),
                    ThreadLocalRandom.current().nextDouble(-1, 1));
            this.axis = particleDir.clone().crossProduct(arcVec).normalize();

            double spawnRadius = (entity.getWidth() + entity.getHeight()) / 3;
            this.particleDir.normalize().multiply(spawnRadius);
            this.rotIncrement = Math.toDegrees(LENGTH_INCREMENT / spawnRadius); // PK doesn't use radians .-.
        }

        private boolean draw() {
            curLength += LENGTH_INCREMENT;
            if (curLength > maxLength) {
                return false;
            }

            particleDir = GeneralMethods.rotateVectorAroundVector(axis, particleDir, rotIncrement);
            Location spawnLoc = entity.getLocation().add(0, entity.getHeight() / 2, 0);
            spawnLoc.add(particleDir);

            SHOCK_PARTICLE_1.display(spawnLoc, 1, 0, 0, 0);
            SHOCK_PARTICLE_2.display(spawnLoc, 1, 0, 0, 0);
            entity.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, spawnLoc, 1, 0, 0, 0, 0);

            entity.getWorld().playSound(entity, Sound.BLOCK_BEEHIVE_WORK, 1, 0.8F);
            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                entity.getWorld().playSound(entity, Sound.ENTITY_CREEPER_HURT, 0.3f, 0);
            }
            return true;
        }
    }
}

