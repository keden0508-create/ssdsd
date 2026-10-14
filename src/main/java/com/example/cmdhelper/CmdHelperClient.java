package com.example.cmdhelper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

public class CmdHelperClient implements ClientModInitializer {

    /** 마검 (단일 인용부호 SNBT 사용) */
    private static final String MAGUM = "give @s netherite_sword["
        + "custom_name={text:'마검 · 암흑의 칼날',color:'dark_purple',bold:true,italic:false},"
        + "lore=[{text:'어둠이 깃든 전설의 검',color:'gray',italic:false}],"
        + "enchantments={sharpness:10,fire_aspect:3,looting:5,sweeping_edge:5,unbreaking:10,mending:1},"
        + "unbreakable={},enchantment_glint_override=true,"
        + "attribute_modifiers=[{type:'attack_damage',id:'cmdhelper:magum_dmg',amount:30,operation:'add_value',slot:'mainhand'},"
        + "{type:'attack_speed',id:'cmdhelper:magum_spd',amount:-1,operation:'add_value',slot:'mainhand'}]]";

    /** 여러 명령을 연달아 실행: /이름 */
    private static final Map<String, String[]> MULTI = Map.of(
        "kit", new String[] {
            "give @s netherite_helmet[enchantments={protection:4,unbreaking:3,mending:1,respiration:3,aqua_affinity:1}]",
            "give @s netherite_chestplate[enchantments={protection:4,unbreaking:3,mending:1}]",
            "give @s netherite_leggings[enchantments={protection:4,unbreaking:3,mending:1,swift_sneak:3}]",
            "give @s netherite_boots[enchantments={protection:4,unbreaking:3,mending:1,feather_falling:4,depth_strider:3,soul_speed:3}]",
            "give @s netherite_pickaxe[enchantments={efficiency:5,unbreaking:3,fortune:3,mending:1}]",
            "give @s netherite_axe[enchantments={efficiency:5,unbreaking:3,sharpness:5,mending:1}]"
        },
        "bye", new String[] {
            "execute as @a at @s run summon lightning_bolt ~ ~ ~",
            "execute as @a at @s run summon tnt ~ ~ ~ {fuse:20}"
        }
    );

