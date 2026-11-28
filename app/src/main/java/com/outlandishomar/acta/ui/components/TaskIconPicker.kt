package com.outlandishomar.acta.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.theme.*
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch

data class TaskIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val drawableRes: Int? = null
)

data class TaskIconCategory(
    val id: String,
    val title: String,
    val categoryIcon: ImageVector? = null,
    @DrawableRes val categoryDrawableRes: Int? = null,
    val icons: List<TaskIconOption>
)

object TaskIcons {
    val categories: List<TaskIconCategory> = listOf(
        // 1. Bathroom (13 user SVGs + 12 built-in icons)
        TaskIconCategory(
            id = "bathroom",
            title = "Bathroom",
            categoryIcon = Icons.Filled.Shower,
            icons = listOf(
                TaskIconOption("bath", "Bath", drawableRes = R.drawable.ic_task_bathroom_bath),
                TaskIconOption("tooth_brush", "Toothbrush", drawableRes = R.drawable.ic_task_bathroom_tooth_brush),
                TaskIconOption("soap_svg", "Soap Bar", drawableRes = R.drawable.ic_task_bathroom_soap),
                TaskIconOption("toilet_paper", "Toilet Paper", drawableRes = R.drawable.ic_task_bathroom_toilet_paper),
                TaskIconOption("cleanse", "Cleanse & Lotion", drawableRes = R.drawable.ic_task_bathroom_cleanse),
                TaskIconOption("hair_brush", "Hair Brush", drawableRes = R.drawable.ic_task_bathroom_hair_brush),
                TaskIconOption("hair_brush2", "Comb & Brush", drawableRes = R.drawable.ic_task_bathroom_hair_brush2),
                TaskIconOption("hair_brush3", "Round Brush", drawableRes = R.drawable.ic_task_bathroom_hair_brush3),
                TaskIconOption("razor", "Razor", drawableRes = R.drawable.ic_task_bathroom_razor),
                TaskIconOption("razor2", "Safety Razor", drawableRes = R.drawable.ic_task_bathroom_razor2),
                TaskIconOption("shave", "Electric Shaver", drawableRes = R.drawable.ic_task_bathroom_shave),
                TaskIconOption("scissors", "Scissors", drawableRes = R.drawable.ic_task_bathroom_scissors),
                TaskIconOption("lipstick", "Lipstick", drawableRes = R.drawable.ic_task_bathroom_lipstick),
                TaskIconOption("shower", "Shower", Icons.Filled.Shower),
                TaskIconOption("bathtub", "Bathtub", Icons.Filled.Bathtub),
                TaskIconOption("wash", "Wash", Icons.Filled.Wash),
                TaskIconOption("clean_hands", "Clean Hands", Icons.Filled.CleanHands),
                TaskIconOption("soap", "Soap", Icons.Filled.Soap),
                TaskIconOption("face", "Face Care", Icons.Filled.Face),
                TaskIconOption("spa", "Spa", Icons.Filled.Spa),
                TaskIconOption("haircut", "Haircut", Icons.Filled.ContentCut),
                TaskIconOption("sanitizer", "Sanitizer", Icons.Filled.Sanitizer),
                TaskIconOption("iron_trial", "Iron", Icons.Filled.Iron),
                TaskIconOption("dry_cleaning", "Dry Cleaning", Icons.Filled.DryCleaning),
                TaskIconOption("laundry_trial", "Laundry", Icons.Filled.LocalLaundryService)
            )
        ),
        // 2. Clothes (20 user SVGs + 12 built-in icons)
        TaskIconCategory(
            id = "clothes",
            title = "Clothes",
            categoryDrawableRes = R.drawable.ic_task_clothes_shirt,
            icons = listOf(
                TaskIconOption("shirt", "Shirt", drawableRes = R.drawable.ic_task_clothes_shirt),
                TaskIconOption("shorts", "Shorts", drawableRes = R.drawable.ic_task_clothes_shorts),
                TaskIconOption("dress", "Dress", drawableRes = R.drawable.ic_task_clothes_dress),
                TaskIconOption("jacket", "Jacket", drawableRes = R.drawable.ic_task_clothes_jacket),
                TaskIconOption("scarf", "Scarf", drawableRes = R.drawable.ic_task_clothes_scarf),
                TaskIconOption("socks", "Socks", drawableRes = R.drawable.ic_task_clothes_socks),
                TaskIconOption("glasses", "Glasses", drawableRes = R.drawable.ic_task_clothes_glasses),
                TaskIconOption("cap", "Cap", drawableRes = R.drawable.ic_task_clothes_cap),
                TaskIconOption("hat", "Hat", drawableRes = R.drawable.ic_task_clothes_hat),
                TaskIconOption("hat2", "Sun Hat", drawableRes = R.drawable.ic_task_clothes_hat2),
                TaskIconOption("shoe", "Shoe", drawableRes = R.drawable.ic_task_clothes_shoe),
                TaskIconOption("heels", "Heels", drawableRes = R.drawable.ic_task_clothes_heels),
                TaskIconOption("bag", "Bag", drawableRes = R.drawable.ic_task_clothes_bag),
                TaskIconOption("watch2", "Wristwatch", drawableRes = R.drawable.ic_task_clothes_watch2),
                TaskIconOption("necklace", "Necklace", drawableRes = R.drawable.ic_task_clothes_necklace),
                TaskIconOption("ring", "Ring", drawableRes = R.drawable.ic_task_clothes_ring),
                TaskIconOption("iron", "Iron", drawableRes = R.drawable.ic_task_clothes_iron),
                TaskIconOption("hair_dryer", "Hair Dryer", drawableRes = R.drawable.ic_task_clothes_hair_dryer),
                TaskIconOption("laundry", "Laundry Basket", drawableRes = R.drawable.ic_task_clothes_laundry),
                TaskIconOption("laundry_washing_machine", "Washing Machine", drawableRes = R.drawable.ic_task_clothes_laundry_washign_mashine),
                TaskIconOption("checkroom", "Clothes", Icons.Filled.Checkroom),
                TaskIconOption("shopping_bag", "Shopping Bag", Icons.Filled.ShoppingBag),
                TaskIconOption("shopping_cart", "Shopping Cart", Icons.Filled.ShoppingCart),
                TaskIconOption("briefcase", "Briefcase", Icons.Filled.Work),
                TaskIconOption("backpack", "Backpack", Icons.Filled.Backpack),
                TaskIconOption("luggage", "Luggage", Icons.Filled.Luggage),
                TaskIconOption("watch", "Watch", Icons.Filled.Watch),
                TaskIconOption("diamond", "Accessory", Icons.Filled.Diamond),
                TaskIconOption("umbrella", "Umbrella", Icons.Filled.Umbrella),
                TaskIconOption("mall", "Mall", Icons.Filled.LocalMall),
                TaskIconOption("store", "Store", Icons.Filled.Storefront),
                TaskIconOption("gift", "Gift", Icons.Filled.Redeem)
            )
        ),
        // 3. Home (6 user SVGs + 9 built-in icons)
        TaskIconCategory(
            id = "home",
            title = "Home",
            categoryIcon = Icons.Filled.Home,
            icons = listOf(
                TaskIconOption("clean_svg", "Broom & Dustpan", drawableRes = R.drawable.ic_task_home_clean),
                TaskIconOption("key", "Key", drawableRes = R.drawable.ic_task_home_key),
                TaskIconOption("alarm_svg", "Alarm Bell", drawableRes = R.drawable.ic_task_home_alarm),
                TaskIconOption("drill", "Drill", drawableRes = R.drawable.ic_task_home_drill),
                TaskIconOption("ladder", "Ladder", drawableRes = R.drawable.ic_task_home_ladder),
                TaskIconOption("pacifier", "Baby Pacifier", drawableRes = R.drawable.ic_task_home_pacifier),
                TaskIconOption("home", "Home", Icons.Filled.Home),
                TaskIconOption("bed", "Bed", Icons.Filled.Bed),
                TaskIconOption("couch", "Couch", Icons.Filled.Weekend),
                TaskIconOption("chair", "Chair", Icons.Filled.Chair),
                TaskIconOption("kitchen", "Kitchen", Icons.Filled.Kitchen),
                TaskIconOption("cleaning", "Cleaning", Icons.Filled.CleaningServices),
                TaskIconOption("lightbulb", "Lightbulb", Icons.Filled.Lightbulb),
                TaskIconOption("yard", "Garden", Icons.Filled.Yard),
                TaskIconOption("repair", "Repair", Icons.Filled.Build)
            )
        ),
        // 4. Food & Drinks (9 user SVGs + 7 built-in icons)
        TaskIconCategory(
            id = "food",
            title = "Food & Drinks",
            categoryDrawableRes = R.drawable.ic_task_food_burger,
            icons = listOf(
                TaskIconOption("cook", "Cook", drawableRes = R.drawable.ic_task_food_cook),
                TaskIconOption("bake", "Bake", drawableRes = R.drawable.ic_task_food_bake),
                TaskIconOption("blend", "Blender", drawableRes = R.drawable.ic_task_food_blend),
                TaskIconOption("microwave", "Microwave", drawableRes = R.drawable.ic_task_food_microwave),
                TaskIconOption("burger", "Burger", drawableRes = R.drawable.ic_task_food_burger),
                TaskIconOption("pizza", "Pizza", drawableRes = R.drawable.ic_task_food_pizza),
                TaskIconOption("drink", "Drink", drawableRes = R.drawable.ic_task_food_drink),
                TaskIconOption("bottle", "Bottle", drawableRes = R.drawable.ic_task_food_bottle),
                TaskIconOption("icecream", "Ice Cream", drawableRes = R.drawable.ic_task_food_icecream),
                TaskIconOption("restaurant", "Eat", Icons.Filled.Restaurant),
                TaskIconOption("coffee", "Coffee", Icons.Filled.Coffee),
                TaskIconOption("fastfood", "Fast Food", Icons.Filled.Fastfood),
                TaskIconOption("cake", "Birthday Cake", Icons.Filled.Cake),
                TaskIconOption("local_bar", "Cocktail", Icons.Filled.LocalBar),
                TaskIconOption("bakery_dining", "Bakery", Icons.Filled.BakeryDining),
                TaskIconOption("ramen_dining", "Noodles", Icons.Filled.RamenDining)
            )
        ),
        // 5. Study & Work (8 user SVGs + 12 built-in icons)
        TaskIconCategory(
            id = "study",
            title = "Study & Work",
            categoryIcon = Icons.Filled.School,
            icons = listOf(
                TaskIconOption("bookmark", "Bookmark", drawableRes = R.drawable.ic_task_study_bookmark),
                TaskIconOption("archive", "Archive Box", drawableRes = R.drawable.ic_task_study_archive),
                TaskIconOption("agreement_shake_hand", "Handshake / Deal", drawableRes = R.drawable.ic_task_study_agreement_shake_hand),
                TaskIconOption("money_saving", "Piggy Bank / Savings", drawableRes = R.drawable.ic_task_study_money_saving),
                TaskIconOption("messege", "Message", drawableRes = R.drawable.ic_task_study_messege),
                TaskIconOption("link_pin", "Link", drawableRes = R.drawable.ic_task_study_link_pin),
                TaskIconOption("search_magnifier", "Search", drawableRes = R.drawable.ic_task_study_search_magnifier),
                TaskIconOption("settings_svg", "Settings", drawableRes = R.drawable.ic_task_study_settings),
                TaskIconOption("school", "School", Icons.Filled.School),
                TaskIconOption("book", "Book", Icons.AutoMirrored.Filled.MenuBook),
                TaskIconOption("reading", "Reading", Icons.Filled.AutoStories),
                TaskIconOption("write", "Write", Icons.Filled.Edit),
                TaskIconOption("assignment", "Assignment", Icons.AutoMirrored.Filled.Assignment),
                TaskIconOption("calculate", "Math", Icons.Filled.Calculate),
                TaskIconOption("science", "Science", Icons.Filled.Science),
                TaskIconOption("language", "Language", Icons.Filled.Translate),
                TaskIconOption("essay", "Essay", Icons.Filled.HistoryEdu),
                TaskIconOption("draw", "Draw", Icons.Filled.Draw),
                TaskIconOption("money", "Finance", Icons.Filled.AttachMoney),
                TaskIconOption("bank", "Bank", Icons.Filled.AccountBalance)
            )
        ),
        // 6. Transport (3 user SVGs + 7 built-in icons)
        TaskIconCategory(
            id = "transport",
            title = "Transport",
            categoryIcon = Icons.Filled.DirectionsCar,
            icons = listOf(
                TaskIconOption("motorscycle", "Motorcycle", drawableRes = R.drawable.ic_task_transport_motorscycle),
                TaskIconOption("jerrycan_fuel", "Fuel / Gas", drawableRes = R.drawable.ic_task_transport_jerrycan_fuel),
                TaskIconOption("compass", "Compass", drawableRes = R.drawable.ic_task_transport_compass),
                TaskIconOption("car", "Car", Icons.Filled.DirectionsCar),
                TaskIconOption("flight", "Flight", Icons.Filled.Flight),
                TaskIconOption("train", "Train", Icons.Filled.Train),
                TaskIconOption("bike", "Bike", Icons.AutoMirrored.Filled.DirectionsBike),
                TaskIconOption("walk", "Walk", Icons.AutoMirrored.Filled.DirectionsWalk),
                TaskIconOption("explore", "Explore", Icons.Filled.Explore),
                TaskIconOption("directions_bus", "Bus", Icons.Filled.DirectionsBus)
            )
        ),
        // 7. Activities (5 user SVGs + 6 built-in icons)
        TaskIconCategory(
            id = "activities",
            title = "Activities",
            categoryDrawableRes = R.drawable.ic_task_activities_basketball,
            icons = listOf(
                TaskIconOption("football", "Football", drawableRes = R.drawable.ic_task_activities_football),
                TaskIconOption("basketball_svg", "Basketball", drawableRes = R.drawable.ic_task_activities_basketball),
                TaskIconOption("ping_pong", "Ping Pong", drawableRes = R.drawable.ic_task_activities_ping_pong),
                TaskIconOption("boxing_glove", "Boxing", drawableRes = R.drawable.ic_task_activities_boxing_glove),
                TaskIconOption("skate", "Roller Skate", drawableRes = R.drawable.ic_task_activities_skate),
                TaskIconOption("run", "Run", Icons.AutoMirrored.Filled.DirectionsRun),
                TaskIconOption("gym", "Gym", Icons.Filled.FitnessCenter),
                TaskIconOption("swim", "Swim", Icons.Filled.Pool),
                TaskIconOption("meditate", "Meditate", Icons.Filled.SelfImprovement),
                TaskIconOption("soccer", "Soccer", Icons.Filled.SportsSoccer),
                TaskIconOption("basketball", "Basketball Solid", Icons.Filled.SportsBasketball)
            )
        ),
        // 8. Health & Nature (23 built-in icons)
        TaskIconCategory(
            id = "health",
            title = "Health & Nature",
            categoryIcon = Icons.Filled.LocalFireDepartment,
            icons = listOf(
                TaskIconOption("morning", "Morning", Icons.Filled.WbSunny),
                TaskIconOption("night", "Night", Icons.Filled.Bedtime),
                TaskIconOption("alarm", "Wake Up", Icons.Filled.Alarm),
                TaskIconOption("fire", "Habit", Icons.Filled.LocalFireDepartment),
                TaskIconOption("prayer", "Prayer", Icons.Filled.Mosque),
                TaskIconOption("heart", "Health", Icons.Filled.Favorite),
                TaskIconOption("cardio", "Cardio", Icons.Filled.MonitorHeart),
                TaskIconOption("medication", "Medicine", Icons.Filled.Medication),
                TaskIconOption("doctor", "Doctor", Icons.Filled.LocalHospital),
                TaskIconOption("water", "Water", Icons.Filled.WaterDrop),
                TaskIconOption("mind", "Focus", Icons.Filled.Psychology),
                TaskIconOption("energy", "Energy", Icons.Filled.Bolt),
                TaskIconOption("pets", "Pets", Icons.Filled.Pets),
                TaskIconOption("park", "Park", Icons.Filled.Park),
                TaskIconOption("nature", "Nature", Icons.Filled.Eco),
                TaskIconOption("forest", "Forest", Icons.Filled.Forest),
                TaskIconOption("beach", "Beach", Icons.Filled.BeachAccess),
                TaskIconOption("mountain", "Mountain", Icons.Filled.Terrain),
                TaskIconOption("star", "Star", Icons.Filled.Star),
                TaskIconOption("celebration", "Party", Icons.Filled.Celebration),
                TaskIconOption("friends", "Friends", Icons.Filled.Groups),
                TaskIconOption("art", "Art", Icons.Filled.Palette),
                TaskIconOption("goal", "Goal", Icons.Filled.Flag)
            )
        ),
        // 9. Music & Leisure (16 user SVGs + 6 built-in icons)
        TaskIconCategory(
            id = "leisure",
            title = "Music & Leisure",
            categoryDrawableRes = R.drawable.ic_task_leisure_headphones,
            icons = listOf(
                TaskIconOption("music_note_svg", "Music Note", drawableRes = R.drawable.ic_task_leisure_music_note),
                TaskIconOption("headphones_svg", "Earphones", drawableRes = R.drawable.ic_task_leisure_headphones),
                TaskIconOption("headset", "Headset Outline", drawableRes = R.drawable.ic_task_leisure_headset),
                TaskIconOption("headset2", "Studio Headphones", drawableRes = R.drawable.ic_task_leisure_headset2),
                TaskIconOption("headset3", "Gaming Headset", drawableRes = R.drawable.ic_task_leisure_headset3),
                TaskIconOption("airpods", "AirPods", drawableRes = R.drawable.ic_task_leisure_airpods),
                TaskIconOption("speaker", "Speaker", drawableRes = R.drawable.ic_task_leisure_speaker),
                TaskIconOption("microphone", "Microphone", drawableRes = R.drawable.ic_task_leisure_microphone),
                TaskIconOption("guitar", "Guitar", drawableRes = R.drawable.ic_task_leisure_guitar),
                TaskIconOption("piano", "Piano", drawableRes = R.drawable.ic_task_leisure_piano),
                TaskIconOption("drums", "Drums", drawableRes = R.drawable.ic_task_leisure_drums),
                TaskIconOption("vinyl", "Vinyl Record", drawableRes = R.drawable.ic_task_leisure_vinyl),
                TaskIconOption("console", "Handheld Console", drawableRes = R.drawable.ic_task_leisure_console),
                TaskIconOption("console_controller", "Game Controller", drawableRes = R.drawable.ic_task_leisure_console_controller),
                TaskIconOption("camera_svg", "Camera", drawableRes = R.drawable.ic_task_leisure_camera),
                TaskIconOption("ticket", "Ticket", drawableRes = R.drawable.ic_task_leisure_ticket),
                TaskIconOption("music", "Music", Icons.Filled.MusicNote),
                TaskIconOption("headphones", "Headphones", Icons.Filled.Headphones),
                TaskIconOption("gaming", "Gaming", Icons.Filled.VideogameAsset),
                TaskIconOption("tv", "TV", Icons.Filled.Tv),
                TaskIconOption("movie", "Movie", Icons.Filled.Movie),
                TaskIconOption("camera", "Camera Solid", Icons.Filled.CameraAlt)
            )
        ),
        // 10. Tech & Apps (86 user SVGs + 6 built-in icons)
        TaskIconCategory(
            id = "tech",
            title = "Tech & Apps",
            categoryDrawableRes = R.drawable.ic_task_tech_laptop,
            icons = listOf(
                TaskIconOption("laptop_svg", "Laptop", drawableRes = R.drawable.ic_task_tech_laptop),
                TaskIconOption("adapt_cable", "Cable & Adapter", drawableRes = R.drawable.ic_task_tech_adapt_cable),
                TaskIconOption("password", "Password", drawableRes = R.drawable.ic_task_tech_password),
                TaskIconOption("logo_whatsapp", "WhatsApp", drawableRes = R.drawable.ic_task_tech_logo_whatsapp),
                TaskIconOption("logo_facebook", "Facebook", drawableRes = R.drawable.ic_task_tech_logo_facebook),
                TaskIconOption("facebookmessenger", "Messenger", drawableRes = R.drawable.ic_task_tech_facebookmessenger),
                TaskIconOption("logo_instagram", "Instagram", drawableRes = R.drawable.ic_task_tech_logo_instagram),
                TaskIconOption("logo_tiktok", "TikTok", drawableRes = R.drawable.ic_task_tech_logo_tiktok),
                TaskIconOption("logo_youtube", "YouTube", drawableRes = R.drawable.ic_task_tech_logo_youtube),
                TaskIconOption("youtubemusic", "YouTube Music", drawableRes = R.drawable.ic_task_tech_youtubemusic),
                TaskIconOption("snapchat", "Snapchat", drawableRes = R.drawable.ic_task_tech_snapchat),
                TaskIconOption("discord", "Discord", drawableRes = R.drawable.ic_task_tech_discord),
                TaskIconOption("signal", "Signal", drawableRes = R.drawable.ic_task_tech_signal),
                TaskIconOption("skype", "Skype", drawableRes = R.drawable.ic_task_tech_skype),
                TaskIconOption("logo_wechat", "WeChat", drawableRes = R.drawable.ic_task_tech_logo_wechat),
                TaskIconOption("vk", "VK", drawableRes = R.drawable.ic_task_tech_vk),
                TaskIconOption("reddit_logo_fill", "Reddit", drawableRes = R.drawable.ic_task_tech_reddit_logo_fill),
                TaskIconOption("logo_pinterest", "Pinterest", drawableRes = R.drawable.ic_task_tech_logo_pinterest),
                TaskIconOption("logo_linkedin", "LinkedIn", drawableRes = R.drawable.ic_task_tech_logo_linkedin),
                TaskIconOption("logo_twitch", "Twitch", drawableRes = R.drawable.ic_task_tech_logo_twitch),
                TaskIconOption("spotify", "Spotify", drawableRes = R.drawable.ic_task_tech_spotify),
                TaskIconOption("applemusic", "Apple Music", drawableRes = R.drawable.ic_task_tech_applemusic),
                TaskIconOption("applepodcasts", "Apple Podcasts", drawableRes = R.drawable.ic_task_tech_applepodcasts),
                TaskIconOption("shazam", "Shazam", drawableRes = R.drawable.ic_task_tech_shazam),
                TaskIconOption("netflix", "Netflix", drawableRes = R.drawable.ic_task_tech_netflix),
                TaskIconOption("primevideo", "Prime Video", drawableRes = R.drawable.ic_task_tech_primevideo),
                TaskIconOption("prime", "Amazon Prime", drawableRes = R.drawable.ic_task_tech_prime),
                TaskIconOption("appletv", "Apple TV", drawableRes = R.drawable.ic_task_tech_appletv),
                TaskIconOption("imdb_logo_cinema", "IMDb", drawableRes = R.drawable.ic_task_tech_imdb_logo_cinema),
                TaskIconOption("logo_google", "Google", drawableRes = R.drawable.ic_task_tech_logo_google),
                TaskIconOption("gmail", "Gmail", drawableRes = R.drawable.ic_task_tech_gmail),
                TaskIconOption("googledrive", "Google Drive", drawableRes = R.drawable.ic_task_tech_googledrive),
                TaskIconOption("googlemaps", "Google Maps", drawableRes = R.drawable.ic_task_tech_googlemaps),
                TaskIconOption("googlephotos", "Google Photos", drawableRes = R.drawable.ic_task_tech_googlephotos),
                TaskIconOption("googletranslate", "Google Translate", drawableRes = R.drawable.ic_task_tech_googletranslate),
                TaskIconOption("googlepay", "Google Pay", drawableRes = R.drawable.ic_task_tech_googlepay),
                TaskIconOption("logo_google_playstore", "Google Play", drawableRes = R.drawable.ic_task_tech_logo_google_playstore),
                TaskIconOption("logo_appstore", "App Store", drawableRes = R.drawable.ic_task_tech_logo_appstore),
                TaskIconOption("logo_chrome", "Chrome", drawableRes = R.drawable.ic_task_tech_logo_chrome),
                TaskIconOption("firefoxbrowser", "Firefox", drawableRes = R.drawable.ic_task_tech_firefoxbrowser),
                TaskIconOption("safari", "Safari", drawableRes = R.drawable.ic_task_tech_safari),
                TaskIconOption("opera", "Opera", drawableRes = R.drawable.ic_task_tech_opera),
                TaskIconOption("internetexplorer", "Internet Explorer", drawableRes = R.drawable.ic_task_tech_internetexplorer),
                TaskIconOption("microsoftword", "Microsoft Word", drawableRes = R.drawable.ic_task_tech_microsoftword),
                TaskIconOption("microsoftexcel", "Microsoft Excel", drawableRes = R.drawable.ic_task_tech_microsoftexcel),
                TaskIconOption("microsoftpowerpoint", "Microsoft PowerPoint", drawableRes = R.drawable.ic_task_tech_microsoftpowerpoint),
                TaskIconOption("microsoftonenote", "Microsoft OneNote", drawableRes = R.drawable.ic_task_tech_microsoftonenote),
                TaskIconOption("microsoftoutlook", "Microsoft Outlook", drawableRes = R.drawable.ic_task_tech_microsoftoutlook),
                TaskIconOption("microsoftteams", "Microsoft Teams", drawableRes = R.drawable.ic_task_tech_microsoftteams),
                TaskIconOption("logo_windows", "Windows", drawableRes = R.drawable.ic_task_tech_logo_windows),
                TaskIconOption("logo_android", "Android", drawableRes = R.drawable.ic_task_tech_logo_android),
                TaskIconOption("logo_linux", "Linux", drawableRes = R.drawable.ic_task_tech_logo_linux),
                TaskIconOption("redhat", "Red Hat", drawableRes = R.drawable.ic_task_tech_redhat),
                TaskIconOption("logo_github", "GitHub", drawableRes = R.drawable.ic_task_tech_logo_github),
                TaskIconOption("logo_visual_studio", "Visual Studio", drawableRes = R.drawable.ic_task_tech_logo_visual_studio),
                TaskIconOption("logo_python", "Python", drawableRes = R.drawable.ic_task_tech_logo_python),
                TaskIconOption("logo_html5", "HTML5", drawableRes = R.drawable.ic_task_tech_logo_html5),
                TaskIconOption("logo_docker", "Docker", drawableRes = R.drawable.ic_task_tech_logo_docker),
                TaskIconOption("cloudflare", "Cloudflare", drawableRes = R.drawable.ic_task_tech_cloudflare),
                TaskIconOption("logo_figma", "Figma", drawableRes = R.drawable.ic_task_tech_logo_figma),
                TaskIconOption("blender", "Blender 3D", drawableRes = R.drawable.ic_task_tech_blender),
                TaskIconOption("godotengine", "Godot Engine", drawableRes = R.drawable.ic_task_tech_godotengine),
                TaskIconOption("logo_audacity", "Audacity", drawableRes = R.drawable.ic_task_tech_logo_audacity),
                TaskIconOption("logo_fl", "FL Studio", drawableRes = R.drawable.ic_task_tech_logo_fl),
                TaskIconOption("logo_dropbox", "Dropbox", drawableRes = R.drawable.ic_task_tech_logo_dropbox),
                TaskIconOption("wikipedia", "Wikipedia", drawableRes = R.drawable.ic_task_tech_wikipedia),
                TaskIconOption("logo_amazon", "Amazon", drawableRes = R.drawable.ic_task_tech_logo_amazon),
                TaskIconOption("shopify", "Shopify", drawableRes = R.drawable.ic_task_tech_shopify),
                TaskIconOption("uber", "Uber", drawableRes = R.drawable.ic_task_tech_uber),
                TaskIconOption("ubereats", "Uber Eats", drawableRes = R.drawable.ic_task_tech_ubereats),
                TaskIconOption("starbucks", "Starbucks", drawableRes = R.drawable.ic_task_tech_starbucks),
                TaskIconOption("logo_paypal", "PayPal", drawableRes = R.drawable.ic_task_tech_logo_paypal),
                TaskIconOption("logo_venmo", "Venmo", drawableRes = R.drawable.ic_task_tech_logo_venmo),
                TaskIconOption("visa", "Visa", drawableRes = R.drawable.ic_task_tech_visa),
                TaskIconOption("mastercard", "Mastercard", drawableRes = R.drawable.ic_task_tech_mastercard),
                TaskIconOption("samsungpay", "Samsung Pay", drawableRes = R.drawable.ic_task_tech_samsungpay),
                TaskIconOption("steam", "Steam", drawableRes = R.drawable.ic_task_tech_steam),
                TaskIconOption("logo_steam", "Steam Alt", drawableRes = R.drawable.ic_task_tech_logo_steam),
                TaskIconOption("logo_epic_games", "Epic Games", drawableRes = R.drawable.ic_task_tech_logo_epic_games),
                TaskIconOption("logo_playstation", "PlayStation", drawableRes = R.drawable.ic_task_tech_logo_playstation),
                TaskIconOption("logo_xbox", "Xbox", drawableRes = R.drawable.ic_task_tech_logo_xbox),
                TaskIconOption("computer", "Computer", Icons.Filled.Computer),
                TaskIconOption("laptop", "Laptop Solid", Icons.Filled.Laptop),
                TaskIconOption("phone", "Phone", Icons.Filled.Smartphone),
                TaskIconOption("code", "Coding", Icons.Filled.Code),
                TaskIconOption("email", "Email", Icons.Filled.Email),
                TaskIconOption("call", "Call", Icons.Filled.Call)
            )
        )
    )

