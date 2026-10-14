package com.example.cmdhelper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 명령 되풀이/지연/매크로, 좌표 도구, HUD, 스톱워치 등 */
public final class Tools {
    private record Pending(long due, String cmd) {}

    private static long tick = 0;
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final List<String> HISTORY = new ArrayList<>();
    private static final Map<String, String> MACROS = new LinkedHashMap<>();
    private static BlockPos pos1, pos2;
    private static boolean hudOn = false;
    private static String trackName = null;
    private static long watchStart = -1;     // nanoTime, -1이면 정지
    private static long watchLast = 0;       // 마지막 랩 기준 (nanoTime)

    private Tools() {}

    // ═════════════ 초기화 (이벤트) ═════════════
    static void init() {
        loadMacros();

        ClientSendMessageEvents.COMMAND.register(cmd -> {
            if (HISTORY.isEmpty() || !HISTORY.get(HISTORY.size() - 1).equals(cmd)) HISTORY.add(cmd);
            if (HISTORY.size() > 200) HISTORY.remove(0);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (mc.player == null || mc.getConnection() == null) return;

            Iterator<Pending> it = PENDING.iterator();
            List<String> due = new ArrayList<>();
            while (it.hasNext()) {
                Pending p = it.next();
                if (p.due <= tick) { due.add(p.cmd); it.remove(); }
            }
            for (String c : due) mc.getConnection().sendCommand(c);

            if (tick % 2 == 0) overlay(mc);
        });
    }

    private static void overlay(Minecraft mc) {
        StringBuilder sb = new StringBuilder();
        if (hudOn) {
            BlockPos b = mc.player.blockPosition();
            int bl = mc.level.getBrightness(LightLayer.BLOCK, b);
            int sk = mc.level.getBrightness(LightLayer.SKY, b);
            sb.append("§e").append(b.getX()).append(' ').append(b.getY()).append(' ').append(b.getZ())
              .append(" §7| §f").append(dirKo(mc.player.getDirection().getName()))
              .append(" §7| §6빛 ").append(bl).append("/").append(sk);
        }
        if (trackName != null) {
            Extras.Mark m = Extras.MARKS.get(trackName);
            if (m == null) trackName = null;
            else {
                double dx = m.x() - mc.player.getX(), dy = m.y() - mc.player.getY(), dz = m.z() - mc.player.getZ();
                if (sb.length() > 0) sb.append(" §7| ");
                sb.append("§b").append(trackName).append(" §f").append(Extras.f(Math.sqrt(dx * dx + dy * dy + dz * dz))).append("m");
            }
        }
        if (watchStart >= 0) {
            if (sb.length() > 0) sb.append(" §7| ");
            sb.append("§a⏱ ").append(fmtTime(System.nanoTime() - watchStart));
        }
        if (sb.length() > 0) sendOverlay(mc, Component.literal(sb.toString()));
    }

    // 버전마다 액션바 메서드 이름이 달라서, 실행 중에 알맞은 메서드를 찾아 씁니다.
    private static java.lang.reflect.Method ovMethod;
    private static boolean ovOnPlayer, ovTwoArgs, ovSearched;

    private static void sendOverlay(Minecraft mc, Component c) {
        try {
            if (!ovSearched) { ovSearched = true; findOverlay(mc); }
            if (ovMethod == null) return;
            Object target = ovOnPlayer ? mc.player : mc.gui;
            if (ovTwoArgs) ovMethod.invoke(target, c, ovOnPlayer); else ovMethod.invoke(target, c);
        } catch (Throwable t) {
            ovMethod = null;
        }
    }

