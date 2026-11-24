package dev.digitalcloset.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ink = Color(0xFF17231F)
private val forest = Color(0xFF285E46)
private val canvas = Color(0xFFF5F7F6)
private val quiet = Color(0xFF43554C)

private enum class Screen(val label: String) {
    CLOSET("Closet"),
    ADD("Add garment"),
    OUTFIT("Outfit"),
}

@Composable
fun ClosetApp() {
    val garments = remember { mutableStateListOf<Garment>().apply { addAll(sampleGarments) } }
    var screen by remember { mutableStateOf(Screen.CLOSET) }

    MaterialTheme(
        colors = lightColors(
            primary = forest,
            onPrimary = Color.White,
            background = canvas,
            surface = Color.White,
            onSurface = ink,
        ),
    ) {
        Column(Modifier.fillMaxSize().background(canvas)) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
                Text("Digital Closet", color = quiet, fontSize = 15.sp)
                Text(
                    when (screen) {
                        Screen.CLOSET -> "Your wardrobe"
                        Screen.ADD -> "Add a garment"
                        Screen.OUTFIT -> "An outfit to try"
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                )
                Text(
                    "Prototype: sample data only",
                    color = quiet,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                when (screen) {
                    Screen.CLOSET -> ClosetScreen(garments)
                    Screen.ADD -> AddScreen { name, category ->
                        garments.add(Garment("local-${garments.size + 1}", name, category))
                        screen = Screen.CLOSET
                    }
                    Screen.OUTFIT -> OutfitScreen(garments)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Screen.entries.forEach { destination ->
                    if (screen == destination) {
                        Button(onClick = { screen = destination }) { Text(destination.label) }
                    } else {
                        OutlinedButton(onClick = { screen = destination }) {
                            Text(destination.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosetScreen(garments: List<Garment>) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "${garments.size} garments in this demo",
                color = quiet,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(garments, key = { it.id }) { garment ->
            Card(elevation = 0.dp, shape = RoundedCornerShape(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp)
                            .background(Color(0xFFE2EDE7), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(garment.name.take(1).uppercase(), color = forest)
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(garment.name, fontWeight = FontWeight.SemiBold, color = ink)
                        Text(garment.category.label, color = quiet, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddScreen(onAdd: (String, Category) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.TOP) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("Name your garment and choose a category.", color = quiet)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Garment name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text("Category", color = ink, fontWeight = FontWeight.SemiBold)
        Category.entries.forEach { option ->
            OutlinedButton(
                onClick = { category = option },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (category == option) Color(0xFFE2EDE7) else Color.White,
                ),
            ) {
                Text(option.label, color = ink)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onAdd(name.trim(), category); name = "" },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add to closet")
        }
    }
}

@Composable
private fun OutfitScreen(garments: List<Garment>) {
    val outfit = suggestOutfit(garments)
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        if (outfit == null) {
            Text("Add a top and bottom, or a one-piece, to see an outfit.", color = ink)
            return@Column
        }
        Text("A simple combination from your closet", color = quiet)
        Spacer(Modifier.height(16.dp))
        outfit.forEach { garment ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(garment.category.label, color = quiet, modifier = Modifier.weight(1f))
                Text(garment.name, color = ink, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "This example pairs a complete base outfit with available shoes.",
            color = forest,
        )
    }
}