    private val optionsById: Map<String, TaskIconOption> = categories
        .flatMap { it.icons }
        .associateBy { it.id }

    fun getOptionByName(name: String?): TaskIconOption? {
        if (name.isNullOrBlank()) return null
        return optionsById[name]
    }

    fun hasIcon(name: String?): Boolean = getOptionByName(name) != null

    fun renderImageVectorToBitmap(
        imageVector: ImageVector,
        sizePx: Int,
        tintColor: Int
    ): android.graphics.Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(
            sizePx.coerceAtLeast(1),
            sizePx.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        val vw = imageVector.viewportWidth.takeIf { it > 0f } ?: 24f
        val vh = imageVector.viewportHeight.takeIf { it > 0f } ?: 24f
        canvas.scale(sizePx / vw, sizePx / vh)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            style = android.graphics.Paint.Style.FILL
        }

        fun drawGroup(group: VectorGroup) {
            canvas.save()
            if (group.translationX != 0f || group.translationY != 0f) {
                canvas.translate(group.translationX, group.translationY)
            }
            if (group.rotation != 0f) {
                canvas.rotate(group.rotation, group.pivotX, group.pivotY)
            }
            if (group.scaleX != 1f || group.scaleY != 1f) {
                canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
            }
            for (i in 0 until group.size) {
                when (val node = group[i]) {
                    is VectorPath -> {
                        val composePath = node.pathData.toPath()
                        val androidPath = composePath.asAndroidPath()
                        androidPath.fillType = if (node.pathFillType == androidx.compose.ui.graphics.PathFillType.EvenOdd) {
                            android.graphics.Path.FillType.EVEN_ODD
                        } else {
                            android.graphics.Path.FillType.WINDING
                        }
                        paint.alpha = (node.fillAlpha.coerceIn(0f, 1f) * 255f).toInt()
                        canvas.drawPath(androidPath, paint)
                    }
                    is VectorGroup -> drawGroup(node)
                }
            }
            canvas.restore()
        }