    private static void findOverlay(Minecraft mc) {
        Object[] targets = { mc.gui, mc.player };
        for (int i = 0; i < targets.length; i++) {
            Object t = targets[i];
            if (t == null) continue;
            List<java.lang.reflect.Method> all = new ArrayList<>();
            for (java.lang.reflect.Method m : t.getClass().getMethods()) all.add(m);
            for (java.lang.reflect.Method m : t.getClass().getDeclaredMethods()) all.add(m);
            for (java.lang.reflect.Method m : all) {
                String n = m.getName().toLowerCase();
                boolean nameOk = n.contains("overlaymessage") || n.contains("actionbar") || (i == 1 && n.equals("displayclientmessage"));
                if (!nameOk) continue;
                Class<?>[] p = m.getParameterTypes();
                boolean one = p.length == 1 && p[0] == Component.class;
                boolean two = p.length == 2 && p[0] == Component.class && p[1] == boolean.class;
                if (!one && !two) continue;
                try { m.setAccessible(true); } catch (Throwable ignored) { continue; }
                ovMethod = m; ovOnPlayer = (i == 1); ovTwoArgs = two;
                return;
            }
        }
    }

    private static String dirKo(String n) {
        return switch (n) { case "north" -> "북(-Z)"; case "south" -> "남(+Z)"; case "east" -> "동(+X)"; case "west" -> "서(-X)"; default -> n; };
    }

    private static String fmtTime(long ns) {
        double s = ns / 1_000_000_000.0;
        return String.format("%d:%04.1f", (int) (s / 60), s % 60);
    }

