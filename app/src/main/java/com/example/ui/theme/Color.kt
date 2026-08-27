package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// SOFT LILAC THEME PALETTE (75% Opacity Background & High Contrast Solid UI)
// ==========================================

// Soft Lilac Main Canvas Background with 75% Opacity
val SoftLilacBase = Color(0xFFEDE7F6)                   // Soft Lilac Base
val SoftLilacBackground = Color(0xFFEDE7F6).copy(alpha = 0.75f) // 75% Opacity Soft Lilac
val LightGradientStart = Color(0xFFEDE7F6).copy(alpha = 0.75f)
val LightGradientEnd = Color(0xFFE8DEF8).copy(alpha = 0.75f)

// Primary & Deep Violet Palette (Solid & High Contrast)
val Primary = Color(0xFF6A4C93)                         // Deep Violet Primary
val OnPrimary = Color(0xFFFFFFFF)                       // Crisp White
val PrimaryContainer = Color(0xFFEDE4FF)                // Solid Soft Violet Tint
val OnPrimaryContainer = Color(0xFF3B2369)              // Deep Violet Text

val DeepVioletHeading = Color(0xFF3B2369)               // Solid Deep Violet (Heading / Title)
val SecondaryPurple = Color(0xFF7E57C2)                 // Vivid Secondary Purple
val MutedPurpleText = Color(0xFF554B6E)                 // Readable Medium Purple-Slate Label
val DarkContrastText = Color(0xFF1E1533)                // High Contrast for Numbers/Values

// Secondary: Slate / Muted Lavender
val Secondary = Color(0xFF7E57C2)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF3EEFA)
val OnSecondaryContainer = Color(0xFF3B2369)

// Tertiary: Emerald Green Accent (Solid)
val Tertiary = Color(0xFF166534)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFDCFCE7)
val OnTertiaryContainer = Color(0xFF166534)

// Base Background (75% Opacity Soft Lilac)
val Background = Color(0xFFEDE7F6).copy(alpha = 0.75f)
val OnBackground = Color(0xFF1E1533)

// Surface and Cards (Clean Solid White #FFFFFF with High Contrast)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1E1533)
val SurfaceVariant = Color(0xFFF5F0FB)
val OnSurfaceVariant = Color(0xFF554B6E)

// Borders (#E4DAF7 / #D8B4FE)
val Outline = Color(0xFFE4DAF7)
val OutlineVariant = Color(0xFFEDE4FF)

// Sisa Laba Card Theme
val SisaLabaBg = Color(0xFFF5F0FB)
val SisaLabaText = Color(0xFF3B2369)
val SisaLabaBorder = Color(0xFFE4DAF7)

// Pastel Theme Cards for Dashboard & Wallet Management (Solid Soft Tints)
val SkyBluePastel = Color(0xFFF5F0FB)
val SkyBlueBorder = Color(0xFFE4DAF7)

val LilacPastel = Color(0xFFF5F0FB)
val LilacBorder = Color(0xFFE4DAF7)

val MintPastel = Color(0xFFF5F0FB)
val MintBorder = Color(0xFFE4DAF7)

val LemonPastel = Color(0xFFF5F0FB)
val LemonBorder = Color(0xFFE4DAF7)

// Status Badges & Accents
val SeaBlue = Color(0xFF6A4C93)
val MintGreen = Color(0xFF6A4C93)
val MintAurora = Color(0xFFF3EEFA)
val HijauGelap = Color(0xFF3B2369)
val PinkAurora = Color(0xFFF3EEFA)
val MagentaLembut = Color(0xFF6A4C93)

// Primary Fintech Smooth Accent (Deep Violet)
val FintechActionGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF6A4C93),
        Color(0xFF6A4C93)
    )
)