        drawGroup(imageVector.root)
        return bmp
    }
}

@Composable
fun TaskIcon(
    iconName: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val option = TaskIcons.getOptionByName(iconName) ?: return
    TaskOptionIcon(
        option = option,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun TaskOptionIcon(
    option: TaskIconOption,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when {
        option.drawableRes != null -> {
            Icon(
                painter = painterResource(id = option.drawableRes),
                contentDescription = option.label,
                tint = tint,
                modifier = modifier
            )
        }
        option.icon != null -> {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CategoryHeaderIcon(
    category: TaskIconCategory,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when {
        category.categoryDrawableRes != null -> {
            Icon(
                painter = painterResource(id = category.categoryDrawableRes),
                contentDescription = category.title,
                tint = tint,
                modifier = modifier
            )
        }
        category.categoryIcon != null -> {
            Icon(
                imageVector = category.categoryIcon,
                contentDescription = category.title,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

@Composable
fun TaskIconPickerDialog(
    selectedIconName: String?,
    onIconSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val dismissProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var containerWidthPx by remember { mutableFloatStateOf(1080f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dismissProgress.snapTo(1f)
        dismissProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    val animateClose: (afterClose: () -> Unit) -> Unit = { afterClose ->
        if (!isClosing) {
            isClosing = true
            scope.launch {
                val remaining = (1f - dismissProgress.value).coerceIn(0f, 1f)
                val duration = (remaining * 300f).toInt().coerceAtLeast(1)
                dismissProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                )
                afterClose()
            }
        }
    }

    Dialog(
        onDismissRequest = { animateClose(onDismiss) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val dialogWindow = (parent as? DialogWindowProvider)?.window
            dialogWindow?.let { w ->
                w.setBackgroundDrawableResource(android.R.color.transparent)
                w.setDimAmount(0f)
                WindowCompat.setDecorFitsSystemWindows(w, false)
            }
            onDispose {}
        }

        PredictiveBackHandler(enabled = !isClosing) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    dismissProgress.snapTo(backEvent.progress.coerceIn(0f, 1f))
                }
                animateClose(onDismiss)
            } catch (e: CancellationException) {
                scope.launch {
                    dismissProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
            }
        }

        val density = LocalDensity.current
        val subPageShadowPx = remember(density) { with(density) { 16.dp.toPx() } }
        val isDark = ThemeState.isThemeDark || TopBarBackground.calculateLuminance() <= 0.5f

        val tileBgColor = if (isDark) {
            lerp(TopBarBackground, Color.White, 0.08f)
        } else {
            lerp(TopBarBackground, Color.Black, 0.06f)
        }
        val searchBarBgColor = if (isDark) {
            lerp(TopBarBackground, Color.White, 0.09f)
        } else {
            lerp(TopBarBackground, Color.Black, 0.07f)
        }
        val bottomBarBgColor = if (isDark) {
            lerp(TopBarBackground, Color.White, 0.14f)
        } else {
            lerp(TopBarBackground, Color.Black, 0.10f)
        }

        var searchQuery by remember { mutableStateOf("") }
        val filteredCategories = remember(searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                TaskIcons.categories
            } else {
                TaskIcons.categories.mapNotNull { cat ->
                    val matching = cat.icons.filter {
                        it.label.lowercase().contains(q) ||
                                it.id.lowercase().contains(q) ||
                                cat.title.lowercase().contains(q)
                    }
                    if (matching.isNotEmpty()) cat.copy(icons = matching) else null
                }
            }
        }

        val listState = rememberLazyListState()
        val activeCategoryIndex by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex.coerceIn(0, (filteredCategories.size - 1).coerceAtLeast(0))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0) containerWidthPx = size.width.toFloat()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = dismissProgress.value.coerceIn(0f, 1f)
                        translationX = containerWidthPx * p
                        shadowElevation = if (p > 0f && p < 1f) subPageShadowPx else 0f
                    }
                    .background(TopBarBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Row: Back Arrow + Pill Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { animateClose(onDismiss) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(searchBarBgColor)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontFamily = InterText
                                ),
                                cursorBrush = SolidColor(Blue500),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search",
                                                color = TextSecondary,
                                                fontSize = 16.sp,
                                                fontFamily = InterText
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty() || selectedIconName != null) {
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            searchQuery = ""
                                        } else {
                                            animateClose {
                                                onIconSelected(null)
                                                onDismiss()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Categorized 6-column Icon Grid
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            top = 8.dp,
                            bottom = 110.dp
                        )
                    ) {
                        if (selectedIconName != null && searchQuery.isEmpty()) {
                            item(key = "remove_icon_row") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tileBgColor)
                                        .clickable {
                                            animateClose {
                                                onIconSelected(null)
                                                onDismiss()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove icon",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Remove selected icon",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterText
                                    )
                                }
                            }
                        }

                        filteredCategories.forEach { category ->
                            item(key = category.id) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 22.dp)
                                ) {
                                    // Category Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 2.dp)
                                    ) {
                                        CategoryHeaderIcon(
                                            category = category,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = category.title,
                                            color = TextSecondary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterText
                                        )
                                    }

                                    // 6-Column Rows
                                    val chunkedIcons = category.icons.chunked(6)
                                    chunkedIcons.forEachIndexed { rowIndex, rowIcons ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = if (rowIndex < chunkedIcons.lastIndex) 10.dp else 0.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            for (colIndex in 0 until 6) {
                                                val option = rowIcons.getOrNull(colIndex)
                                                if (option != null) {
                                                    val isSelected = option.id == selectedIconName
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isSelected) Blue500.copy(alpha = 0.22f) else tileBgColor
                                                            )
                                                            .then(
                                                                if (isSelected) {
                                                                    Modifier.border(
                                                                        1.5.dp,
                                                                        Blue500,
                                                                        RoundedCornerShape(12.dp)
                                                                    )
                                                                } else Modifier
                                                            )
                                                            .clickable {
                                                                animateClose {
                                                                    onIconSelected(option.id)
                                                                    onDismiss()
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        TaskOptionIcon(
                                                            option = option,
                                                            tint = if (isSelected) Blue500 else TextPrimary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                } else {
                                                    Spacer(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Bottom Category Bar (Structured style)
                if (searchQuery.isEmpty() && filteredCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bottomBarBgColor)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasHeaderItem = selectedIconName != null
                        filteredCategories.forEachIndexed { index, category ->
                            val targetListIndex = if (hasHeaderItem) index + 1 else index
                            val isCatSelected = activeCategoryIndex == targetListIndex ||
                                    (index == 0 && activeCategoryIndex == 0)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCatSelected) TopBarBackground.copy(alpha = 0.65f)
                                        else Color.Transparent
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch {
                                            listState.animateScrollToItem(targetListIndex)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryHeaderIcon(
                                    category = category,
                                    tint = if (isCatSelected) Blue500 else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
