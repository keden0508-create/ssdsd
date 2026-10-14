package com.example.cmdhelper;

import java.util.LinkedHashMap;
import java.util.Map;

/** NBT 태그 / 아이템 데이터 컴포넌트 설명 사전 */
public final class NbtDict {
    public static final Map<String, String> TAGS = new LinkedHashMap<>();

    private static void t(String k, String v) { TAGS.put(k.toLowerCase(), v); }

    static {
        // ── 공통 / 엔티티 ──
        t("id", "엔티티·아이템·블록 엔티티의 ID (예: minecraft:zombie)");
        t("Pos", "위치 [x, y, z] (double 3개)");
        t("Motion", "속도 [dx, dy, dz] (블록/틱)");
        t("Rotation", "회전 [yaw, pitch] (float 2개)");
        t("Health", "현재 체력 (float, 하트 1칸 = 2)");
        t("Air", "남은 숨 (틱, 기본 300)");
        t("Fire", "불 붙은 남은 시간 (틱, -1이면 불 안 붙음)");
        t("FallDistance", "누적 낙하 거리");
        t("OnGround", "땅 위에 있는지 (0b/1b)");
        t("Invulnerable", "무적 여부 (1b면 데미지 무시)");
        t("NoAI", "AI 비활성화 (1b면 움직이지 않음)");
        t("NoGravity", "중력 무시 (1b면 떠 있음)");
        t("Silent", "소리 비활성화");
        t("Glowing", "발광 효과 (외곽선)");
        t("PersistenceRequired", "디스폰 방지 (1b면 멀어져도 안 사라짐)");
        t("CustomName", "커스텀 이름 (텍스트 컴포넌트)");
        t("CustomNameVisible", "이름표를 항상 표시할지");
        t("Tags", "태그 목록 (@e[tag=...] 선택자용)");
        t("Passengers", "탑승 중인 엔티티 목록");
        t("UUID", "고유 식별자 (int 4개 배열)");
        t("Age", "나이/스폰 후 경과 틱 (아이템은 6000에 소멸)");
        t("PickupDelay", "아이템 엔티티 줍기 지연 (틱, 32767이면 못 줍음)");
        t("Item", "아이템 엔티티가 가진 아이템 스택");
        t("ArmorItems", "방어구 4칸 [발, 다리, 가슴, 머리] (구버전 호환)");
        t("HandItems", "손 아이템 [주손, 보조손] (구버전 호환)");
        t("Attributes", "속성 목록 (최대체력, 이동속도 등)");
        t("active_effects", "적용 중인 포션 효과 목록");
        t("Count", "아이템 개수 (1.20.5+ 에서는 count)");
        t("count", "아이템 개수");
        t("components", "아이템의 데이터 컴포넌트 묶음 (1.20.5+ 에서 NBT 대체)");
        // ── 블록 엔티티 ──
        t("Items", "컨테이너 내부 아이템 목록 (Slot 포함)");
        t("Slot", "컨테이너 칸 번호");
        t("Lock", "잠금 키 (구버전, 지금은 lock 컴포넌트)");
        t("LootTable", "열 때 채워질 전리품 테이블");
        t("LootTableSeed", "전리품 시드 (0이면 매번 랜덤)");
        t("x", "블록 엔티티 X 좌표");
        t("y", "블록 엔티티 Y 좌표");
        t("z", "블록 엔티티 Z 좌표");
        // ── 아이템 데이터 컴포넌트 ──
        t("minecraft:custom_name", "아이템 이름 (모루 이름, 기울임 없음)");
        t("minecraft:item_name", "아이템 기본 이름 (모루로 못 바꿈, 기울임 없음)");
        t("minecraft:lore", "아이템 설명(로어) 줄 목록");
        t("minecraft:damage", "소모된 내구도");
        t("minecraft:max_damage", "최대 내구도");
        t("minecraft:max_stack_size", "최대 스택 크기 (1~99)");
        t("minecraft:unbreakable", "무한 내구도");
        t("minecraft:enchantments", "인챈트 목록 {레벨}");
        t("minecraft:stored_enchantments", "인챈트 책에 저장된 인챈트");
        t("minecraft:repair_cost", "모루 수리 누적 비용");
        t("minecraft:custom_model_data", "리소스팩용 커스텀 모델 값");
        t("minecraft:attribute_modifiers", "장착 시 속성 보정 (공격력, 방어력 등)");
        t("minecraft:enchantment_glint_override", "인챈트 반짝임 강제 켜기/끄기");
        t("minecraft:potion_contents", "포션 종류·색상·효과");
        t("minecraft:food", "음식 속성 (영양, 포화도, 항상 먹기 가능)");
        t("minecraft:tool", "도구 속성 (채굴 규칙, 속도)");
        t("minecraft:can_break", "모험 모드에서 부술 수 있는 블록");
        t("minecraft:can_place_on", "모험 모드에서 설치 가능한 블록");
        t("minecraft:dyed_color", "가죽 방어구 등의 염색 색상");
        t("minecraft:profile", "플레이어 머리 소유자 프로필");
        t("minecraft:container", "셜커 상자 등 안에 든 아이템");
        t("minecraft:bundle_contents", "가방 안의 아이템");
        t("minecraft:trim", "방어구 장식(트림)");
        t("minecraft:rarity", "이름 색상을 결정하는 희귀도");
        t("minecraft:hide_tooltip", "툴팁 전체 숨김");
        t("minecraft:tooltip_display", "툴팁 일부 줄 숨김 설정");
        t("minecraft:custom_data", "자유롭게 쓰는 사용자 NBT (데이터팩·명령어용)");
        t("minecraft:entity_data", "스폰 알·아이템으로 소환할 때 엔티티 NBT");
        t("minecraft:block_entity_data", "설치 시 블록 엔티티에 적용될 NBT");
        t("minecraft:block_state", "설치 시 블록 상태 값");
        t("minecraft:lock", "컨테이너 잠금 키 아이템 이름");
        // ── 추가 아이템 구성요소 ──
        t("minecraft:additional_trade_cost", "거래 시 추가 가격 증가량");
        t("minecraft:attack_range", "공격 사거리 설정");
        t("minecraft:axolotl_variant", "아홀로틀 색상 종류");
        t("minecraft:banner_patterns", "현수막 무늬 층 목록");
        t("minecraft:base_color", "방패·현수막의 기본 염료 색");
        t("minecraft:bees", "벌집/벌통 안에 든 벌");
        t("minecraft:blocks_attacks", "방패처럼 공격을 막는 속성");
        t("minecraft:break_sound", "아이템이 부서질 때 소리");
        t("minecraft:bucket_entity_data", "양동이 속 생물의 NBT");
        t("minecraft:cat_collar", "고양이 목줄 색");
        t("minecraft:cat_variant", "고양이 무늬 종류");
        t("minecraft:charged_projectiles", "석궁에 장전된 발사체");
        t("minecraft:chicken_variant", "닭 종류");
        t("minecraft:consumable", "먹기/마시기 동작 설정 (시간, 소리, 효과)");
        t("minecraft:container_loot", "상자 아이템에 들어 있는 전리품 테이블");
        t("minecraft:cow_variant", "소 종류");
        t("minecraft:creative_slot_lock", "크리에이티브 인벤토리에서 칸 고정");
        t("minecraft:damage_resistant", "특정 피해 유형에 면역 (불, 용암 등)");
        t("minecraft:death_protection", "죽음 방지 효과 (불사의 토템 같은 동작)");
        t("minecraft:debug_stick_state", "디버그 막대로 선택한 블록 상태");
        t("minecraft:enchantable", "인챈트 가능 여부와 인챈트 가능 수치");
        t("minecraft:equippable", "장착 가능한 칸·소리·모델 설정");
        t("minecraft:firework_explosion", "폭죽 별 하나의 폭발 모양");
        t("minecraft:fireworks", "폭죽 로켓 비행 시간과 폭발 목록");
        t("minecraft:fox_variant", "여우 종류");
        t("minecraft:frog_variant", "개구리 종류");
        t("minecraft:glider", "겉날개처럼 활공 가능");
        t("minecraft:horse_variant", "말 털 색");
        t("minecraft:instrument", "염소 뿔 소리 종류");
        t("minecraft:intangible_projectile", "발사체가 물건을 줍지 못하는 상태");
        t("minecraft:item_model", "사용할 아이템 모델 지정");
        t("minecraft:jukebox_playable", "주크박스 재생 곡 지정");
        t("minecraft:kinetic_weapon", "운동 에너지(돌진) 공격 속성");
        t("minecraft:llama_variant", "라마 카펫 색/종류");
        t("minecraft:lodestone_tracker", "나침반이 가리키는 자석석 위치");
        t("minecraft:map_color", "지도 테두리 색");
        t("minecraft:map_decorations", "지도 위 표시(깃발, 마커)");
        t("minecraft:map_id", "지도 번호");
        t("minecraft:minimum_attack_charge", "공격이 적용되는 최소 충전 비율");
        t("minecraft:mooshroom_variant", "무시룸 종류 (빨강/갈색)");
        t("minecraft:note_block_sound", "머리 아이템이 음 블록에 내는 소리");
        t("minecraft:ominous_bottle_amplifier", "불길한 병 효과 단계");
        t("minecraft:painting_variant", "그림 종류");
        t("minecraft:parrot_variant", "앵무새 색");
        t("minecraft:piercing_weapon", "찌르기 공격 속성");
        t("minecraft:pig_variant", "돼지 종류");
        t("minecraft:potion_duration_scale", "포션 지속시간 배율");
        t("minecraft:provides_banner_patterns", "현수막 무늬로 쓸 수 있는 아이템 태그");
        t("minecraft:provides_trim_material", "방어구 장식 재료로 쓰일 때의 재질");
        t("minecraft:rabbit_variant", "토끼 종류");
        t("minecraft:recipes", "지식 책이 해금하는 조리법 목록");
        t("minecraft:repairable", "수리 재료로 쓸 수 있는 아이템");
        t("minecraft:salmon_size", "연어 크기");
        t("minecraft:sheep_color", "양 색");
        t("minecraft:shulker_color", "셜커 색");
        t("minecraft:suspicious_stew_effects", "의심스러운 스튜 효과 목록");
        t("minecraft:swing_animation", "휘두르는 동작 종류");
        t("minecraft:tooltip_style", "툴팁 배경/테두리 스타일");
        t("minecraft:tropical_fish_base_color", "열대어 몸 색");
        t("minecraft:tropical_fish_pattern", "열대어 무늬");
        t("minecraft:tropical_fish_pattern_color", "열대어 무늬 색");
        t("minecraft:use_cooldown", "사용 후 재사용 대기 시간");
        t("minecraft:use_effects", "사용 중 이동 속도 등 효과 설정");
        t("minecraft:use_remainder", "사용 후 남는 아이템 (예: 꿀병 → 빈 병)");
        t("minecraft:villager_variant", "주민 바이옴 종류");
        t("minecraft:weapon", "무기 속성 (방패 무력화, 내구도 소모 등)");
        t("minecraft:wolf_collar", "늑대 목줄 색");
        t("minecraft:wolf_sound_variant", "늑대 소리 종류");
        t("minecraft:wolf_variant", "늑대 종류");
        t("minecraft:writable_book_content", "책과 깃펜의 페이지 내용");
        t("minecraft:written_book_content", "서명된 책의 제목·저자·페이지·세대");
    }

    private NbtDict() {}

    public static String explain(String key) {
        String s = TAGS.get(key.toLowerCase());
        if (s != null) return s;
        if (!key.contains(":")) s = TAGS.get("minecraft:" + key.toLowerCase());
        return s;
    }
}
