package com.aether.thrones

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AetherThronesApp()
        }
    }
}

// ===================== نماذج البيانات والإعدادات =====================
data class PlayerData(
    var coins: Int = 500,
    var unlockedLevel: Int = 1,
    var unitLevels: MutableMap<String, Int> = mutableMapOf(
        "guardian" to 1,
        "archer" to 1,
        "mage" to 1,
        "ninja" to 1
    ),
    var isArabic: Boolean = true,
    var soundEnabled: Bool = true
)

typealias Bool = Boolean

data class GameUnit(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    var x: Float, // الموقع على الخط الأفقي (0 إلى 1000)
    val isPlayer: Bool,
    var hp: Float,
    val maxHp: Float,
    val damage: Float,
    val speed: Float,
    val range: Float,
    val cost: Int,
    val color: Color
)

// ===================== واجهة اللعبة الرئيسية =====================
@Composable
fun AetherThronesApp() {
    val context = LocalContext.current
    var playerData by remember { mutableStateOf(loadPlayerData(context)) }
    var currentScreen by remember { mutableStateOf("splash") }
    var selectedLevel by remember { mutableStateOf(1) }

    // حفظ تلقائي عند التغيير
    LaunchedEffect(playerData) {
        savePlayerData(context, playerData)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF09090B))
                )
            )
    ) {
        when (currentScreen) {
            "splash" -> SplashScreen(onFinish = { currentScreen = "menu" })
            "menu" -> MainMenuScreen(
                playerData = playerData,
                onNavigate = { screen -> currentScreen = screen }
            )
            "levels" -> LevelSelectScreen(
                playerData = playerData,
                onSelectLevel = { lvl ->
                    selectedLevel = lvl
                    currentScreen = "battle"
                },
                onBack = { currentScreen = "menu" }
            )
            "units" -> UnitsScreen(
                playerData = playerData,
                onUpdate = { playerData = it },
                onBack = { currentScreen = "menu" }
            )
            "settings" -> SettingsScreen(
                playerData = playerData,
                onUpdate = { playerData = it },
                onBack = { currentScreen = "menu" }
            )
            "battle" -> BattleScreen(
                level = selectedLevel,
                playerData = playerData,
                onFinishBattle = { won, earnedCoins ->
                    if (won) {
                        playerData.coins += earnedCoins
                        if (selectedLevel >= playerData.unlockedLevel && selectedLevel < 10) {
                            playerData.unlockedLevel = selectedLevel + 1
                        }
                    }
                    currentScreen = "levels"
                },
                onBack = { currentScreen = "levels" }
            )
        }
    }
}

// ===================== شاشة التحميل =====================
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinish()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))),
                contentAlignment = Alignment.Center
            ) {
                Text("⚔️", fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "عروش الأثير",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aether Thrones",
                color = Color(0xFFA78BFA),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = Color(0xFF38BDF8))
        }
    }
}

