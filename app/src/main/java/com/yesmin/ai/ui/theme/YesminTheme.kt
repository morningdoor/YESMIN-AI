package com.yesmin.ai.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val dark=darkColorScheme(primary=Color(0xFFBEA1FF),secondary=Color(0xFF63E5EF),background=Color(0xFF08090E),surface=Color(0xFF12131E),surfaceVariant=Color(0xFF242335),onPrimary=Color(0xFF231346))
private val light=lightColorScheme(primary=Color(0xFF6940BA),secondary=Color(0xFF006B73),background=Color(0xFFFAF7FF),surface=Color(0xFFFFFBFF))
@Composable fun YesminTheme(mode:String,content:@Composable ()->Unit) {
 MaterialTheme(colorScheme=if(mode=="dark" || (mode=="system" && isSystemInDarkTheme())) dark else light,content=content)
}