    /** 단순 별칭: /이름  →  서버 명령어 */
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("gmc", "gamemode creative"),
        Map.entry("gms", "gamemode survival"),
        Map.entry("gma", "gamemode adventure"),
        Map.entry("gmsp", "gamemode spectator"),
        Map.entry("day", "time set day"),
        Map.entry("noon", "time set noon"),
        Map.entry("night", "time set night"),
        Map.entry("sun", "weather clear"),
        Map.entry("rain", "weather rain"),
        Map.entry("keepinv", "gamerule keep_inventory true"),
        Map.entry("cll", "clear @s"),
        // ── 재미용 ──
        Map.entry("thunderall", "execute as @a at @s run summon lightning_bolt ~ ~ ~"),
        Map.entry("thunderme", "execute at @s run summon lightning_bolt ~ ~ ~"),
        Map.entry("thundermobs", "execute at @e[type=!player,distance=..40] run summon lightning_bolt ~ ~ ~"),
        Map.entry("storm", "weather thunder"),
        Map.entry("god", "effect give @s resistance infinite 255 true"),
        Map.entry("speed", "effect give @s speed infinite 5 true"),
        Map.entry("jump", "effect give @s jump_boost infinite 5 true"),
        Map.entry("nv", "effect give @s night_vision infinite 0 true"),
        Map.entry("heal", "effect give @s instant_health 1 10 true"),
        Map.entry("feed", "effect give @s saturation 1 20 true"),
        Map.entry("clearfx", "effect clear @s"),
        Map.entry("rocket", "effect give @s levitation 4 40 true"),
        Map.entry("boom", "execute at @s run summon tnt ~ ~ ~ {fuse:0}"),
        Map.entry("boomall", "execute as @a at @s run summon tnt ~ ~ ~ {fuse:0}"),
        Map.entry("rtp", "spreadplayers 0 0 100 10000 false @s"),
        Map.entry("apple", "give @s enchanted_golden_apple 64"),
        Map.entry("totem", "give @s totem_of_undying 8"),
        Map.entry("magum", MAGUM)
    );

    @Override
    public void onInitializeClient() {
        Tools.init();
        ClientCommandRegistrationCallback.EVENT.register((d, ctx) -> register(d));
    }

    private void register(CommandDispatcher<FabricClientCommandSource> d) {
        Extras.register(d);
        Tools.register(d);
        // 1) 단순 별칭
        ALIASES.forEach((name, cmd) ->
            d.register(ClientCommands.literal(name).executes(c -> { run(c.getSource(), cmd); return 1; })));

        MULTI.forEach((name, cmds) ->
            d.register(ClientCommands.literal(name).executes(cx -> {
                for (String s : cmds) run(cx.getSource(), s);
                return 1;
            })));

        // /head <닉네임> : 플레이어 머리
        d.register(ClientCommands.literal("head")
            .then(ClientCommands.argument("name", StringArgumentType.word())
                .executes(cx -> { run(cx.getSource(), "give @s player_head[profile='" + StringArgumentType.getString(cx, "name") + "']"); return 1; })));

        // /cmdhelp : 전체 명령어 목록
        d.register(ClientCommands.literal("cmdhelp").executes(cx -> {
            msg(cx.getSource(), "§6[단축] §f" + String.join(" ", ALIASES.keySet().stream().sorted().map(s -> "/" + s).toList()));
            msg(cx.getSource(), "§6[세트] §f/kit /bye");
            msg(cx.getSource(), "§6[인자] §f/tpu N, /gi 아이템 [개수], /ke [반경], /ki [반경], /head 닉네임, /here");
            msg(cx.getSource(), "§6[추가] §f/cb x y z, /cbnear, /im 아이템 모델, /imh 모델, /cmd 아이템 숫자, /gimmick, /exec, /nether, /chunk, /copyxyz, /stack, /ticks, /dice, /coin, /shrug, /tableflip, /mark /marks /tpmark /unmark");
            msg(cx.getSource(), "§6[도구] §f/again /hist /repeat N 명령 /after 10s 명령 /macro save|run|list|del /pos1 /pos2 /posvol /fillcmd /fillrun /clonecmd /sel /snip /snbt /hud /track /stopwatch /rainbow /box");
            msg(cx.getSource(), "§6[NBT] §f/nbt explain|list|comp|hand|entity|block");
            return 1;
        }));

        // 2) /tpu <칸>  : 위로 N칸 순간이동
        d.register(ClientCommands.literal("tpu")
            .then(ClientCommands.argument("n", IntegerArgumentType.integer())
                .executes(c -> { run(c.getSource(), "tp @s ~ ~" + IntegerArgumentType.getInteger(c, "n") + " ~"); return 1; })));

        // 3) /gi <아이템 [개수]>  : give @s ...
        d.register(ClientCommands.literal("gi")
            .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                .executes(c -> { run(c.getSource(), "give @s " + StringArgumentType.getString(c, "args")); return 1; })));

        // 4) /ke [반경]  주변 몹 제거 (플레이어 제외),  /ki [반경] 주변 아이템 제거
        d.register(ClientCommands.literal("ke")
            .executes(c -> { run(c.getSource(), "kill @e[type=!player,distance=..20]"); return 1; })
            .then(ClientCommands.argument("r", IntegerArgumentType.integer(1))
                .executes(c -> { run(c.getSource(), "kill @e[type=!player,distance=.." + IntegerArgumentType.getInteger(c, "r") + "]"); return 1; })));
        d.register(ClientCommands.literal("ki")
            .executes(c -> { run(c.getSource(), "kill @e[type=item,distance=..30]"); return 1; })
            .then(ClientCommands.argument("r", IntegerArgumentType.integer(1))
                .executes(c -> { run(c.getSource(), "kill @e[type=item,distance=.." + IntegerArgumentType.getInteger(c, "r") + "]"); return 1; })));

        // 5) /here : 현재 좌표·차원 표시
        d.register(ClientCommands.literal("here").executes(c -> {
            Player p = c.getSource().getPlayer();
            BlockPos b = p.blockPosition();
            msg(c.getSource(), "§e좌표: §f" + b.getX() + " " + b.getY() + " " + b.getZ()
                + " §7(" + p.level().dimension().identifier() + ")");
            return 1;
        }));

        // 6) /nbt ...
        d.register(ClientCommands.literal("nbt")
            .then(ClientCommands.literal("explain")
                .then(ClientCommands.argument("tag", StringArgumentType.greedyString())
                    .executes(c -> {
                        String k = StringArgumentType.getString(c, "tag");
                        String e = NbtDict.explain(k);
                        msg(c.getSource(), e == null ? "§c사전에 없는 태그: " + k : "§a" + k + " §f→ " + e);
                        return 1;
                    })))
            .then(ClientCommands.literal("list").executes(c -> {
                NbtDict.TAGS.forEach((k, v) -> msg(c.getSource(), "§7" + k + " §8- §f" + v));
                return 1;
            }))
            .then(ClientCommands.literal("comp")
                .executes(c -> { listComps(c.getSource(), ""); return 1; })
                .then(ClientCommands.argument("q", StringArgumentType.greedyString())
                    .executes(c -> { listComps(c.getSource(), StringArgumentType.getString(c, "q")); return 1; })))
            .then(ClientCommands.literal("hand").executes(c -> { explainHand(c.getSource()); return 1; }))
            .then(ClientCommands.literal("entity").executes(c -> {
                Entity e = Minecraft.getInstance().crosshairPickEntity;
                if (e == null) { msg(c.getSource(), "§c조준 중인 엔티티가 없습니다."); return 0; }
                run(c.getSource(), "data get entity " + e.getStringUUID());
                return 1;
            }))
            .then(ClientCommands.literal("block").executes(c -> {
                HitResult h = Minecraft.getInstance().hitResult;
                if (!(h instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) {
                    msg(c.getSource(), "§c조준 중인 블록이 없습니다."); return 0;
                }
                BlockPos p = bh.getBlockPos();
                run(c.getSource(), "data get block " + p.getX() + " " + p.getY() + " " + p.getZ());
                return 1;
            })));
    }

    private void explainHand(FabricClientCommandSource src) {
        Player p = src.getPlayer();
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) { msg(src, "§c손에 든 아이템이 없습니다."); return; }

        var ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag tag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        if (!(tag instanceof CompoundTag root)) { msg(src, "§c직렬화 실패"); return; }

        msg(src, "§6── 손에 든 아이템 ──");
        for (String top : root.keySet()) {
            if (top.equals("components")) continue;
            msg(src, "§a" + top + " §f= " + root.get(top) + explainSuffix(top));
        }
        root.getCompound("components").ifPresentOrElse(comps -> {
            if (comps.keySet().isEmpty()) msg(src, "§7(추가 컴포넌트 없음)");
            for (String k : comps.keySet()) {
                msg(src, "§b" + k + " §f= " + comps.get(k) + explainSuffix(k));
            }
        }, () -> msg(src, "§7(추가 컴포넌트 없음)"));
    }

    private void listComps(FabricClientCommandSource src, String q) {
        String f = q.toLowerCase();
        int[] n = {0};
        NbtDict.TAGS.forEach((k, v) -> {
            if (!k.startsWith("minecraft:")) return;
            if (!f.isEmpty() && !k.contains(f) && !v.contains(q)) return;
            n[0]++;
            msg(src, "§b" + k + " §8- §f" + v);
        });
        if (n[0] == 0) msg(src, "§c일치하는 구성요소가 없습니다: " + q);
    }

    private static String explainSuffix(String key) {
        String e = NbtDict.explain(key);
        return e == null ? "" : "\n  §8↳ §7" + e;
    }

    private static void run(FabricClientCommandSource src, String cmd) {
        src.getClient().getConnection().sendCommand(cmd);
    }

    private static void msg(FabricClientCommandSource src, String s) {
        src.sendFeedback(Component.literal(s));
    }
}