// ===================== القائمة الرئيسية =====================
@Composable
fun MainMenuScreen(playerData: PlayerData, onNavigate: (String) -> Unit) {
    val isAr = playerData.isArabic
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isAr) "عروش الأثير" else "Aether Thrones",
            color = Color(0xFFF43F5E),
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = if (isAr) "ملحمة السحر والاستراتيجية" else "Fantasy Strategy Epic",
            color = Color(0xFF94A3B8),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(text = if (isAr) "⚔️ ابدأ المعركة (الحملة)" else "⚔️ Start Campaign") {
            onNavigate("levels")
        }
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = if (isAr) "🛡️ ترقية الوحدات" else "🛡️ Upgrade Units") {
            onNavigate("units")
        }
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = if (isAr) "⚙️ الإعدادات" else "⚙️ Settings") {
            onNavigate("settings")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🪙 ${playerData.coins}", color = Color(0xFFFBBF24), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ===================== شاشة اختيار المراحل (1-10) =====================
@Composable
fun LevelSelectScreen(playerData: PlayerData, onSelectLevel: (Int) -> Unit, onBack: () -> Unit) {
    val isAr = playerData.isArabic
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("⬅️", color = Color.White, fontSize = 20.sp)
            }
            Text(
                text = if (isAr) "اختر المرحلة" else "Select Level",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(10) { index ->
                val levelNum = index + 1
                val isUnlocked = levelNum <= playerData.unlockedLevel
                val isBoss = levelNum == 3 || levelNum == 6 || levelNum == 10

                Card(
                    onClick = { if (isUnlocked) onSelectLevel(levelNum) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${if (isAr) "المرحلة" else "Level"} $levelNum",
                                color = if (isUnlocked) Color.White else Color.Gray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isBoss) {
                                Text(
                                    text = if (isAr) "🔥 معركة زعيم ملحمية" else "🔥 Epic Boss Battle",
                                    color = Color(0xFFF43F5E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = if (isUnlocked) "▶️" else "🔒",
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ===================== شاشة الوحدات والترقية =====================
@Composable
fun UnitsScreen(playerData: PlayerData, onUpdate: (PlayerData) -> Unit, onBack: () -> Unit) {
    val isAr = playerData.isArabic
    val unitsInfo = listOf(
        Triple("guardian", if (isAr) "حارس الرون" else "Rune Guardian", 100),
        Triple("archer", if (isAr) "رامية النجوم" else "Star Archer", 120),
        Triple("mage", if (isAr) "ساحر الشرر" else "Spark Mage", 150),
        Triple("ninja", if (isAr) "نينجا الظل" else "Shadow Ninja", 200)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Text("⬅️", color = Color.White, fontSize = 20.sp) }
            Text(
                text = if (isAr) "ترقية الوحدات" else "Upgrade Units",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text("🪙 ${playerData.coins}", color = Color(0xFFFBBF24), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(unitsInfo) { (id, name, baseCost) ->
                val lvl = playerData.unitLevels[id] ?: 1
                val upgradeCost = baseCost * lvl

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${if (isAr) "المستوى" else "Level"}: $lvl", color = Color(0xFF38BDF8), fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                if (playerData.coins >= upgradeCost) {
                                    playerData.coins -= upgradeCost
                                    playerData.unitLevels[id] = lvl + 1
                                    onUpdate(playerData.copy())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playerData.coins >= upgradeCost) Color(0xFF10B981) else Color.Gray
                            )
                        ) {
                            Text("⬆️ $upgradeCost")
                        }
                    }
                }
            }
        }
    }
}

// ===================== شاشة الإعدادات =====================
@Composable
fun SettingsScreen(playerData: PlayerData, onUpdate: (PlayerData) -> Unit, onBack: () -> Unit) {
    val isAr = playerData.isArabic
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Text("⬅️", color = Color.White, fontSize = 20.sp) }
            Text(
                text = if (isAr) "الإعدادات" else "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (isAr) "اللغة / Language" else "Language / اللغة", color = Color.White, fontSize = 18.sp)
            Button(
                onClick = {
                    playerData.isArabic = !playerData.isArabic
                    onUpdate(playerData.copy())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text(if (isAr) "العربية 🇸🇦" else "English 🇬🇧")
            }
        }
    }
}

// ===================== شاشة المعركة الرئيسية (Battle Screen) =====================
@Composable
fun BattleScreen(
    level: Int,
    playerData: PlayerData,
    onFinishBattle: (Bool, Int) -> Unit,
    onBack: () -> Unit
) {
    val isAr = playerData.isArabic
    val isBossLevel = level == 3 || level == 6 || level == 10

    // حالة البلورات والموارد
    var playerCrystalHp by remember { mutableStateOf(1000f) }
    var enemyCrystalHp by remember { mutableStateOf(1000f + (level * 200f)) }
    var mana by remember { mutableStateOf(50f) }
    var stormCooldown by remember { mutableStateOf(0f) }

    // قائمة الوحدات النشطة في ساحة القتال
    val units = remember { mutableStateListOf<GameUnit>() }

    // حالة انتهاء المعركة
    var battleResult by remember { mutableStateOf<Bool?>(null) } // true: win, false: lose

    // حلقة توليد المانا والذكاء الاصطناعي وحركة المعركة
    LaunchedEffect(Unit) {
        while (battleResult == null) {
            delay(100)
            // توليد المانا تلقائياً
            if (mana < 100f) mana = (mana + 1.5f).coerceAtMost(100f)

            // تبريد القدرة الخارقة
            if (stormCooldown > 0f) stormCooldown = (stormCooldown - 0.1f).coerceAtLeast(0f)

            // الذكاء الاصطناعي للعدو
            if (Random.Float() < 0.03f + (level * 0.005f)) {
                val enemyTypes = listOf("guardian", "archer", "mage", "ninja")
                val type = enemyTypes.random()
                val eHp = 200f + (level * 30f)
                units.add(
                    GameUnit(
                        id = type,
                        nameAr = "عدو",
                        nameEn = "Enemy",
                        x = 850f,
                        isPlayer = false,
                        hp = eHp,
                        maxHp = eHp,
                        damage = 25f + (level * 5f),
                        speed = 3f,
                        range = 40f,
                        cost = 0,
                        color = Color(0xFFEF4444)
                    )
                )
            }

            // حركة وتحكّم الوحدات والقتال
            val iterator = units.iterator()
            while (iterator.hasNext()) {
                val unit = iterator.next()
                if (unit.hp <= 0) {
                    iterator.remove()
                    continue
                }

                // البحث عن أقرب عدو في المدى
                val opponent = units.firstOrNull { it.isPlayer != unit.isPlayer && kotlin.math.abs(it.x - unit.x) <= unit.range }
                if (opponent != null) {
                    // اشتباك وقتال
                    opponent.hp -= unit.damage * 0.1f
                } else {
                    // تحرك نحو الهدف
                    if (unit.isPlayer) {
                        unit.x += unit.speed
                        if (unit.x >= 880f) {
                            enemyCrystalHp -= unit.damage * 0.1f
                            unit.hp = 0f // تضحية بالوحدة عند ضرب البلورة
                        }
                    } else {
                        unit.x -= unit.speed
                        if (unit.x <= 120f) {
                            playerCrystalHp -= unit.damage * 0.1f
                            unit.hp = 0f
                        }
                    }
                }
            }

            // فحص الفوز أو الخسارة
            if (playerCrystalHp <= 0) battleResult = false
            if (enemyCrystalHp <= 0) battleResult = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Mo
