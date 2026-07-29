package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val urlRegex = "(?i)\\b(?:https?|ftp)://[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]".toRegex()
    
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in urlRegex.findAll(text)) {
            append(text.substring(lastIndex, match.range.first))
            
            val url = match.value
            pushLink(androidx.compose.ui.text.LinkAnnotation.Url(url = url))
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                append(url)
            }
            pop()
            
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color),
        maxLines = maxLines,
        overflow = overflow
    )
}
