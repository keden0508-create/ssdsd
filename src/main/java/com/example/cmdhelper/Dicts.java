package com.example.cmdhelper;

import java.util.LinkedHashMap;
import java.util.Map;

/** 맵 제작 기믹 사전 / execute 사전 */
public final class Dicts {
    public static final Map<String, String> GIMMICK = new LinkedHashMap<>();
    public static final Map<String, String> EXEC = new LinkedHashMap<>();

    public static final Map<String, String> SEL = new LinkedHashMap<>();
    public static final Map<String, String> SNIP = new LinkedHashMap<>();

    private static void s(String k, String v) { SEL.put(k, v); }
    private static void n(String k, String v) { SNIP.put(k, v); }
    private static void g(String k, String v) { GIMMICK.put(k, v); }
    private static void e(String k, String v) { EXEC.put(k, v); }

    static {
        // ───────── 맵 제작 기믹 ─────────
        g("마커", "눈에 안 보이는 위치 표시용 엔티티. summon marker ~ ~ ~ {Tags:[\"spawn\"]}");
        g("우클릭버튼", "interaction 엔티티로 버튼 만들기. summon interaction ~ ~ ~ {width:1f,height:1f,response:true,Tags:[\"btn\"]} → 감지: execute as @e[tag=btn] if data entity @s interaction run (실행내용) 후 data remove entity @s interaction");
        g("공중글씨", "text_display로 글자 띄우기. summon text_display ~ ~1 ~ {text:\"안녕\",billboard:\"center\"}");
        g("공중아이템", "item_display로 아이템 전시. summon item_display ~ ~1 ~ {item:{id:\"diamond\",count:1}}");
        g("공중블록", "block_display로 블록 모양 장식. summon block_display ~ ~ ~ {block_state:{Name:\"gold_block\"}}");
        g("점수판", "scoreboard objectives add 이름 dummy / scoreboard players set @s 이름 0 / add");
        g("킬수", "scoreboard objectives add kills minecraft.killed:minecraft.zombie (좀비 처치 자동 집계)");
        g("트리거", "플레이어가 OP 없이 스스로 값을 바꾸는 점수. objectives add t trigger → players enable @a t → 플레이어가 /trigger t set 1");
        g("타이머", "scoreboard players remove #timer t 1 반복 + execute if score #timer t matches 0 run (끝날 때 실행)");
        g("가짜플레이어", "#이름 처럼 가상 이름에 점수 저장해서 변수처럼 사용 (엔티티 없이)");
        g("저장소", "데이터 저장소(변수 모음). data modify storage ns:vars score set value 5");
        g("랜덤", "random value 1..6 (결과 얻기), execute store result score #r t run random value 1..10");
        g("영역감지", "execute if entity @a[x=0,y=64,z=0,dx=10,dy=5,dz=10] run ... (직육면체 범위 안에 있는지)");
        g("거리감지", "execute if entity @a[distance=..5] run ... (반경 5칸 안)");
        g("바닥블록감지", "execute as @a at @s if block ~ ~-0.5 ~ gold_block run ... (밟고 있는 블록으로 발동)");
        g("손아이템감지", "execute if items entity @s weapon.mainhand diamond_sword run ...");
        g("인벤토리감지", "execute if items entity @s container.* diamond run ... (인벤 전체에서 다이아 찾기)");
        g("아이템회수", "clear @s diamond 1 (1개 소모) / clear @s diamond 0 (보유 여부만 확인)");
        g("팀색", "team add red / team modify red color red / team join red @s (이름·발광 색)");
        g("충돌끄기", "team modify 팀 collisionRule never (몸 밀림 방지)");
        g("이름숨기기", "team modify 팀 nametagVisibility never");
        g("피아식별", "team modify 팀 friendlyFire false (같은 팀 공격 불가)");
        g("보스바", "bossbar add timer \"남은 시간\" → bossbar set minecraft:timer players @a → bossbar set minecraft:timer max 60 / value 30");
        g("타이틀", "title @a title \"시작!\" / title @a subtitle \"준비\" / title @a actionbar \"문구\"");
        g("체력바꾸기", "attribute @s max_health base set 40 (하트 20칸)");
        g("이동잠금", "effect give @s slowness infinite 255 true + effect give @s jump_boost infinite 128 true");
        g("투명인간", "effect give @s invisibility infinite 0 true");
        g("리스폰", "spawnpoint @s ~ ~ ~ (현재 위치를 리스폰 지점으로)");
        g("월드보더", "worldborder set 50 20 (20초 동안 지름 50으로 축소) / worldborder center 0 0");
        g("몹고정", "summon zombie ~ ~ ~ {NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b}");
        g("무한리필", "gamerule keep_inventory true / gamerule mob_drops false (게임룰 이름은 snake_case)");
        g("소리", "playsound minecraft:entity.player.levelup master @a ~ ~ ~ 1 1");
        g("파티클", "particle flame ~ ~1 ~ 0.3 0.3 0.3 0.02 20 (불꽃 20개)");
        g("순간이동", "tp @s 0 64 0 / tp @s ~ ~ ~ facing 0 64 0 (바라볼 방향 지정)");
        g("블록복사", "clone 시작좌표 끝좌표 대상좌표 (건물 복사), fill 으로 영역 채우기");
        g("함수", "데이터팩 function 파일로 명령어 묶기. function ns:이름 / tick 태그로 매 틱 자동 실행");
        g("조건타이머", "스코어 값에 따라 단계 진행: execute if score #stage t matches 1 run ...");
        g("폭죽연출", "summon firework_rocket ~ ~ ~ {LifeTime:20,FireworksItem:{id:\"firework_rocket\",count:1,components:{\"minecraft:fireworks\":{explosions:[{shape:\"star\",colors:[16711680]}]}}}}");
        g("전체공지", "tellraw @a {text:\"공지\",color:\"gold\"} / say 메시지");

        // ───────── execute 사전 ─────────
        e("run", "마지막에 실행할 실제 명령어. execute ... run say hi");
        e("as", "실행 주체(@s)를 바꿈. 위치는 그대로. execute as @a run ...");
        e("at", "위치·방향·차원을 대상 기준으로. execute at @s run ...");
        e("as_at", "가장 흔한 조합: execute as @a at @s run ... (각 플레이어가 자기 위치에서)");
        e("positioned", "위치만 변경. positioned 0 64 0 / positioned as @s / positioned over world_surface (지표면으로)");
        e("rotated", "방향만 변경. rotated 90 0 / rotated as @s");
        e("facing", "특정 지점/엔티티를 바라보게. facing 0 64 0 / facing entity @p eyes");
        e("align", "좌표를 블록 모서리로 내림. align xyz → 정수 좌표, 블록 중앙은 positioned ~0.5 ~ ~0.5");
        e("anchored", "기준점 선택. anchored eyes (눈) / feet (발)");
        e("in", "차원 이동 실행. in minecraft:the_nether run ...");
        e("on", "관계 엔티티: attacker, controller, leasher, origin, owner, passengers, target, vehicle. execute on attacker run ...");
        e("summon", "엔티티를 소환하고 그 엔티티를 주체로. execute summon marker run ...");
        e("if_entity", "엔티티가 있으면: execute if entity @a[distance=..5] run ...  (unless면 없으면)");
        e("if_block", "블록이 맞으면: execute if block ~ ~-1 ~ stone run ...");
        e("if_blocks", "두 영역이 같으면: if blocks 시작 끝 대상 all|masked");
        e("if_biome", "바이옴 검사: if biome ~ ~ ~ minecraft:desert");
        e("if_data", "데이터 존재 검사: if data entity @s Items[0]");
        e("if_dimension", "차원 검사: if dimension minecraft:overworld");
        e("if_function", "함수 결과 검사: if function ns:check");
        e("if_items", "슬롯 아이템 검사: if items entity @s weapon.mainhand diamond_sword");
        e("if_loaded", "청크 로드 여부: if loaded ~ ~ ~");
        e("if_predicate", "프레디케이트(조건 JSON) 검사: if predicate ns:is_night");
        e("if_score", "점수 비교: if score @s a > @s b / if score @s a matches 1..5");
        e("unless", "if의 반대. unless entity @a run ... (아무도 없으면)");
        e("store_result", "명령 결과 값을 저장: execute store result score @s hp run data get entity @s Health");
        e("store_success", "성공 여부(1/0)를 저장: store success score #ok t run ...");
        e("예시_밟기", "execute as @a at @s if block ~ ~-1 ~ gold_block run effect give @s speed 1 2");
        e("예시_공격자", "execute as @e[type=zombie] on attacker run say 때렸다");
        e("예시_범위", "execute as @a at @s as @e[type=zombie,distance=..5] run kill @s");
        e("예시_지표", "execute positioned 0 0 0 over world_surface run summon marker ~ ~ ~");
    }


