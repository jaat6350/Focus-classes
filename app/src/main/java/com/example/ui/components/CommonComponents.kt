package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CategoryActivity
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.CategoryTest
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusNavy
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusRoyalBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTopAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    showBrandLogo: Boolean = false
) {
    TopAppBar(
        title = {
            if (showBrandLogo) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Focus Classes Logo",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "FOCUS ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "CLASSES",
                        color = FocusGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FocusNavy,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
fun FocusBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FocusPrimaryBlue,
                selectedTextColor = FocusPrimaryBlue,
                unselectedIconColor = FocusTextSecondary,
                unselectedTextColor = FocusTextSecondary,
                indicatorColor = FocusPrimaryBlue.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_home")
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.Group, contentDescription = "Batches") },
            label = { Text("Batches", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FocusPrimaryBlue,
                selectedTextColor = FocusPrimaryBlue,
                unselectedIconColor = FocusTextSecondary,
                unselectedTextColor = FocusTextSecondary,
                indicatorColor = FocusPrimaryBlue.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_batches")
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Students") },
            label = { Text("Students", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FocusPrimaryBlue,
                selectedTextColor = FocusPrimaryBlue,
                unselectedIconColor = FocusTextSecondary,
                unselectedTextColor = FocusTextSecondary,
                indicatorColor = FocusPrimaryBlue.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_students")
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard") },
            label = { Text("Leaderboard", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = FocusPrimaryBlue,
                selectedTextColor = FocusPrimaryBlue,
                unselectedIconColor = FocusTextSecondary,
                unselectedTextColor = FocusTextSecondary,
                indicatorColor = FocusPrimaryBlue.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_leaderboard")
        )
    }
}

@Composable
fun StudentAvatar(
    imagePath: String?,
    fullName: String,
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(FocusPrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "$fullName profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
            )
        } else {
            // Neutral initials or avatar icon
            val initials = fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")

            if (initials.isNotBlank() && size >= 40) {
                Text(
                    text = initials,
                    color = FocusPrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.38).sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "$fullName default avatar",
                    tint = FocusPrimaryBlue,
                    modifier = Modifier.size((size * 0.6).dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBadge(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = FocusTextDark
        )
    }
}

@Composable
fun NumberSelectPill(
    number: Int,
    isSelected: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) CategoryAttendance else Color.White
    val textColor = if (isSelected) Color.White else FocusTextDark
    val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(borderModifier)
            .clickable { onSelect(number) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun focusTextFieldColors(
    containerColor: Color = Color.White
): androidx.compose.material3.TextFieldColors {
    return androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedTextColor = FocusTextDark,
        unfocusedTextColor = FocusTextDark,
        disabledTextColor = FocusTextSecondary,
        errorTextColor = com.example.ui.theme.FocusErrorRed,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = FocusPrimaryBlue,
        errorCursorColor = com.example.ui.theme.FocusErrorRed,
        focusedBorderColor = FocusPrimaryBlue,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = FocusPrimaryBlue,
        unfocusedLabelColor = FocusTextSecondary,
        focusedPlaceholderColor = FocusTextSecondary,
        unfocusedPlaceholderColor = FocusTextSecondary,
        focusedLeadingIconColor = FocusPrimaryBlue,
        unfocusedLeadingIconColor = FocusTextSecondary,
        focusedTrailingIconColor = FocusPrimaryBlue,
        unfocusedTrailingIconColor = FocusTextSecondary
    )
}
