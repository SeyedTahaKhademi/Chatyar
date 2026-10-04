package ir.hooshamoozan.chatyar.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val t = LocalAppText.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("چ", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(t.appName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                t.tagline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(38.dp))
            Text(
                t.onboardingTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(t.onboardingBody, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
            Spacer(Modifier.height(22.dp))
            IntroCard(Icons.Outlined.Lock, t.onboardingPrivacyTitle, t.onboardingPrivacyBody)
            Spacer(Modifier.height(12.dp))
            IntroCard(Icons.Outlined.Dns, t.onboardingProviderTitle, t.onboardingProviderBody)
            Spacer(Modifier.height(12.dp))
            IntroCard(Icons.Outlined.Tune, t.onboardingLocalTitle, t.onboardingLocalBody)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(t.understood, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun IntroCard(icon: ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