    static {
        // ───────── 선택자 사전 ─────────
        s("@p", "가장 가까운 플레이어");
        s("@r", "무작위 플레이어 (type= 로 엔티티도 가능)");
        s("@a", "모든 플레이어");
        s("@e", "모든 엔티티 (살아있는 것 + 아이템 + 마커 등)");
        s("@s", "명령을 실행하는 자기 자신");
        s("type", "엔티티 종류. @e[type=zombie] / type=!player (제외) / type=#minecraft:skeletons (태그)");
        s("distance", "거리. distance=..5 (5칸 이내) / distance=3..10");
        s("x y z", "기준 좌표. @e[x=0,y=64,z=0,distance=..10]");
        s("dx dy dz", "x y z 에서 뻗는 직육면체 범위. @a[x=0,y=64,z=0,dx=10,dy=5,dz=10]");
        s("tag", "태그. @e[tag=boss] / tag=!boss / tag= (태그 없는 것)");
        s("name", "이름. @e[name=\"홍길동\"] / name=!홍길동");
        s("team", "팀. @a[team=red] / team=!red / team= (팀 없는 것)");
        s("scores", "점수. @a[scores={kills=5..}] / scores={a=1,b=..3}");
        s("nbt", "엔티티 NBT. @e[nbt={Invulnerable:1b}] (플레이어는 일부 제한)");
        s("level", "경험치 레벨. @a[level=10..]");
        s("gamemode", "게임모드. @a[gamemode=survival] / gamemode=!creative");
        s("limit", "최대 개수. @e[type=zombie,limit=3]");
        s("sort", "정렬: nearest(가까운 순) / furthest / random / arbitrary");
        s("x_rotation", "상하 시선 각도. @a[x_rotation=-90..-45] (위를 봄)");
        s("y_rotation", "좌우 방향 각도. @a[y_rotation=-45..45] (대략 남쪽)");
        s("predicate", "프레디케이트 조건. @a[predicate=ns:is_night]");
        s("advancements", "발전과제. @a[advancements={minecraft:story/mine_stone=true}]");
        s("!", "부정. 많은 인자 앞에 ! 를 붙이면 '아닌 것'");
        s("범위", "N.. (이상) / ..N (이하) / A..B (사이)");

        // ───────── 데이터팩 스니펫 ─────────
        n("경로", "data/네임스페이스/function/a.mcfunction, predicate/, loot_table/, advancement/, recipe/ (폴더명은 단수형)");
        n("tick", "매 틱 실행: data/minecraft/tags/function/tick.json → {\"values\":[\"ns:loop\"]}");
        n("load", "월드 로드 시 실행: data/minecraft/tags/function/load.json → {\"values\":[\"ns:init\"]}");
        n("predicate_밤", "{\"condition\":\"minecraft:time_check\",\"value\":{\"min\":13000,\"max\":23000},\"period\":24000}");
        n("predicate_비", "{\"condition\":\"minecraft:weather_check\",\"raining\":true}");
        n("predicate_확률", "{\"condition\":\"minecraft:random_chance\",\"chance\":0.3}");
        n("predicate_좀비", "{\"condition\":\"minecraft:entity_properties\",\"entity\":\"this\",\"predicate\":{\"type\":\"minecraft:zombie\"}}");
        n("predicate_서바이벌", "{\"condition\":\"minecraft:entity_properties\",\"entity\":\"this\",\"predicate\":{\"type_specific\":{\"type\":\"minecraft:player\",\"gamemode\":[\"survival\"]}}}");
        n("loot_table", "{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\"}]}]}  → loot give @s loot ns:이름");
        n("advancement_신호", "명령으로 줄 수 있는 발전과제: {\"criteria\":{\"x\":{\"trigger\":\"minecraft:impossible\"}},\"rewards\":{\"function\":\"ns:reward\"}} → advancement grant @s only ns:이름");
        n("recipe_무형", "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[\"minecraft:stick\"],\"result\":{\"id\":\"minecraft:diamond\",\"count\":1}}");
        n("function_매개", "function ns:f with storage ns:args  (매크로 함수: 줄 앞에 $ , 값은 $(변수))");
        n("reload", "데이터팩 수정 후 /reload");
    }

    private Dicts() {}
}
