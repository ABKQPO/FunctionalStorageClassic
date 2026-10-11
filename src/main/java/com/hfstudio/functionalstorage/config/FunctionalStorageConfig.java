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
    @Config.Order(1)
    public static final General GENERAL = new General();

    @Config.Comment("Storage capacity tuning; reopen the world after changing these values")
    @Config.RequiresWorldRestart
    @Config.Order(2)
    public static final Storage STORAGE = new Storage();

    @Config.Comment("Upgrade automation tuning")
    @Config.Order(3)
    public static final Upgrades UPGRADES = new Upgrades();

    @Config.Comment("Optional mod integrations")
    @Config.Order(4)
    public static final Compatibility COMPATIBILITY = new Compatibility();

    @Config.Comment("Client side behaviour")
    @Config.Order(5)
    public static final Client CLIENT = new Client();

    @Config.Comment("Adaptive storage and RF behaviour")
    @Config.RequiresMcRestart
    @Config.Order(6)
    public static final Adaptive ADAPTIVE = new Adaptive();

    public static class General {

        @Config.Comment("Keep stored contents, filters, lock state, and upgrades in the dropped block when broken")
        @Config.DefaultBoolean(true)
        @Config.Order(1)
        public boolean keepContentsOnBreak = true;

        @Config.Comment("Armory cabinet slot amount")
        @Config.RangeInt(min = 1, max = 8192)
        @Config.DefaultInt(4096)
        @Config.Order(2)
        public int armoryCabinetSize = 4096;

        @Config.Comment("Linking range radius of the storage controller in blocks")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(8)
        @Config.Order(3)
        public int drawerControllerLinkingRange = 8;

        @Config.Comment("Maximum number of drawers a single storage controller will link")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(512)
        @Config.Order(4)
        public int controllerLinkLimit = 512;

        @Config.Comment("Register the configured extra compacting rules")
        @Config.DefaultBoolean(true)
        @Config.Order(5)
        public boolean registerExtraCompactingRules = true;

        @Config.Comment({ "Additional compacting rules in the form: higher item, lower item, ratio.",
            "Items must use modid:name or modid:name:meta. Example: minecraft:clay, minecraft:clay_ball, 4" })
        @Config.Order(6)
        public String[] extraCompactingRules = { "minecraft:clay, minecraft:clay_ball, 4" };

        @Config.Comment("Ore dictionary names that storage matching must never match")
        @Config.Order(7)
        public String[] oreDictionaryBlacklist = {};

        @Config.Comment("Ore dictionary names allowed for storage matching. Empty allows every non-blacklisted name")
        @Config.Order(8)
        public String[] oreDictionaryWhitelist = {};
    }

    public static class Storage {

        @Config.Comment("Copper Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(8)
        @Config.Order(1)
        public int copperMultiplier = 8;

        @Config.Comment("Gold Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(16)
        @Config.Order(2)
        public int goldMultiplier = 16;

        @Config.Comment("Diamond Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(24)
        @Config.Order(3)
        public int diamondMultiplier = 24;

        @Config.Comment("Netherite Upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(32)
        @Config.Order(4)
        public int netheriteMultiplier = 32;

        @Config.Comment("Turtle scute upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(48)
        @Config.Order(5)
        public int turtleScuteMultiplier = 48;

        @Config.Comment("Nether star upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(72)
        @Config.Order(6)
        public int netherStarMultiplier = 72;

        @Config.Comment("Heavy core upgrade storage multiplier")
        @Config.RangeInt(min = 1, max = 1024)
        @Config.DefaultInt(128)
        @Config.Order(7)
        public int heavyCoreMultiplier = 128;

        @Config.Comment("Fluid capacity divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(2)
        @Config.Order(8)
        public int fluidDivisor = 2;

        @Config.Comment("Essentia capacity divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        @Config.Order(9)
        public int aspectDivisor = 4;

        @Config.Comment("Controller range divisor applied to storage upgrades")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        @Config.Order(10)
        public int rangeDivisor = 4;

        @Config.Comment("Base capacity of a single drawer slot before upgrades")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(32)
        @Config.Order(11)
        public int baseItemCapacity = 32;

        @Config.Comment("Base capacity of a single fluid tank before upgrades")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(32000)
        @Config.Order(12)
        public int baseFluidCapacity = 32000;

        @Config.Comment("Base capacity of a single essentia slot before upgrades")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(256)
        @Config.Order(13)
        public int baseAspectCapacity = 256;
    }

    public static class Upgrades {

        @Config.Comment("Ticks removed from each operation by an installed speed augment")
        @Config.RangeInt(min = 0, max = 200)
        @Config.DefaultInt(10)
        @Config.Order(1)
        public int speedAugmentReduction = 10;

        @Config.Comment("Maximum number of speed augments that can be installed in one upgrade")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(6)
        @Config.Order(2)
        public int maxSpeedAugments = 6;

        @Config.Comment("Maximum number of efficiency augments that can be installed in one upgrade")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(8)
        @Config.Order(3)
        public int maxEfficiencyAugments = 8;

        @Config.Comment("Every how many ticks drawer upgrades run")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(4)
        @Config.Order(4)
        public int upgradeTick = 4;

        @Config.Comment("How many items the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        @Config.Order(5)
        public int upgradePullItems = 4;

        @Config.Comment("How much fluid the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        @Config.Order(6)
        public int upgradePullFluid = 500;

        @Config.Comment("How many items the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        @Config.Order(7)
        public int upgradePushItems = 4;

        @Config.Comment("How much fluid the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        @Config.Order(8)
        public int upgradePushFluid = 500;

        @Config.Comment("How many items the collector upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(4)
        @Config.Order(9)
        public int upgradeCollectorItems = 4;

        @Config.Comment("How much fluid the collector upgrade will try to collect")
        @Config.RangeInt(min = 1, max = 10000)
        @Config.DefaultInt(500)
        @Config.Order(10)
        public int upgradeCollectorFluid = 500;

        @Config.Comment("How many essentia units the pulling upgrade will try to pull")
        @Config.RangeInt(min = 1, max = 1000)
        @Config.DefaultInt(8)
        @Config.Order(11)
        public int upgradePullAspect = 8;

        @Config.Comment("How many essentia units the pushing upgrade will try to push")
        @Config.RangeInt(min = 1, max = 1000)
        @Config.DefaultInt(8)
        @Config.Order(12)
        public int upgradePushAspect = 8;

        @Config.Comment("Stone Generation Upgrade tier 1 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(1)
        @Config.Order(13)
        public int stoneGenerationTier1 = 1;

        @Config.Comment("Stone Generation Upgrade tier 2 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(2)
        @Config.Order(14)
        public int stoneGenerationTier2 = 2;

        @Config.Comment("Stone Generation Upgrade tier 3 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(4)
        @Config.Order(15)
        public int stoneGenerationTier3 = 4;

        @Config.Comment("Stone Generation Upgrade tier 4 generation rate")
        @Config.RangeInt(min = 1, max = 4096)
        @Config.DefaultInt(8)
        @Config.Order(16)
        public int stoneGenerationTier4 = 8;

        @Config.Comment("Stone Generation Upgrade tier 5 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(16)
        @Config.Order(17)
        public int stoneGenerationTier5 = 16;

        @Config.Comment("Ticks between stone generator operations")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(18)
        public int stoneGenerationInterval = 1;

        @Config.Comment("Water Generation Upgrade tier 1 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1000)
        @Config.Order(19)
        public int waterGenerationTier1 = 1000;

        @Config.Comment("Water Generation Upgrade tier 2 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(2000)
        @Config.Order(20)
        public int waterGenerationTier2 = 2000;

        @Config.Comment("Water Generation Upgrade tier 3 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(4000)
        @Config.Order(21)
        public int waterGenerationTier3 = 4000;

        @Config.Comment("Water Generation Upgrade tier 4 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(8000)
        @Config.Order(22)
        public int waterGenerationTier4 = 8000;

        @Config.Comment("Water Generation Upgrade tier 5 generation rate")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(16000)
        @Config.Order(23)
        public int waterGenerationTier5 = 16000;

        @Config.Comment("Ticks between water generator operations")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(24)
        public int waterGenerationInterval = 1;

        @Config.Comment("Generated item amount for universal generator tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(25)
        public int universalGenerationTier1 = 1;

        @Config.Comment("Generated item amount for universal generator tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(26)
        public int universalGenerationTier2 = 1;

        @Config.Comment("Generated item amount for universal generator tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(27)
        public int universalGenerationTier3 = 1;

        @Config.Comment("Generated item amount for universal generator tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(28)
        public int universalGenerationTier4 = 1;

        @Config.Comment("Generated item amount for universal generator tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(2)
        @Config.Order(29)
        public int universalGenerationTier5 = 2;

        @Config.Comment("Ticks between universal item generation attempts")
        @Config.RangeInt(min = 1, max = 200)
        @Config.DefaultInt(1)
        @Config.Order(30)
        public int universalGenerationTick = 1;

        @Config.Comment("Whether universal item generation uses the configured global item")
        @Config.DefaultBoolean(false)
        @Config.Order(31)
        public boolean universalGenerationRegistered = false;

        @Config.Comment("Item produced by universal generation upgrades when registered generation is enabled")
        @Config.DefaultString("minecraft:sand")
        @Config.Order(32)
        public String universalGenerationItem = "minecraft:sand";

        @Config.Comment("Generated item amount for resource generator tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(8)
        @Config.Order(33)
        public int resourceGenerationRateTier1 = 8;

        @Config.Comment("Generated item amount for resource generator tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(16)
        @Config.Order(34)
        public int resourceGenerationRateTier2 = 16;

        @Config.Comment("Generated item amount for resource generator tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(32)
        @Config.Order(35)
        public int resourceGenerationRateTier3 = 32;

        @Config.Comment("Generated item amount for resource generator tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(64)
        @Config.Order(36)
        public int resourceGenerationRateTier4 = 64;

        @Config.Comment("Generated item amount for resource generator tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(64)
        @Config.Order(37)
        public int resourceGenerationRateTier5 = 64;

        @Config.Comment("Ticks between standard resource generator operations at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(20)
        @Config.Order(38)
        public int resourceGenerationIntervalTier1 = 20;

        @Config.Comment("Ticks between standard resource generator operations at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(15)
        @Config.Order(39)
        public int resourceGenerationIntervalTier2 = 15;

        @Config.Comment("Ticks between standard resource generator operations at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(10)
        @Config.Order(40)
        public int resourceGenerationIntervalTier3 = 10;

        @Config.Comment("Ticks between standard resource generator operations at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(5)
        @Config.Order(41)
        public int resourceGenerationIntervalTier4 = 5;

        @Config.Comment("Ticks between standard resource generator operations at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(42)
        public int resourceGenerationIntervalTier5 = 1;

        @Config.Comment("Ticks between obsidian generator operations at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(300)
        @Config.Order(43)
        public int obsidianGenerationIntervalTier1 = 300;

        @Config.Comment("Ticks between obsidian generator operations at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(80)
        @Config.Order(44)
        public int obsidianGenerationIntervalTier2 = 80;

        @Config.Comment("Ticks between obsidian generator operations at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(20)
        @Config.Order(45)
        public int obsidianGenerationIntervalTier3 = 20;

        @Config.Comment("Ticks between obsidian generator operations at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(5)
        @Config.Order(46)
        public int obsidianGenerationIntervalTier4 = 5;

        @Config.Comment("Ticks between obsidian generator operations at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(47)
        public int obsidianGenerationIntervalTier5 = 1;

        @Config.Comment("Generated obsidian amount per operation at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1)
        @Config.Order(48)
        public int obsidianGenerationRateTier1 = 1;

        @Config.Comment("Generated obsidian amount per operation at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(16)
        @Config.Order(49)
        public int obsidianGenerationRateTier2 = 16;

        @Config.Comment("Generated obsidian amount per operation at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(32)
        @Config.Order(50)
        public int obsidianGenerationRateTier3 = 32;

        @Config.Comment("Generated obsidian amount per operation at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(64)
        @Config.Order(51)
        public int obsidianGenerationRateTier4 = 64;

        @Config.Comment("Generated obsidian amount per operation at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(64)
        @Config.Order(52)
        public int obsidianGenerationRateTier5 = 64;

        @Config.Comment("Generated lava amount per operation at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(20)
        @Config.Order(53)
        public int lavaGenerationRateTier1 = 20;

        @Config.Comment("Generated lava amount per operation at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(250)
        @Config.Order(54)
        public int lavaGenerationRateTier2 = 250;

        @Config.Comment("Generated lava amount per operation at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1000)
        @Config.Order(55)
        public int lavaGenerationRateTier3 = 1000;

        @Config.Comment("Generated lava amount per operation at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(2500)
        @Config.Order(56)
        public int lavaGenerationRateTier4 = 2500;

        @Config.Comment("Generated lava amount per operation at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(5000)
        @Config.Order(57)
        public int lavaGenerationRateTier5 = 5000;

        @Config.Comment("Generated milk amount per operation at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(50)
        @Config.Order(58)
        public int milkGenerationRateTier1 = 50;

        @Config.Comment("Generated milk amount per operation at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(250)
        @Config.Order(59)
        public int milkGenerationRateTier2 = 250;

        @Config.Comment("Generated milk amount per operation at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1000)
        @Config.Order(60)
        public int milkGenerationRateTier3 = 1000;

        @Config.Comment("Generated milk amount per operation at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(2500)
        @Config.Order(61)
        public int milkGenerationRateTier4 = 2500;

        @Config.Comment("Generated milk amount per operation at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(5000)
        @Config.Order(62)
        public int milkGenerationRateTier5 = 5000;

        @Config.Comment("Generated honey amount per operation at tier 1")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(25)
        @Config.Order(63)
        public int honeyGenerationRateTier1 = 25;

        @Config.Comment("Generated honey amount per operation at tier 2")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(100)
        @Config.Order(64)
        public int honeyGenerationRateTier2 = 100;

        @Config.Comment("Generated honey amount per operation at tier 3")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(250)
        @Config.Order(65)
        public int honeyGenerationRateTier3 = 250;

        @Config.Comment("Generated honey amount per operation at tier 4")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(500)
        @Config.Order(66)
        public int honeyGenerationRateTier4 = 500;

        @Config.Comment("Generated honey amount per operation at tier 5")
        @Config.RangeInt(min = 1, max = 1000000)
        @Config.DefaultInt(1000)
        @Config.Order(67)
        public int honeyGenerationRateTier5 = 1000;

        @Config.Comment("Ticks between breaker upgrade operations")
        @Config.RangeInt(min = 1, max = 2000)
        @Config.DefaultInt(60)
        @Config.Order(68)
        public int breakerTick = 60;

        @Config.Comment("Ticks between placer upgrade operations")
        @Config.RangeInt(min = 1, max = 2000)
        @Config.DefaultInt(60)
        @Config.Order(69)
        public int placerTick = 60;

        @Config.Comment("Ticks between refill upgrade operations")
        @Config.RangeInt(min = 1, max = 2000)
        @Config.DefaultInt(60)
        @Config.Order(70)
        public int refillTick = 60;
    }

    public static class Compatibility {

        @Config.Comment("Enable the Waila integration for drawers and controllers")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        @Config.Order(1)
        public boolean enableWailaCompatibility = true;

        @Config.Comment("Enable the Applied Energistics 2 storage bridge for drawers and controllers")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        @Config.Order(2)
        public boolean enableAE2Compatibility = true;

        @Config.Comment("Enable Thaumcraft essentia storage and the essentia drawer")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        @Config.Order(3)
        public boolean enableThaumcraftCompatibility = true;

        @Config.Comment("Let a FindIt search reach the drawers linked to a found storage controller")
        @Config.DefaultBoolean(true)
        @Config.RequiresMcRestart
        @Config.Order(4)
        public boolean enableFindItCompatibility = true;

        @Config.Comment("Highlight only linked drawers containing the item found by a FindIt search")
        @Config.DefaultBoolean(true)
        @Config.Order(5)
        public boolean highlightLinkedDrawersOnFind = true;
    }

    public static class Client {

        @Config.Comment("Render block items on drawer faces in three dimensions; disable for flat icons")
        @Config.DefaultBoolean(true)
        @Config.Order(1)
        public boolean threeDimensionalBlockDisplay = true;

        @Config.Comment("Distance in blocks at which drawer contents stop rendering")
        @Config.RangeInt(min = 1, max = 256)
        @Config.DefaultInt(16)
        @Config.Order(2)
        public int drawerRenderRange = 16;

        @Config.Comment("Render the numeric amount on drawer faces by default")
        @Config.DefaultBoolean(true)
        @Config.Order(3)
        public boolean defaultShowItemCount = true;

        @Config.Comment("Render the stored item or fluid icon on drawer faces by default")
        @Config.DefaultBoolean(true)
        @Config.Order(4)
        public boolean defaultShowItemRender = true;

        @Config.Comment("Render installed upgrade icons on drawer faces by default")
        @Config.DefaultBoolean(true)
        @Config.Order(5)
        public boolean defaultShowUpgrades = true;

        @Config.Comment("Prioritize depositing filled fluid containers during controller and BogoSorter drop-off")
        @Config.DefaultBoolean(true)
        @Config.Order(6)
        public boolean prioritizeFluidContainerDeposit = true;
    }

    public static class Adaptive {

        @Config.Comment("Register Adaptive Functional Storage content")
        @Config.DefaultBoolean(true)
        @Config.Order(1)
        public boolean enabled = true;

        @Config.Comment("Enable RF power consumption for adaptive matrices when CoFHCore is installed")
        @Config.DefaultBoolean(true)
        @Config.Order(2)
        public boolean enableRfPower = true;

        @Config.Comment("Maximum linking range of an adaptive matrix in blocks")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(24)
        @Config.Order(3)
        public int linkingRange = 24;

        @Config.Comment("RF buffer capacity of an adaptive matrix")
        @Config.RangeInt(min = 1, max = 2147483647)
        @Config.DefaultInt(1000000)
        @Config.Order(4)
        public int energyCapacity = 1000000;

        @Config.Comment("Maximum RF received by an adaptive matrix in one operation")
        @Config.RangeInt(min = 1, max = 2147483647)
        @Config.DefaultInt(1000000)
        @Config.Order(5)
        public int maxEnergyReceive = 1000000;

        @Config.Comment("Base RF consumed by an adaptive matrix per tick")
        @Config.RangeInt(min = 0, max = 2147483647)
        @Config.DefaultInt(20)
        @Config.Order(6)
        public int baseEnergyCost = 20;

        @Config.Comment("Additional RF consumed per loaded item drawer per tick")
        @Config.RangeInt(min = 0, max = 2147483647)
        @Config.DefaultInt(5)
        @Config.Order(7)
        public int itemDrawerEnergyCost = 5;

        @Config.Comment("Maximum resource entries in one terminal synchronization packet")
        @Config.RangeInt(min = 1, max = 64)
        @Config.DefaultInt(16)
        @Config.Order(8)
        public int terminalEntriesPerPacket = 16;

        @Config.Comment("Maximum terminal synchronization packets sent per viewer per tick")
        @Config.RangeInt(min = 1, max = 16)
        @Config.DefaultInt(2)
        @Config.Order(9)
        public int terminalPacketsPerTick = 2;
    }
}
