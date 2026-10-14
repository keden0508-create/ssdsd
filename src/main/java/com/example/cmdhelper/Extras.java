package com.example.cmdhelper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 커맨드블록 열기 / item_model / 사전 / 잡다한 편의 기능 */
public final class Extras {
    record Mark(double x, double y, double z, String dim) {}
    static final Map<String, Mark> MARKS = new LinkedHashMap<>();
    private static final Random RNG = new Random();

    private Extras() {}

    static void register(CommandDispatcher<FabricClientCommandSource> d) {
        // ── 1) 커맨드 블록 열기 ──
        d.register(ClientCommands.literal("cb")
            .executes(c -> {
                HitResult h = Minecraft.getInstance().hitResult;
                if (h instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK)
                    return openCb(c.getSource(), bh.getBlockPos());
                msg(c.getSource(), "§c조준 중인 블록이 없습니다. /cb x y z 로 좌표를 적어 주세요.");
                return 0;
            })
            .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                    .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                        .executes(c -> openCb(c.getSource(), new BlockPos(
                            IntegerArgumentType.getInteger(c, "x"),
                            IntegerArgumentType.getInteger(c, "y"),
                            IntegerArgumentType.getInteger(c, "z"))))))));

        d.register(ClientCommands.literal("cbnear").executes(c -> {
            Minecraft mc = c.getSource().getClient();
            BlockPos origin = mc.player.blockPosition();
            BlockPos best = null; double bd = 1e9;
            for (int dx = -5; dx <= 5; dx++) for (int dy = -5; dy <= 5; dy++) for (int dz = -5; dz <= 5; dz++) {
                BlockPos p = origin.offset(dx, dy, dz);
                if (!isCmdBlock(mc.level.getBlockState(p))) continue;
                double dist = mc.player.distanceToSqr(Vec3.atCenterOf(p));
                if (dist < bd) { bd = dist; best = p; }
            }
            if (best == null) { msg(c.getSource(), "§c근처 5칸 안에 커맨드 블록이 없습니다."); return 0; }
            return openCb(c.getSource(), best);
        }));

        // ── 2) item_model / custom_model_data 빠르게 give ──
        d.register(ClientCommands.literal("im")
            .then(ClientCommands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                String[] a = StringArgumentType.getString(c, "args").trim().split("\\s+");
                if (a.length < 2) { msg(c.getSource(), "§e사용법: /im 아이템 모델 [개수]  예) /im stick minecraft:diamond_sword"); return 0; }
                run(c.getSource(), "give @s " + a[0] + "[item_model='" + a[1] + "'] " + (a.length > 2 ? a[2] : "1"));
                return 1;
            })));
        d.register(ClientCommands.literal("imh")
            .then(ClientCommands.argument("model", StringArgumentType.greedyString()).executes(c -> {
                ItemStack st = c.getSource().getPlayer().getMainHandItem();
                if (st.isEmpty()) { msg(c.getSource(), "§c손에 든 아이템이 없습니다."); return 0; }
                String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                run(c.getSource(), "give @s " + id + "[item_model='" + StringArgumentType.getString(c, "model").trim() + "']");
                return 1;
            })));
        d.register(ClientCommands.literal("cmd")
            .then(ClientCommands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                String[] a = StringArgumentType.getString(c, "args").trim().split("\\s+");
                if (a.length < 2) { msg(c.getSource(), "§e사용법: /cmd 아이템 숫자  예) /cmd stick 1"); return 0; }
                run(c.getSource(), "give @s " + a[0] + "[custom_model_data={floats:[" + a[1] + "f]}]");
                return 1;
            })));

        // ── 3,4) 사전 ──
        d.register(dictCmd("gimmick", Dicts.GIMMICK, "맵 제작 기믹"));
        d.register(dictCmd("exec", Dicts.EXEC, "execute"));

        // ── 잡다한 편의 ──
        d.register(ClientCommands.literal("nether").executes(c -> {
            Player p = c.getSource().getPlayer();
            msg(c.getSource(), "§e현재: §f" + f(p.getX()) + " " + f(p.getY()) + " " + f(p.getZ()));
            msg(c.getSource(), "§d→ 네더에서: §f" + f(p.getX() / 8) + " " + f(p.getY()) + " " + f(p.getZ() / 8));
            msg(c.getSource(), "§a→ 오버월드에서: §f" + f(p.getX() * 8) + " " + f(p.getY()) + " " + f(p.getZ() * 8));
            return 1;
        }));
        d.register(ClientCommands.literal("chunk").executes(c -> {
            BlockPos b = c.getSource().getPlayer().blockPosition();
            int cx = b.getX() >> 4, cz = b.getZ() >> 4;
            msg(c.getSource(), "§e청크: §f" + cx + ", " + cz + " §7(청크 안 위치 " + (b.getX() & 15) + ", " + (b.getZ() & 15) + ")");
            msg(c.getSource(), "§e리전 파일: §fr." + (cx >> 5) + "." + (cz >> 5) + ".mca");
            return 1;
        }));
        d.register(ClientCommands.literal("copyxyz").executes(c -> {
            BlockPos b = c.getSource().getPlayer().blockPosition();
            c.getSource().getClient().keyboardHandler.setClipboard(b.getX() + " " + b.getY() + " " + b.getZ());
            msg(c.getSource(), "§a좌표를 클립보드에 복사했습니다.");
            return 1;
        }));
        d.register(ClientCommands.literal("stack")
            .then(ClientCommands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                String[] a = StringArgumentType.getString(c, "args").trim().split("\\s+");
                try {
                    int n = Integer.parseInt(a[0]);
                    int max = a.length > 1 ? Integer.parseInt(a[1]) : 64;
                    msg(c.getSource(), "§e" + n + "개 = §f" + (n / max) + "스택 + " + (n % max) + "개 §7(스택 " + max + " 기준)");
                } catch (Exception ex) { msg(c.getSource(), "§e사용법: /stack 개수 [스택크기]"); }
                return 1;
            })));
        d.register(ClientCommands.literal("ticks")
            .then(ClientCommands.argument("v", StringArgumentType.greedyString()).executes(c -> {
                Matcher m = Pattern.compile("([0-9.]+)\\s*([tsmh]?)").matcher(StringArgumentType.getString(c, "v").trim());
                if (!m.matches()) { msg(c.getSource(), "§e사용법: /ticks 5s | 100t | 2m | 1h"); return 0; }
                double n = Double.parseDouble(m.group(1));
                double t = switch (m.group(2)) { case "t" -> n; case "m" -> n * 1200; case "h" -> n * 72000; default -> n * 20; };
                msg(c.getSource(), "§f" + f(t) + "틱 = " + f(t / 20) + "초 = " + f(t / 1200) + "분 §7(마크 하루 24000틱)");
                return 1;
            })));
        d.register(ClientCommands.literal("dice")
            .executes(c -> { run(c.getSource(), "random roll 1..6"); return 1; })
            .then(ClientCommands.argument("n", IntegerArgumentType.integer(2))
                .executes(c -> { run(c.getSource(), "random roll 1.." + IntegerArgumentType.getInteger(c, "n")); return 1; })));
        d.register(ClientCommands.literal("coin").executes(c -> {
            msg(c.getSource(), RNG.nextBoolean() ? "§e🪙 앞면!" : "§e🪙 뒷면!");
            return 1;
        }));
        d.register(ClientCommands.literal("shrug").executes(c -> {
            c.getSource().getClient().getConnection().sendChat("¯\\_(ツ)_/¯"); return 1;
        }));
        d.register(ClientCommands.literal("tableflip").executes(c -> {
            c.getSource().getClient().getConnection().sendChat("(╯°□°)╯︵ ┻━┻"); return 1;
        }));

        // ── 위치 북마크 (게임 끄면 사라짐) ──
        d.register(ClientCommands.literal("mark")
            .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                Player p = c.getSource().getPlayer();
                String n = StringArgumentType.getString(c, "name");
                MARKS.put(n, new Mark(p.getX(), p.getY(), p.getZ(), p.level().dimension().identifier().toString()));
                msg(c.getSource(), "§a'" + n + "' 위치를 저장했습니다.");
                return 1;
            })));
        d.register(ClientCommands.literal("marks").executes(c -> {
            if (MARKS.isEmpty()) msg(c.getSource(), "§7저장된 위치가 없습니다. /mark 이름");
            MARKS.forEach((k, v) -> msg(c.getSource(), "§b" + k + " §f" + f(v.x) + " " + f(v.y) + " " + f(v.z) + " §7(" + v.dim + ")"));
            return 1;
        }));
        d.register(ClientCommands.literal("tpmark")
            .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                Mark m = MARKS.get(StringArgumentType.getString(c, "name"));
                if (m == null) { msg(c.getSource(), "§c그 이름의 위치가 없습니다."); return 0; }
                run(c.getSource(), "tp @s " + m.x + " " + m.y + " " + m.z);
                return 1;
            })));
        d.register(ClientCommands.literal("unmark")
            .then(ClientCommands.literal("all").executes(c -> {
                MARKS.clear();
                msg(c.getSource(), "§7저장된 위치를 모두 지웠습니다.");
                return 1;
            }))
            .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                MARKS.remove(StringArgumentType.getString(c, "name"));
                msg(c.getSource(), "§7삭제했습니다.");
                return 1;
            })));
    }

    // ───────── helpers ─────────
    static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> dictCmd(
            String name, Map<String, String> dict, String title) {
        return ClientCommands.literal(name)
            .executes(c -> {
                msg(c.getSource(), "§6[" + title + " 사전] §7/" + name + " 검색어 로 설명 보기");
                msg(c.getSource(), "§f" + String.join(", ", dict.keySet()));
                return 1;
            })
            .then(ClientCommands.argument("q", StringArgumentType.greedyString()).executes(c -> {
                String q = StringArgumentType.getString(c, "q").trim().toLowerCase();
                int[] n = {0};
                dict.forEach((k, v) -> {
                    if (k.toLowerCase().contains(q) || v.toLowerCase().contains(q)) {
                        n[0]++;
                        msg(c.getSource(), "§b" + k + " §8→ §f" + v);
                    }
                });
                if (n[0] == 0) msg(c.getSource(), "§c검색 결과가 없습니다: " + q);
                return 1;
            }));
    }

    private static boolean isCmdBlock(BlockState s) {
        return s.is(Blocks.COMMAND_BLOCK) || s.is(Blocks.CHAIN_COMMAND_BLOCK) || s.is(Blocks.REPEATING_COMMAND_BLOCK);
    }

    private static int openCb(FabricClientCommandSource s, BlockPos pos) {
        Minecraft mc = s.getClient();
        if (!isCmdBlock(mc.level.getBlockState(pos))) {
            msg(s, "§c그 좌표는 커맨드 블록이 아닙니다 (로드된 곳이어야 함)."); return 0;
        }
        if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 30) {
            msg(s, "§c너무 멉니다. 약 5칸 안으로 가까이 가세요 (서버 상호작용 거리 제한)."); return 0;
        }
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        msg(s, "§a커맨드 블록 열기 요청 (크리에이티브 + OP 필요, 웅크리기 해제)");
        return 1;
    }

    static String f(double v) { return String.format("%.1f", v); }
    static void run(FabricClientCommandSource s, String cmd) { s.getClient().getConnection().sendCommand(cmd); }
    static void msg(FabricClientCommandSource s, String m) { s.sendFeedback(Component.literal(m)); }
}
