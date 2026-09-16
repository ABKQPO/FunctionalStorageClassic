package com.hfstudio.functionalstorage.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hfstudio.functionalstorage.FunctionalStorage;

@Config(
    modid = FunctionalStorage.MOD_ID,
    filename = FunctionalStorage.MOD_ID,
    configSubDirectory = FunctionalStorage.MOD_ID,
    category = "")
@Config.LangKeyPattern(pattern = "functionalstorage.gui.config.%cat.%field", fullyQualified = true)
@Config.Comment("Functional Storage configuration")
public class FunctionalStorageConfig {

    public static void registerConfig() throws ConfigException {
        ConfigurationManager.registerConfig(FunctionalStorageConfig.class);
    }

    @Config.Comment("General behaviour")
    public static final General GENERAL = new General();

    @Config.Comment("Storage capacity tuning")
    public static final Storage STORAGE = new Storage();

    @Config.Comment("Upgrade automation tuning")
    public static final Upgrades UPGRADES = new Upgrades();

    @Config.Comment("Optional mod integrations")
    public static final Compatibility COMPATIBILITY = new Compatibility();

    @Config.Comment("Client side behaviour")
    @Config.RequiresMcRestart
    public static final Client CLIENT = new Client();

    public static class General {

        @Config.Comment("Keep stored contents, filters, lock state, and upgrades in the dropped block when broken")
        @Config.DefaultBoolean(true)
        public boolean keepContentsOnBreak = true;

        @Config.Comment("Armory cabinet slot amount")
        @Config.RangeInt(min = 1, max = 8192)
        @Config.DefaultInt(4096)
        public int armoryCabinetSize = 4096;

        @Config.Comment("Linking range radius of the storage controller in blocks")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(8)
        public int drawerControllerLinkingRange = 8;

        @Config.Comment("Maximum number of drawers a single storage controller will link")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(512)
        public int controllerLinkLimit = 512;

        @Config.Comment("Register the configured extra compacting rules")
        @Config.DefaultBoolean(true)
        public boolean registerExtraCompactingRules = true;

        @Config.Comment({ "Additional compacting rules in the form: higher item, lower item, ratio.",
            "Items must use modid:name or modid:name:meta. Example: minecraft:clay, minecraft:clay_ball, 4" })
        public String[] extraCompactingRules = { "minecraft:clay, minecraft:clay_ball, 4" };

        @Config.Comment("Ore dictionary names the Ore Dictionary Upgrade must never match")
        public String[] oreDictionaryBlacklist = {};

        @Config.Comment("Ore dictionary names the Ore Dictionary Upgrade may match. Empty allows every non-blacklisted name")
        public String[] oreDictionaryWhitelist = {};
    }

    public static class Storage {