    // ═════════════ 명령어 등록 ═════════════
    static void register(CommandDispatcher<FabricClientCommandSource> d) {
        // ── 되풀이 / 반복 / 지연 ──
        d.register(ClientCommands.literal("again").executes(c -> {
            if (HISTORY.isEmpty()) { msg(c, "§7기록된 명령어가 없습니다."); return 0; }
            String last = HISTORY.get(HISTORY.size() - 1);
            msg(c, "§7→ /" + last);
            Extras.run(c.getSource(), last);
            return 1;
        }));
        d.register(ClientCommands.literal("hist")
            .executes(c -> { showHist(c.getSource(), ""); return 1; })
            .then(ClientCommands.literal("clear").executes(c -> { HISTORY.clear(); msg(c, "§7명령어 기록을 모두 지웠습니다."); return 1; }))
            .then(ClientCommands.argument("q", StringArgumentType.greedyString())
                .executes(c -> { showHist(c.getSource(), StringArgumentType.getString(c, "q")); return 1; })));
        d.register(ClientCommands.literal("repeat")
            .then(ClientCommands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                String a = StringArgumentType.getString(c, "args").trim();
                int sp = a.indexOf(' ');
                if (sp < 0) { msg(c, "§e사용법: /repeat 횟수 명령어  예) /repeat 5 say hi"); return 0; }
                int n;
                try { n = Integer.parseInt(a.substring(0, sp)); } catch (Exception ex) { msg(c, "§c횟수는 숫자로 쓰세요."); return 0; }
                n = Math.max(1, Math.min(n, 100));
                String cmd = strip(a.substring(sp + 1));
                for (int i = 0; i < n; i++) Extras.run(c.getSource(), cmd);
                msg(c, "§a" + n + "번 실행했습니다.");
                return 1;
            })));
        d.register(ClientCommands.literal("after")
            .then(ClientCommands.literal("clear").executes(c -> { PENDING.clear(); msg(c, "§7예약된 명령을 모두 취소했습니다."); return 1; }))
            .then(ClientCommands.argument("args", StringArgumentType.greedyString()).executes(c -> {
                String a = StringArgumentType.getString(c, "args").trim();
                int sp = a.indexOf(' ');
                long t = sp < 0 ? -1 : parseTicks(a.substring(0, sp));
                if (t < 0) { msg(c, "§e사용법: /after 10s 명령어  (t=틱, s=초, m=분)"); return 0; }
                PENDING.add(new Pending(tick + Math.max(1, t), strip(a.substring(sp + 1))));
                msg(c, "§a" + a.substring(0, sp) + " 뒤에 실행 예약 (취소: /after clear)");
                return 1;
            })));

        // ── 매크로 ──
        d.register(ClientCommands.literal("macro")
            .then(ClientCommands.literal("save")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .then(ClientCommands.argument("cmds", StringArgumentType.greedyString()).executes(c -> {
                        String n = StringArgumentType.getString(c, "name");
                        MACROS.put(n, StringArgumentType.getString(c, "cmds").trim());
                        saveMacros();
                        msg(c, "§a매크로 '" + n + "' 저장 (실행: /macro run " + n + ")");
                        return 1;
                    }))))
            .then(ClientCommands.literal("run")
                .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                    String body = MACROS.get(StringArgumentType.getString(c, "name"));
                    if (body == null) { msg(c, "§c그런 매크로가 없습니다. /macro list"); return 0; }
                    long delay = 0;
                    for (String part : body.split(";")) {
                        String cmd = strip(part.trim());
                        if (cmd.isEmpty()) continue;
                        if (cmd.startsWith("wait ")) {
                            long t = parseTicks(cmd.substring(5).trim());
                            if (t > 0) delay += t;
                            continue;
                        }
                        if (delay == 0) Extras.run(c.getSource(), cmd);
                        else PENDING.add(new Pending(tick + delay, cmd));
                    }
                    return 1;
                })))
            .then(ClientCommands.literal("clear").executes(c -> {
                int n = MACROS.size();
                MACROS.clear();
                saveMacros();
                msg(c, "§7매크로 " + n + "개를 모두 지웠습니다.");
                return 1;
            }))
            .then(ClientCommands.literal("list").executes(c -> {
                if (MACROS.isEmpty()) msg(c, "§7저장된 매크로 없음. /macro save 이름 명령1; 명령2; wait 2s; 명령3");
                MACROS.forEach((k, v) -> msg(c, "§b" + k + " §8→ §f" + v));
                return 1;
            }))
            .then(ClientCommands.literal("del")
                .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                    MACROS.remove(StringArgumentType.getString(c, "name"));
                    saveMacros();
                    msg(c, "§7삭제했습니다.");
                    return 1;
                }))));

        // ── 좌표 도구 (pos1/pos2/fill/clone) ──
        for (int i = 1; i <= 2; i++) {
            final int idx = i;
            d.register(ClientCommands.literal("pos" + i)
                .executes(c -> { setPos(c.getSource(), idx, null); return 1; })
                .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                    .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                        .then(ClientCommands.argument("z", IntegerArgumentType.integer()).executes(c -> {
                            setPos(c.getSource(), idx, new BlockPos(
                                IntegerArgumentType.getInteger(c, "x"),
                                IntegerArgumentType.getInteger(c, "y"),
                                IntegerArgumentType.getInteger(c, "z")));
                            return 1;
                        })))));
        }
        d.register(ClientCommands.literal("posclear").executes(c -> { pos1 = null; pos2 = null; msg(c, "§7pos1, pos2를 지웠습니다."); return 1; }));
        d.register(ClientCommands.literal("posvol").executes(c -> {
            if (!bothSet(c.getSource())) return 0;
            long dx = Math.abs(pos1.getX() - pos2.getX()) + 1, dy = Math.abs(pos1.getY() - pos2.getY()) + 1, dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;
            msg(c, "§e" + dx + " × " + dy + " × " + dz + " = §f" + (dx * dy * dz) + "칸 §7(fill/clone 기본 한도 32768)");
            return 1;
        }));
        d.register(ClientCommands.literal("fillcmd")
            .then(ClientCommands.argument("block", StringArgumentType.greedyString()).executes(c -> {
                if (!bothSet(c.getSource())) return 0;
                String cmd = "fill " + xyz(pos1) + " " + xyz(pos2) + " " + StringArgumentType.getString(c, "block").trim();
                c.getSource().getClient().keyboardHandler.setClipboard("/" + cmd);
                msg(c, "§a클립보드에 복사: §f/" + cmd);
                return 1;
            })));
        d.register(ClientCommands.literal("fillrun")
            .then(ClientCommands.argument("block", StringArgumentType.greedyString()).executes(c -> {
                if (!bothSet(c.getSource())) return 0;
                Extras.run(c.getSource(), "fill " + xyz(pos1) + " " + xyz(pos2) + " " + StringArgumentType.getString(c, "block").trim());
                return 1;
            })));
        d.register(ClientCommands.literal("clonecmd").executes(c -> {
            if (!bothSet(c.getSource())) return 0;
            BlockPos me = c.getSource().getPlayer().blockPosition();
            String cmd = "clone " + xyz(pos1) + " " + xyz(pos2) + " " + xyz(me);
            c.getSource().getClient().keyboardHandler.setClipboard("/" + cmd);
            msg(c, "§a클립보드에 복사 (붙여넣을 위치 = 내 발 위치): §f/" + cmd);
            return 1;
        }));

        // ── 사전 ──
        d.register(Extras.dictCmd("sel", Dicts.SEL, "선택자"));
        d.register(Extras.dictCmd("snip", Dicts.SNIP, "데이터팩 스니펫"));

        // ── SNBT 검사기 ──
        d.register(ClientCommands.literal("snbt")
            .then(ClientCommands.argument("s", StringArgumentType.greedyString()).executes(c -> {
                String r = checkSnbt(StringArgumentType.getString(c, "s"));
                msg(c, r == null ? "§a괄호와 따옴표 짝이 맞습니다." : "§c" + r);
                return 1;
            })));

        // ── HUD / 거리 추적 / 스톱워치 ──
        d.register(ClientCommands.literal("hud").executes(c -> {
            hudOn = !hudOn;
            msg(c, hudOn ? "§aHUD 켬 (좌표 | 방향 | 빛)" : "§7HUD 끔");
            return 1;
        }));
        d.register(ClientCommands.literal("track")
            .then(ClientCommands.literal("off").executes(c -> { trackName = null; msg(c, "§7추적 종료"); return 1; }))
            .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                String n = StringArgumentType.getString(c, "name");
                if (!Extras.MARKS.containsKey(n)) { msg(c, "§c먼저 /mark " + n + " 으로 위치를 저장하세요."); return 0; }
                trackName = n;
                msg(c, "§a'" + n + "' 까지의 거리를 화면에 표시합니다. (/track off 로 종료)");
                return 1;
            })));
        d.register(ClientCommands.literal("stopwatch")
            .then(ClientCommands.literal("start").executes(c -> { watchStart = System.nanoTime(); watchLast = watchStart; msg(c, "§a스톱워치 시작"); return 1; }))
            .then(ClientCommands.literal("lap").executes(c -> {
                if (watchStart < 0) { msg(c, "§7시작 전입니다. /stopwatch start"); return 0; }
                long now = System.nanoTime();
                msg(c, "§e랩 §f" + fmtTime(now - watchLast) + " §7(누적 " + fmtTime(now - watchStart) + ")");
                watchLast = now;
                return 1;
            }))
            .then(ClientCommands.literal("stop").executes(c -> {
                if (watchStart < 0) { msg(c, "§7시작 전입니다."); return 0; }
                msg(c, "§e기록: §f" + fmtTime(System.nanoTime() - watchStart));
                watchStart = -1;
                return 1;
            })));

        // ── 글자 꾸미기 ──
        d.register(ClientCommands.literal("rainbow")
            .then(ClientCommands.argument("text", StringArgumentType.greedyString()).executes(c -> {
                String[] colors = {"red", "gold", "yellow", "green", "aqua", "blue", "light_purple"};
                String t = StringArgumentType.getString(c, "text").replace("\"", "").replace("\\", "");
                StringBuilder sb = new StringBuilder("tellraw @a [");
                int i = 0;
                for (int k = 0; k < t.length(); k++) {
                    char ch = t.charAt(k);
                    if (i > 0) sb.append(',');
                    sb.append("{text:\"").append(ch).append("\",color:\"").append(colors[i % colors.length]).append("\"}");
                    if (ch != ' ') i++; else i++;
                }
                sb.append(']');
                Extras.run(c.getSource(), sb.toString());
                return 1;
            })));
        d.register(ClientCommands.literal("box")
            .then(ClientCommands.argument("text", StringArgumentType.greedyString()).executes(c -> {
                String t = StringArgumentType.getString(c, "text").trim();
                String bar = "━".repeat(t.length() + 2);
                var conn = c.getSource().getClient().getConnection();
                conn.sendChat("┏" + bar + "┓");
                conn.sendChat("┃ " + t + " ┃");
                conn.sendChat("┗" + bar + "┛");
                return 1;
            })));
    }

    // ═════════════ helpers ═════════════
    private static void showHist(FabricClientCommandSource s, String q) {
        int shown = 0;
        for (int i = HISTORY.size() - 1; i >= 0 && shown < 10; i--) {
            String h = HISTORY.get(i);
            if (q.isEmpty() || h.toLowerCase().contains(q.toLowerCase())) { Extras.msg(s, "§7/§f" + h); shown++; }
        }
        if (shown == 0) Extras.msg(s, "§7일치하는 기록이 없습니다. (서버로 보낸 명령어만 기록됩니다)");
    }

    private static void setPos(FabricClientCommandSource s, int idx, BlockPos given) {
        Minecraft mc = s.getClient();
        BlockPos p = given;
        if (p == null) {
            if (mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) p = bh.getBlockPos();
            else p = mc.player.blockPosition();
        }
        if (idx == 1) pos1 = p; else pos2 = p;
        Extras.msg(s, "§aPOS" + idx + " = §f" + xyz(p));
    }

    private static boolean bothSet(FabricClientCommandSource s) {
        if (pos1 == null || pos2 == null) { Extras.msg(s, "§c/pos1 과 /pos2 를 먼저 지정하세요. (조준한 블록, 없으면 발 위치)"); return false; }
        return true;
    }

    private static String xyz(BlockPos p) { return p.getX() + " " + p.getY() + " " + p.getZ(); }

    private static String strip(String c) { return c.startsWith("/") ? c.substring(1) : c; }

    /** "10s" / "5t" / "2m" → 틱. 실패하면 -1 */
    static long parseTicks(String v) {
        Matcher m = Pattern.compile("([0-9.]+)\\s*([tsmh]?)").matcher(v.trim());
        if (!m.matches()) return -1;
        double n = Double.parseDouble(m.group(1));
        return (long) switch (m.group(2)) { case "t" -> n; case "m" -> n * 1200; case "h" -> n * 72000; default -> n * 20; };
    }

    private static String checkSnbt(String s) {
        Deque<Character> st = new ArrayDeque<>();
        boolean inStr = false; char q = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (inStr) {
                if (ch == '\\') i++;
                else if (ch == q) inStr = false;
                continue;
            }
            if (ch == '\'' || ch == '"') { inStr = true; q = ch; continue; }
            if (ch == '{' || ch == '[' || ch == '(') st.push(ch);
            else if (ch == '}' || ch == ']' || ch == ')') {
                if (st.isEmpty()) return "위치 " + (i + 1) + ": 짝이 없는 닫는 괄호 '" + ch + "'";
                char o = st.pop();
                if ((o == '{' && ch != '}') || (o == '[' && ch != ']') || (o == '(' && ch != ')'))
                    return "위치 " + (i + 1) + ": 여는 괄호 '" + o + "' 와 닫는 괄호 '" + ch + "' 가 맞지 않습니다";
            } else if (ch == ',') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) == ' ') j++;
                if (j < s.length() && (s.charAt(j) == '}' || s.charAt(j) == ']'))
                    return "위치 " + (i + 1) + ": 닫기 직전의 쉼표 (버전에 따라 오류가 날 수 있음)";
            }
        }
        if (inStr) return "따옴표(" + q + ")가 닫히지 않았습니다";
        if (!st.isEmpty()) return "닫히지 않은 괄호가 " + st.size() + "개 있습니다: " + st;
        return null;
    }

    private static void msg(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> c, String m) {
        Extras.msg(c.getSource(), m);
    }

    // ── 매크로 파일 저장/로드 ──
    private static Path macroFile() { return FabricLoader.getInstance().getConfigDir().resolve("cmdhelper_macros.txt"); }

    private static void loadMacros() {
        try {
            Path f = macroFile();
            if (!Files.exists(f)) return;
            for (String line : Files.readAllLines(f)) {
                int t = line.indexOf('\t');
                if (t > 0) MACROS.put(line.substring(0, t), line.substring(t + 1));
            }
        } catch (Exception ignored) {}
    }

    private static void saveMacros() {
        try {
            StringBuilder sb = new StringBuilder();
            MACROS.forEach((k, v) -> sb.append(k).append('\t').append(v.replace("\n", " ")).append('\n'));
            Files.writeString(macroFile(), sb.toString());
        } catch (Exception ignored) {}
    }
}