        @Config.Comment("Copper Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(8)
        public int copperMultiplier = 8;

        @Config.Comment("Gold Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(16)
        public int goldMultiplier = 16;

        @Config.Comment("Diamond Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(24)
        public int diamondMultiplier = 24;

        @Config.Comment("Netherite Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(32)
        public int netheriteMultiplier = 32;

        @Config.Comment("Fluid capacity divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(2)
        public int fluidDivisor = 2;

        @Config.Comment("Essentia capacity divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        public int aspectDivisor = 4;

        @Config.Comment("Controller range divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        public int rangeDivisor = 4;

        @Config.Comment("Base capacity of a single drawer slot before upgrades")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(32)
        public int baseItemCapacity = 32;

        @Config.Comment("Base capacity of a single fluid tank in millibuckets before upgrades")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(32000)
        public int baseFluidCapacity = 32000;

        @Config.Comment("Base capacity of a single essentia slot before upgrades")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(256)
        public int baseAspectCapacity = 256;
    }

    public static class Upgrades {

        @Config.Comment("Every how many ticks drawer upgrades run")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(4)
        public int upgradeTick = 4;

        @Config.Comment("How many items the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        public int upgradePullItems = 4;

        @Config.Comment("How much fluid in millibuckets the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        public int upgradePullFluid = 500;

        @Config.Comment("How many items the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        public int upgradePushItems = 4;

        @Config.Comment("How much fluid in millibuckets the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        public int upgradePushFluid = 500;

        @Config.Comment("How many items the collector upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        public int upgradeCollectorItems = 4;

        @Config.Comment("How much fluid in millibuckets the collector upgrade will try to collect")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        public int upgradeCollectorFluid = 500;

        @Config.Comment("How many esssentia units the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 1000)
        @Config.DefaultInt(8)
        public int upgradePullAspect = 8;

        @Config.Comment("How many essentia units the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 1000)
        @Config.DefaultInt(8)
        public int upgradePushAspect = 8;

        @Config.Comment("Stone Generation Upgrade tier 1 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(1)
        public int stoneGenerationTier1 = 1;

        @Config.Comment("Stone Generation Upgrade tier 2 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(2)
        public int stoneGenerationTier2 = 2;

        @Config.Comment("Stone Generation Upgrade tier 3 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(4)
        public int stoneGenerationTier3 = 4;

        @Config.Comment("Stone Generation Upgrade tier 4 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(8)
        public int stoneGenerationTier4 = 8;

        @Config.Comment("Water Generation Upgrade tier 1 generation rate in millibuckets")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1000)
        public int waterGenerationTier1 = 1000;

        @Config.Comment("Water Generation Upgrade tier 2 generation rate in millibuckets")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(2000)
        public int waterGenerationTier2 = 2000;

        @Config.Comment("Water Generation Upgrade tier 3 generation rate in millibuckets")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(4000)
        public int waterGenerationTier3 = 4000;

        @Config.Comment("Water Generation Upgrade tier 4 generation rate in millibuckets")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(8000)
        public int waterGenerationTier4 = 8000;

        @Config.Comment("Ticks between universal item generation attempts")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(1)
        public int universalGenerationTick = 1;

        @Config.Comment("Whether universal item generation uses the configured global item")
        @Config.DefaultBoolean(false)
        public boolean universalGenerationRegistered = false;

        @Config.Comment("Item produced by universal generation upgrades when registered generation is enabled")
        @Config.DefaultString("minecraft:sand")
        public String universalGenerationItem = "minecraft:sand";

        @Config.Comment("Ticks between stonecutter upgrade operations")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(20)
        public int stonecuttingTick = 20;

        @Config.Comment("Ticks between breaker upgrade operations")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(20)
        public int breakerTick = 20;

        @Config.Comment("Ticks between placer upgrade operations")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(20)
        public int placerTick = 20;

        @Config.Comment("Ticks between refill upgrade operations")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(20)
        public int refillTick = 20;
    }

    public static class Compatibility {

        @Config.Comment("Enable the Waila integration for drawers and controllers")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        public boolean enableWailaCompatibility = true;

        @Config.Comment("Enable the Applied Energistics 2 storage bridge for drawers and controllers")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        public boolean enableAE2Compatibility = true;

        @Config.Comment("Enable Thaumcraft essentia storage and the essentia drawer")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        public boolean enableThaumcraftCompatibility = true;
    }

    public static class Client {

        @Config.Comment("Distance in blocks at which drawer contents stop rendering")
        @Config.RangeInt(min = 1, max = 128)
        @Config.DefaultInt(16)
        public int drawerRenderRange = 16;

        @Config.Comment("Render the numeric amount on drawer faces by default")
        @Config.DefaultBoolean(true)
        public boolean defaultShowItemCount = true;

        @Config.Comment("Render the stored item or fluid icon on drawer faces by default")
        @Config.DefaultBoolean(true)
        public boolean defaultShowItemRender = true;

        @Config.Comment("Render installed upgrade icons on drawer faces by default")
        @Config.DefaultBoolean(true)
        public boolean defaultShowUpgrades = true;
    }
}
